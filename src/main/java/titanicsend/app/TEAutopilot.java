package titanicsend.app;

import heronarts.lx.LX;
import heronarts.lx.LXLoopTask;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.osc.OscMessage;
import heronarts.lx.pattern.LXPattern;
import titanicsend.app.autopilot.*;
import titanicsend.app.autopilot.events.TEPhraseEvent;
import titanicsend.app.autopilot.utils.TEMixerUtils;
import titanicsend.util.TE;
import titanicsend.util.TEMath;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TEAutopilot implements LXLoopTask {
    private boolean enabled = false;

    // number of bars to fade out on various occasions
    private final int MISPREDICTED_FADE_OUT_BARS = 2;
    private final int PREV_FADE_OUT_BARS = 2;

    // number of bars after a chorus to continue leaving
    // FX channels visible
    private final double TRIGGERS_AT_CHORUS_LENGTH_BARS = 0.75;
    private final double STROBES_AT_CHORUS_LENGTH_BARS = 1.0;


    // various fader levels of importance
    private static double LEVEL_FULL = 1.0,
                           LEVEL_MISPREDICTED_FADE_OUT = 0.75, // fading out mistaken transition
                           LEVEL_PREV_FADE_OUT = 0.75, // fading out prev channel
                           LEVEL_BARELY_ON = 0.03,
                           LEVEL_HALF = 0.5,
                           LEVEL_OFF = 0.0,
                           LEVEL_FADE_IN = 0.4; // fading in next channel

    // Probability that we launch CHORUS clips upon a repeated CHORUS phrase
    // sometimes there are like 5 CHORUS's in a row, and want to keep some variety
    private static float PROB_CLIPS_ON_SAME_PHRASE = 1f;

    // if OSC messages are older than this, throw them out
    private static long OSC_MSG_MAX_AGE_MS = 5 * 1000;

    private LX lx;

    // OSC message related fields
    private ConcurrentLinkedQueue<TEOscMessage> unprocessedOscMessages;
    private long lastOscMessageReceivedAt;

    // if we detect BPM is off by more than this, adjust
    private static double BPM_ERROR_ADJUST = 1.0;

    // our historical tracking object, keeping state about events in past
    private TEHistorian history;

    // our pattern library, used to filter for new patterns
    private TEPatternLibrary library;

    // "oldNext" is essentially the echo channel -- we use it
    // to gradually fade out a pattern that may have been abruptly cut off
    // for example, we were transitioning from UP -> CHORUS, but all of a sudden we
    // see a DOWN, we fade out the "old next" (CHORUS) channel over 1-2 bars
    private LXChannel prevChannel, curChannel, nextChannel, oldNextChannel;

    // whether or not we should be fading out various channels
    private boolean oldNextFadeOutMode = false;
    private boolean prevFadeOutMode = false;

    private TEChannelName prevChannelName, curChannelName, nextChannelName, oldNextChannelName;
    private TEPhrase prevPhrase = null,
                     curPhrase = null,
                     nextPhrase = null,
                     oldNextPhrase = null;

    // use this to track the last set value of each of these faders
    private double nextChannelFaderVal = 0.0,
                   oldNextChannelFaderVal = 0.0,
                   fxChannelFaderVal = 0.0;

    // FX channels
    private LXChannel triggerChannel = null,
                      strobesChannel = null,
                      fxChannel = null;

    public TEAutopilot(LX lx, TEPatternLibrary l) {
        this.lx = lx;
        this.library = l;

        // this queue needs to be accessible from OSC listener in diff thread
        unprocessedOscMessages = new ConcurrentLinkedQueue<TEOscMessage>();

        // start any logic that begins with being enabled
        setEnabled(enabled);
    }

    public void resetHistory() {
        // historical logs of events for calculations
        history = new TEHistorian();

        // phrase state
        prevPhrase = null;
        curPhrase = TEPhrase.DOWN;
        nextPhrase = TEPhrase.UP;
        oldNextPhrase = null;

        prevChannelName = null;
        curChannelName = TEMixerUtils.getChannelNameFromPhraseType(curPhrase);
        nextChannelName = TEMixerUtils.getChannelNameFromPhraseType(nextPhrase);
        oldNextChannelName = null;

        // this call will also wait for the mixer to be initialized
        curChannel = TEMixerUtils.getChannelByName(lx, curChannelName);
        nextChannel = TEMixerUtils.getChannelByName(lx, nextChannelName);
        TEMixerUtils.setFaderTo(lx, curChannelName, LEVEL_FULL);
        triggerChannel = TEMixerUtils.getChannelByName(lx, TEChannelName.TRIGGERS);
        strobesChannel = TEMixerUtils.getChannelByName(lx, TEChannelName.STROBES);
        
        oldNextFadeOutMode = false;
        prevFadeOutMode = false;
    }

    protected void onOscMessage(OscMessage msg) {
        try {
            TEOscMessage oscTE = new TEOscMessage(msg);

            if (!isEnabled()) {
                // if autopilot isn't enabled, don't bother tracking these
                return;

            } else {
                //TE.log("Adding OSC message to queue: %s", address);
                unprocessedOscMessages.add(oscTE);
            }
        } catch (Exception e) {
            TE.log("Exception parsing OSC message (%s): %s", msg.toString(), e.toString());
        }
    }

    public void onBeatEvent(long beatAt) throws Exception {
        history.logBeat(beatAt);
    }

    @Override
    public void loop(double deltaMs) {
        long now = System.currentTimeMillis();
        try {
            // if autopilot isn't enabled, just ignore for now
            if (!isEnabled()) return;

            // check our patterns are indexed
            // this requires that LX's mixer / channels are setup, so
            // we do this here, as opposed to in the constructor, which is called
            // in TEApp before LX is really finished
            if (!this.library.isReady()) {
                this.library.indexPatterns();
            }

            // collect audio/FFT statistics
            // TODO(will) for when we want to act without OSC messages -- just based on pure audio

            // check for new OSC messages
            while (unprocessedOscMessages.size() > 0) {
                // grab a new message off the queue
                TEOscMessage oscTE = unprocessedOscMessages.poll();
                if (oscTE == null) {
                    // this should never happen, since we test for size() of queue, but good to check
                    TE.log("unprocessedOscMessages pulled off null value -- should never happen!");
                    continue;

                } else if (oscTE.timestamp <= now - OSC_MSG_MAX_AGE_MS) {
                    // if these messages are older than this, ignore
                    continue;
                }
                //TODO(will) test for & ignore for really old OSC messages. With multiple decks it's
                // possible to have a huge buildup of OSC messages from other deck that we don't
                // need/want to process

                // grab message & update with the most recent OscMessage received at
                String address = oscTE.message.getAddressPattern().toString();

                // handle OSC message based on type
                if (TEOscMessage.isPhraseChange(address)) {
                    onPhraseChange(address, oscTE.timestamp, deltaMs);
                } else if (TEOscMessage.isBeat(address)) {
                    onBeatEvent(oscTE.timestamp);

                } else {
                    // unrecognized OSC message!
                    TE.log("Don't recognize OSC message: %s", address);
                }
            }

        } catch (Exception e) {
            TE.err("ERROR - unexpected exception in Autopilot.run(): %s", e.toString());
            e.printStackTrace(System.out);
        }

        // get some useful stats about the current phrase
        int repeatedPhraseCount = history.getRepeatedPhraseCount();
        double repeatedPhraseLengthBars = history.getRepeatedPhraseBarProgress(lx.engine.tempo.bpm());
        double currentPhraseLengthBars = repeatedPhraseLengthBars - history.getRepeatedPhraseLengthBars();

        // update ongoing transitions!
        try {
            // update fader value for NEXT channel
            if (nextChannelName != null) {
                int estPhraseLengthBars = 16;
                double estFracCompleted = currentPhraseLengthBars / estPhraseLengthBars;

                // over consecutive phrases, we want to steadily approach full fader, but never get there
                double normalizedNumPhrases = repeatedPhraseLengthBars / estPhraseLengthBars + 1.;
                double nextChannelFaderFloorLevel = Math.min(
                        nextChannelFaderVal,
                        LEVEL_FADE_IN * (1.0 - Math.pow(0.5, normalizedNumPhrases - 1))); // 0 -> .5 - > .75  -> etc
                double nextChannelFaderCeilingLevel = LEVEL_FADE_IN * (1.0 - Math.pow(0.5, normalizedNumPhrases));  // .5 -> .75 -> .875 -> etc

                double range = nextChannelFaderCeilingLevel - nextChannelFaderFloorLevel;
                // can play around with the 1.5 exponent to make curve steeper!
                nextChannelFaderVal = range * Math.pow(estFracCompleted, 1.5) + nextChannelFaderFloorLevel;
                TEMixerUtils.setFaderTo(lx, nextChannelName, nextChannelFaderVal);
            }

            // update fader value for OLD NEXT channel
            if (oldNextFadeOutMode && currentPhraseLengthBars < MISPREDICTED_FADE_OUT_BARS) {
                //TE.log("FADE OLD NEXT: Fading out %s", oldNextChannelName);
                double newVal = TEMath.ease(
                        TEMath.EasingFunction.LINEAR_RAMP_DOWN,
                        currentPhraseLengthBars, 0.0, MISPREDICTED_FADE_OUT_BARS,
                        LEVEL_OFF, LEVEL_MISPREDICTED_FADE_OUT);
                TEMixerUtils.setFaderTo(lx, oldNextChannelName, newVal);
            }

            // update fader for prev channel
            if (prevFadeOutMode && currentPhraseLengthBars < PREV_FADE_OUT_BARS) {
                //TE.log("FADE PREV: Fading out %s", prevChannelName);
                double newVal = TEMath.ease(
                        TEMath.EasingFunction.LINEAR_RAMP_DOWN,
                        currentPhraseLengthBars, 0.0, PREV_FADE_OUT_BARS,
                        LEVEL_OFF, LEVEL_PREV_FADE_OUT);
                TEMixerUtils.setFaderTo(lx, prevChannelName, newVal);

            } else if (currentPhraseLengthBars >= PREV_FADE_OUT_BARS) {
                TEMixerUtils.setFaderTo(lx, prevChannelName, LEVEL_OFF);
            }

            // update FX channels
            if (strobesChannel.fader.getValue() > 0.0 && currentPhraseLengthBars < STROBES_AT_CHORUS_LENGTH_BARS) {
                double newVal = TEMath.ease(
                        TEMath.EasingFunction.LINEAR_RAMP_DOWN,
                        currentPhraseLengthBars, 0.0, STROBES_AT_CHORUS_LENGTH_BARS,
                        LEVEL_OFF, LEVEL_FULL);
                TEMixerUtils.setFaderTo(lx, TEChannelName.STROBES, newVal);

            } else if (currentPhraseLengthBars >= PREV_FADE_OUT_BARS) {
                TEMixerUtils.setFaderTo(lx, TEChannelName.STROBES, LEVEL_OFF);
            }

            if (triggerChannel.fader.getValue() > 0.0 && currentPhraseLengthBars < TRIGGERS_AT_CHORUS_LENGTH_BARS) {
                double newVal = TEMath.ease(
                        TEMath.EasingFunction.LINEAR_RAMP_DOWN,
                        currentPhraseLengthBars, 0.0, TRIGGERS_AT_CHORUS_LENGTH_BARS,
                        LEVEL_OFF, LEVEL_FULL);
                TEMixerUtils.setFaderTo(lx, TEChannelName.TRIGGERS, newVal);

            } else if (currentPhraseLengthBars >= TRIGGERS_AT_CHORUS_LENGTH_BARS) {
                TEMixerUtils.setFaderTo(lx, TEChannelName.TRIGGERS, LEVEL_OFF);
            }

        } catch (IndexOutOfBoundsException e) {
            // no phrase events detected yet
        }
    }

    private void updatePhraseState(TEPhrase newPhrase) {
        oldNextFadeOutMode = false;

        // phrase state
        oldNextPhrase = nextPhrase;
        prevPhrase = curPhrase;
        curPhrase = newPhrase;
        nextPhrase = guessNextPhrase(newPhrase);
    }

    private void startPattern(LXChannel channel, LXPattern pattern) {
        // disable the 100ms latency restriction LX has
        channel.transitionEnabled.setValue(false);

        // trigger the pattern
        channel.goPattern(pattern);
        channel.goPattern(pattern); // make doubly sure no transition of 100ms happens
    }

    private void onPhraseChange(String oscAddress, long timestamp, double deltaMs) throws Exception {
        // detect phrase type
        TEPhrase detectedPhrase = TEPhrase.resolvePhrase(oscAddress);
        if (detectedPhrase == TEPhrase.UNKNOWN)
            // skip if we don't understand the phrase
            return;

        // update state to reflect this
        this.updatePhraseState(detectedPhrase);
        boolean predictedCorrectly = (oldNextPhrase == curPhrase);
        boolean isSamePhrase = (prevPhrase == curPhrase);
        TE.log("HIT: %s: [%s -> %s -> %s (?)], (old next: %s)"
                , curPhrase, prevPhrase, curPhrase, nextPhrase, oldNextPhrase);

        // record history for pattern library
        // need to do this before we a) pick new patterns, and b) logPhrase() with historian
        double numBarsInLastPhraseRun = history.getRepeatedPhraseBarProgress(lx.engine.tempo.bpm());
        if (numBarsInLastPhraseRun > 0) {
            LXPattern curPattern = curChannel.getActivePattern();
            LXPattern curNextPattern = nextChannel.getActivePattern();
            library.logPhrase(curPattern, curNextPattern, numBarsInLastPhraseRun);
        }

        // clear mixer state
        TEMixerUtils.turnDownAllChannels(lx, true);

        if (isSamePhrase) {
            // our current channel should just keep running!
            // our next channel should be reset to 0.0
            // past channel == current channel, so no transition down needed
            TE.log("[AUTOVJ] Same phrase! no changes");

        } else {
            // update channel name & references based on phrase change
            prevChannelName = TEMixerUtils.getChannelNameFromPhraseType(prevPhrase);
            curChannelName = TEMixerUtils.getChannelNameFromPhraseType(curPhrase);
            nextChannelName = TEMixerUtils.getChannelNameFromPhraseType(nextPhrase);
            nextChannelFaderVal = 0.0;

            prevChannel = TEMixerUtils.getChannelByName(lx, prevChannelName);
            curChannel = TEMixerUtils.getChannelByName(lx, curChannelName);
            nextChannel = TEMixerUtils.getChannelByName(lx, nextChannelName);

            LXPattern newCurPattern = curChannel.getActivePattern();

            // set fader levels
            if (predictedCorrectly) {
                // if this was a transition away from a CHORUS or UP or DOWN into
                // diff phrase, we don't have strobes to cover, so let's echo
                prevFadeOutMode = (curPhrase == TEPhrase.DOWN || curPhrase == TEPhrase.UP);

                // we nailed it!
                TE.log("[AUTOVJ] We predicted correctly: prevFadeOutMode=%s", prevFadeOutMode);

            } else {
                // we didn't predict the phrase change correctly, turn off
                // the channel we were trying to transition into
                oldNextChannelName = TEMixerUtils.getChannelNameFromPhraseType(oldNextPhrase);
                oldNextChannel = TEMixerUtils.getChannelByName(lx, oldNextChannelName);
                oldNextFadeOutMode = (oldNextChannel != null);
                TE.log("[AUTOVJ] We didn't predict right, oldNextChannelName=%s, oldNextFadeOutMode=%s", oldNextChannelName, oldNextFadeOutMode);

                // pick a new pattern for our current channel, since we didn't see this coming
                // try to make it compatible with the one we were fading in, since we'll fade that mistaken one out
                if (prevChannel != null) {
                    newCurPattern = this.library.pickRandomCompatibleNextPattern(prevChannel.getActivePattern(), prevPhrase, curPhrase);
                } else {
                    newCurPattern = this.library.pickRandomPattern(curPhrase);
                }

                // print the current active pattern, along with what we're going to change to
                //TE.log("active pattern in current channel: %s, going to change to=%s", curChannel.getActivePattern(), newCurPattern);
                startPattern(curChannel, newCurPattern);
            }

            // pick a pattern we'll start fading into on "nextChannel" during the new few bars
            LXPattern newNextPattern = this.library.pickRandomCompatibleNextPattern(newCurPattern, curPhrase, nextPhrase);
            startPattern(nextChannel, newNextPattern);
            //TE.log("Selected new next pattern: %s, for channel %s", newNextPattern, nextChannelName);
        }

        TEMixerUtils.setFaderTo(lx, curChannelName, LEVEL_FULL);

        // trigger FX if needed
        this.enableFX(isSamePhrase);

        // add to historical log of events
        history.logPhrase(timestamp, curPhrase, lx.engine.tempo.bpm.getValue());
    }

    private void enableFX(boolean isSamePhrase) {
        if (curPhrase != TEPhrase.CHORUS)
            // only hit FX on chorus starts
            return;

        Random rand = new Random();
        if (isSamePhrase && rand.nextFloat() > PROB_CLIPS_ON_SAME_PHRASE) {
            // if it's the same phrase repeated, let's only trigger clips
            // certain fraction of the time
            return;
        }

        // make new active patterns
        if (prevPhrase != curPhrase) {
            // only play strobes if it's the first chorus phrase in a row
            LXPattern newStrobesPattern = TEMixerUtils.pickRandomPatternFromChannel(strobesChannel);
            startPattern(strobesChannel, newStrobesPattern);
            TEMixerUtils.setFaderTo(lx, TEChannelName.STROBES, LEVEL_FULL);
        }

        LXPattern newTriggersPattern = TEMixerUtils.pickRandomPatternFromChannel(triggerChannel);
        startPattern(triggerChannel, newTriggersPattern);

        // turn on strobes and triggers here, main loop will
        // turn them off after certain number of bars
        TEMixerUtils.setFaderTo(lx, TEChannelName.TRIGGERS, LEVEL_FULL);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (enabled == this.enabled)
            // only enact this logic if different from
            // current state!
            return;

        this.enabled = enabled;

        if (this.enabled) {
            TE.log("VJ autoilot enabled!");
            resetHistory();  // reset TEHistorian state
        } else {
            TE.log("VJ autoilot disabled!");
        }
    }

    public TEPhrase guessNextPhrase(TEPhrase newPhrase) {
        //TODO(will) make this smarter to count the number of beats
        // contiguously we've been on the same phrase type. Can also
        // use audio data -- this is just dumb first cut...
        boolean isSame = false;
        try {
            TEPhraseEvent prevPhraseEvt = history.phraseEvents.get(-1);
            isSame = (newPhrase == prevPhraseEvt.getPhraseType());
        } catch (IndexOutOfBoundsException e) {
            // there was no prev phrase event in history!
        }

        TEPhrase estimatedNextPhrase;

        // very dumb rule-based approach for now
        if (newPhrase == TEPhrase.TRO)
            estimatedNextPhrase = TEPhrase.UP;
        else if (newPhrase == TEPhrase.UP)
            estimatedNextPhrase = TEPhrase.CHORUS;
        else if (newPhrase == TEPhrase.DOWN)
            estimatedNextPhrase = TEPhrase.UP;
        else if (newPhrase == TEPhrase.CHORUS)
            estimatedNextPhrase = TEPhrase.DOWN;
        else
            estimatedNextPhrase = TEPhrase.DOWN;

        return estimatedNextPhrase;
    }
}
