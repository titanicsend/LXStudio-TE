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

    // when we're in oscBeatOnlyModeOn=true, this is how long
    // we wait to change phrases
    private final int OSC_BEAT_ONLY_PHRASE_LEN_BARS = 32;

    // number of bars after a chorus to continue leaving
    // FX channels visible
    private final double TRIGGERS_AT_CHORUS_LENGTH_BARS = 1.0;
    private final double STROBES_AT_CHORUS_LENGTH_BARS = 1.25;


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
    private static long OSC_MSG_MAX_AGE_MS = 2 * 1000;

    // after a while (ie: 2 min) without receiving a phrase OSC msg,
    // on the next downbeat chose a phrase and enter it! this is how
    // non-rekordbox phrase mode is engaged
    private static long OSC_PHRASE_TIMEOUT_MS = 2 * 60 * 1000;
    private boolean oscBeatModeOnlyOn = false;

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

    /**
     * Instantiate the autopilot with a reference to both LX and the pattern library.
     *
     * @param lx
     * @param l
     */
    public TEAutopilot(LX lx, TEPatternLibrary l) {
        this.lx = lx;
        this.library = l;

        // this queue needs to be accessible from OSC listener in diff thread
        unprocessedOscMessages = new ConcurrentLinkedQueue<TEOscMessage>();

        // start any logic that begins with being enabled
        setEnabled(enabled);
    }

    /**
     * Reset history around autopilot, channel state, phrase state, etc.
     */
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

    /**
     * This is the entrypoint for OSC messages that come externally from ShowKontrol.
     * First they are processed by TEOscListener, but once they are deemed in scope for
     * Autopilot, they are dispatched here.
     *
     * @param msg
     */
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

    /**
     * Called when an OSC beat event comes through. Will only be triggered
     * if autoVJ is enabled.
     *
     * @param msg OscMessage from ShowKontrol
     * @throws Exception
     */
    public void onBeatEvent(TEOscMessage msg) throws Exception {
        long beatAt = msg.timestamp;
        history.logBeat(beatAt);

        // if this isn't a downbeat, ignore. want to wait for the start of a
        // bar to change modes or launch a new phrase if warranted
        if (msg.extractBeatCount() != 0)
            return;

        // if we're seeing phrase OSC messages regularly, no need to continue either
        if (beatAt - history.getLastOscPhraseAt() < OSC_PHRASE_TIMEOUT_MS)
            return;

        // are we currently in Osc phrase mode and need to transition?
        if (!oscBeatModeOnlyOn) {
            // it has been so long without a phrase that we need to enter
            // oscBeatModeOnlyOn=true and trigger phrases ourselves!
            this.resetHistory();
            oscBeatModeOnlyOn = true;

            // now trigger a phrase (DOWN is probably safest)
            String syntheticOscAddr = TEOscMessage.makeOscPhraseChangeAddress(TEPhrase.DOWN);
            onPhraseChange(syntheticOscAddr, beatAt);

        } else {
            // we've been in osc beat only mode for a while now, we just need to decide if it's
            // time to transition to a new phrase

            //TODO(will) based on audio signal, is this a likely CHORUS start?
            // if so, can change nextPhrase

            // get some useful stats about the current phrase
            double repeatedPhraseLengthBars = history.getRepeatedPhraseBarProgress(lx.engine.tempo.bpm());

            // otherwise: if it's been OSC_BEAT_ONLY_PHRASE_LEN_BARS bars, let's change
            if (OSC_BEAT_ONLY_PHRASE_LEN_BARS - repeatedPhraseLengthBars < 1) {
                // now trigger the next phrase
                String syntheticOscAddr = TEOscMessage.makeOscPhraseChangeAddress(nextPhrase);
                onPhraseChange(syntheticOscAddr, beatAt);
            }
        }
    }

    /**
     * Main event loop for autopilot. Mostly a no-op if autopilot is disabled.
     *
     * @param deltaMs ms since last loop ran
     */
    @Override
    public void loop(double deltaMs) {
        long now = System.currentTimeMillis();
        try {
            // if autopilot isn't enabled, just ignore for now
            if (!isEnabled()) return;

            // check our patterns are indexed
            // allows for switches in project files since we
            // need to do this more than once (so can't use TEApp)
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

                // grab message & update with the most recent OscMessage received at
                String address = oscTE.message.getAddressPattern().toString();

                // handle OSC message based on type
                if (TEOscMessage.isPhraseChange(address)) {
                    this.resetHistory();
                    history.setLastOscPhraseAt(oscTE.timestamp);
                    oscBeatModeOnlyOn = false;
                    onPhraseChange(address, oscTE.timestamp);

                } else if (TEOscMessage.isBeat(address)) {
                    onBeatEvent(oscTE);

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

            // fade out channels, if needed
            updateFadeOutChannels(currentPhraseLengthBars);

            // update FX channels, if needed
            updateFXChannels(currentPhraseLengthBars);

        } catch (IndexOutOfBoundsException e) {
            // no phrase events detected yet
        }
    }

    /**
     * Based on state set around phrase, update our faders with an eye towards the next expected phrase.
     * @param curPhraseLenBars: num contiguous bars in the current phrase type
     */
    private void updateFadeOutChannels(double curPhraseLenBars) {
        // update fader value for OLD NEXT channel
        if (oldNextFadeOutMode && curPhraseLenBars < MISPREDICTED_FADE_OUT_BARS) {
            //TE.log("FADE OLD NEXT: Fading out %s", oldNextChannelName);
            double newVal = TEMath.ease(
                    TEMath.EasingFunction.LINEAR_RAMP_DOWN,
                    curPhraseLenBars, 0.0, MISPREDICTED_FADE_OUT_BARS,
                    LEVEL_OFF, LEVEL_MISPREDICTED_FADE_OUT);
            TEMixerUtils.setFaderTo(lx, oldNextChannelName, newVal);
        }

        // update fader for prev channel
        if (prevFadeOutMode && curPhraseLenBars < PREV_FADE_OUT_BARS) {
            //TE.log("FADE PREV: Fading out %s", prevChannelName);
            double newVal = TEMath.ease(
                    TEMath.EasingFunction.LINEAR_RAMP_DOWN,
                    curPhraseLenBars, 0.0, PREV_FADE_OUT_BARS,
                    LEVEL_OFF, LEVEL_PREV_FADE_OUT);
            TEMixerUtils.setFaderTo(lx, prevChannelName, newVal);

        } else if (curPhraseLenBars >= PREV_FADE_OUT_BARS) {
            TEMixerUtils.setFaderTo(lx, prevChannelName, LEVEL_OFF);
        }
    }

    /**
     * Based on state set around phrase, update our FX faders with an eye towards the next expected phrase.
     * @param curPhraseLenBars
     */
    private void updateFXChannels(double curPhraseLenBars) {
        if (strobesChannel.fader.getValue() > 0.0 && curPhraseLenBars < STROBES_AT_CHORUS_LENGTH_BARS) {
            double newVal = TEMath.ease(
                    TEMath.EasingFunction.LINEAR_RAMP_DOWN,
                    curPhraseLenBars, 0.0, STROBES_AT_CHORUS_LENGTH_BARS,
                    LEVEL_OFF, LEVEL_FULL);
            TEMixerUtils.setFaderTo(lx, TEChannelName.STROBES, newVal);

        } else if (curPhraseLenBars >= PREV_FADE_OUT_BARS) {
            TEMixerUtils.setFaderTo(lx, TEChannelName.STROBES, LEVEL_OFF);
        }

        if (triggerChannel.fader.getValue() > 0.0 && curPhraseLenBars < TRIGGERS_AT_CHORUS_LENGTH_BARS) {
            double newVal = TEMath.ease(
                    TEMath.EasingFunction.LINEAR_RAMP_DOWN,
                    curPhraseLenBars, 0.0, TRIGGERS_AT_CHORUS_LENGTH_BARS,
                    LEVEL_OFF, LEVEL_FULL);
            TEMixerUtils.setFaderTo(lx, TEChannelName.TRIGGERS, newVal);

        } else if (curPhraseLenBars >= TRIGGERS_AT_CHORUS_LENGTH_BARS) {
            TEMixerUtils.setFaderTo(lx, TEChannelName.TRIGGERS, LEVEL_OFF);
        }
    }

    /**
     * Given a new phrase, update our phrase state around current,
     * previous, and next phrase. This is the foundation of how we fade in and out
     * of different phrase-based channels to make transitions happen.
     *
     * @param newPhrase
     */
    private void updatePhraseState(TEPhrase newPhrase) {
        oldNextFadeOutMode = false;

        // phrase state
        oldNextPhrase = nextPhrase;
        prevPhrase = curPhrase;
        curPhrase = newPhrase;
        nextPhrase = guessNextPhrase(newPhrase);
    }

    /**
     * Start an LXPattern. We want this to happen without delay.
     *
     * @param channel
     * @param pattern
     */
    private void startPattern(LXChannel channel, LXPattern pattern) {
        // disable the 100ms latency restriction LX has
        channel.transitionEnabled.setValue(false);

        // trigger the pattern
        channel.goPattern(pattern);
        channel.goPattern(pattern); // make doubly sure no transition of 100ms happens
    }

    /**
     * Callback that happens when a new phrase is triggered.
     *
     * @param oscAddress String address that denotes what kind of phrase
     * @param timestamp when this phrase was triggered
     * @throws Exception
     */
    private void onPhraseChange(String oscAddress, long timestamp) throws Exception {
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
                TE.log("active pattern in current channel: %s, going to change to=%s", curChannel.getActivePattern(), newCurPattern);
                startPattern(curChannel, newCurPattern);
            }

            // imagine: UP --> DOWN --> ? (but really any misprediction)
            // our next predicted would be UP, thus prevPhrase == nextPhrase, and so we'd be fading
            // out of UP while also trying to fade in AND switching the pattern, giving us the worst of all worlds:
            // we'd pick a new pattern (at full fader!) and not fade out of old one, and then likely
            // enter into a build where we flopped between at least two UPs. very bad look. this IF clause
            // prevents this!
            //
            // This could equivalently go in the:
            //
            //    if (predictedCorrectly) { ... }
            //
            // block, but I think it's clearer what's going on why here, and generalizes better if we decide to
            // add more phrase types later!
            if (prevPhrase != nextPhrase) {
                // pick a pattern we'll start fading into on "nextChannel" during the new few bars
                LXPattern newNextPattern = this.library.pickRandomCompatibleNextPattern(newCurPattern, curPhrase, nextPhrase);
                startPattern(nextChannel, newNextPattern);
                TE.log("Selected new next pattern: %s, for channel %s", newNextPattern, nextChannelName);
            }
        }

        TEMixerUtils.setFaderTo(lx, curChannelName, LEVEL_FULL);

        // trigger FX if needed
        this.enableFX(isSamePhrase);

        // add to historical log of events
        history.logPhrase(timestamp, curPhrase, lx.engine.tempo.bpm.getValue());
    }

    /**
     * Determines whether or not to trigger FX around important sonic events.
     *
     * @param isSamePhrase
     */
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

    /**
     * Is autopilot enabled? If not, ignore OSC messages and other input.
     *
     * @return boolean
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Toggle enable/disable autopilot. Clears history.
     *
     * @param enabled
     */
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

    /**
     * Guess the next phrase based on new current phrase and potentially
     * older ones. This is rule-based for now.
     *
     * @param newPhrase
     * @return
     */
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
