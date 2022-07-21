package titanicsend.app;

import heronarts.lx.LX;
import heronarts.lx.LXLoopTask;
import heronarts.lx.clip.LXClip;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.osc.OscMessage;
import heronarts.lx.pattern.LXPattern;
import titanicsend.app.autopilot.*;
import titanicsend.app.autopilot.events.TEPhraseEvent;
import titanicsend.app.autopilot.utils.TEMixerUtils;
import titanicsend.app.autopilot.utils.TETimeUtils;
import titanicsend.util.TE;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TEAutopilot implements LXLoopTask {
    private boolean enabled = false;
    // should we try to sync BPM when ProDJlink seems off?
    private boolean autoBpmSyncEnabled = false;
    // do not change LX tempo unless the newly suggested tempo
    // differs by more than this amount
    private double TEMPO_DIFF_THRESHOLD = 0.05;

    // if we predicted the wrong next phrase, fade out the
    // phrase we were fading in over the next number of bars
    // sort of a "we totally meant to do that!" kind of thing
    private final int MISPREDICTED_FADE_OUT_BARS = 2;

    // various fader levels of importance
    private static double LEVEL_FULL = 1.0,
                           LEVEL_ECHO = 0.75,
                           LEVEL_BARELY_ON = 0.01,
                           LEVEL_HALF = 0.5,
                           LEVEL_OFF = 0.0;

    // Probability that we launch CHORUS clips upon a repeated CHORUS phrase
    // sometimes there are like 5 CHORUS's in a row, and want to keep some variety
    private static float PROB_CLIPS_ON_SAME_PHRASE = 0.5f;

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

    // transition state fields
    private LXChannel prevChannel, curChannel, nextChannel, oldNextChannel;
    private boolean echoMode = false;
    private TEChannelName prevChannelName, curChannelName, nextChannelName, oldNextChannelName;
    private TEPhrase prevPhrase = null,
                     curPhrase = null,
                     nextPhrase = null,
                     oldNextPhrase = null;

    // FX channels
    private LXChannel triggerChannel = null,
                      strobesChannel = null;

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
        prevChannelName = null;
        curChannelName = null;
        nextChannelName = null;
        oldNextChannelName = null;

        prevPhrase = null;
        curPhrase = null;
        nextPhrase = null;
        oldNextPhrase = null;

        echoMode = false;
    }

    private void initializeTempoEMA() {
        history.resetTempoTracking(this.lx.engine.tempo.bpm());
    }

    protected void onOscMessage(OscMessage msg) {
        try {
            String address = msg.getAddressPattern().toString();
            TEOscMessage oscTE = new TEOscMessage(msg);

            // First, let's check for global OSC messages that don't concern autopilot, or that
            // we should act on immediately
            //TODO(will) go back to using built-in OSC listener for setBPM messages once:
            // 1. Mark merges his commit for utilizing the main OSC listener
            // 2. Mark adds protection on input checking for setBPM = 0.0 messages (https://github.com/heronarts/LX/blob/e3d0d11a7d61c73cd8dde0c877f50ea4a58a14ff/src/main/java/heronarts/lx/Tempo.java#L201)
            if (TEOscMessage.isTempoChange(address)) {
                double newTempo = TEOscMessage.extractBpm(msg);
                double tempoDiff = Math.abs(newTempo - lx.engine.tempo.bpm());
                if (TETimeUtils.isValidBPM(newTempo) && tempoDiff > TEMPO_DIFF_THRESHOLD) {
                    TE.log("[OSC] Changing LX tempo to %f", (float) newTempo);
                    this.lx.engine.tempo.setBpm((float) newTempo);

                    // clear and restart history for beats/tempo
                    history.resetBeatTracking();
                    history.resetTempoTracking(this.lx.engine.tempo.bpm());
                }

            } else if (!isEnabled()) {
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
        if (!history.isTrackingTempo())
            history.resetTempoTracking(lx.engine.tempo.bpm.getValue());

        // log beat event
        history.logBeat(beatAt);

        // Tempo fine adjustment due to irregularities with OSC
        // If you want to ignore this, simply turn `autoBpmSyncEnabled`=false
        if (autoBpmSyncEnabled && history.readyForTempoEstimation()) {
            double estBPMAvg = history.estimateBPM();
            if (Math.abs(lx.engine.tempo.bpm() - estBPMAvg) > BPM_ERROR_ADJUST) {
                TE.log("BPM est from beats: %f, LX BPM is: %f, overriding BPM --> %f!"
                        , estBPMAvg, lx.engine.tempo.bpm(), estBPMAvg);
                lx.engine.tempo.setBpm(estBPMAvg);
            }
        }
    }

    @Override
    public void loop(double deltaMs) {
        long now = System.currentTimeMillis();

        // check our patterns are indexed
        // this requires that LX's mixer / channels are setup, so
        // we do this here, as opposed to in the constructor, which is called
        // in TEApp before LX is really finished
        if (!this.library.isReady())
            this.library.indexPatterns();

        try {
            // if autopilot isn't enabled, just ignore for now
            if (!isEnabled()) return;

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

                } else if (TEOscMessage.isDownbeat(address)) {
                    // nothing to do yet

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

        // update state of transition srcChannel
        try {
            // update fader value for NEXT channel
            if (nextChannelName != null) {
                int estPhraseLengthBars = 16;
                double estFracCompleted = currentPhraseLengthBars / estPhraseLengthBars;

                // over consecutive phrases, we want to steadily approach full fader, but never get there
                double nextChannelFaderFloorLevel = LEVEL_FULL * (1.0 - Math.pow(0.5, repeatedPhraseCount - 1)); // 0 -> .5 - > .75  -> etc
                double nextChannelFaderCeilingLevel = LEVEL_FULL * (1.0 - Math.pow(0.5, repeatedPhraseCount));  // .5 -> .75 -> .875 -> etc
                double range = nextChannelFaderCeilingLevel - nextChannelFaderFloorLevel;
                double faderVal = range * estFracCompleted + nextChannelFaderFloorLevel;
                TEMixerUtils.setFaderTo(lx, nextChannelName, faderVal);
            }

            // update fader value for OLD NEXT channel
            if (echoMode) {
                 // if we need to echo out the old channel (usually when we predicted wrong)
                 // echo it out here
                if (currentPhraseLengthBars < MISPREDICTED_FADE_OUT_BARS) {
                    double oldNextChannelFaderFloorLevel = 0.0;
                    double oldNextChannelFaderCeilingLevel = LEVEL_ECHO;
                    double range = oldNextChannelFaderCeilingLevel - oldNextChannelFaderFloorLevel;
                    double estFracRemaining = 1.0 - (currentPhraseLengthBars / MISPREDICTED_FADE_OUT_BARS);
                    double faderVal = range * estFracRemaining + oldNextChannelFaderFloorLevel;
                    TEMixerUtils.setFaderTo(lx, oldNextChannelName, faderVal);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            // no phrase events detected yet
        }
    }

    private void updatePhraseState(TEPhrase newPhrase) {
        echoMode = false;

        // phrase state
        oldNextPhrase = nextPhrase;
        prevPhrase = curPhrase;
        curPhrase = newPhrase;
        nextPhrase = guessNextPhrase(newPhrase);
    }

    private void onPhraseChange(String oscAddress, long timestamp, double deltaMs) throws Exception {
        // detect phrase type and update state to reflect this
        this.updatePhraseState(TEPhrase.resolvePhrase(oscAddress));
        boolean predictedCorrectly = (oldNextPhrase == curPhrase);
        boolean isSamePhrase = (prevPhrase == curPhrase);
        TE.log("Prev: %s, Cur: %s, Next (est): %s, Old next: %s", prevPhrase, curPhrase, nextPhrase, oldNextPhrase);

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

            prevChannel = TEMixerUtils.getChannelByName(lx, prevChannelName);
            curChannel = TEMixerUtils.getChannelByName(lx, curChannelName);
            nextChannel = TEMixerUtils.getChannelByName(lx, nextChannelName);

            LXPattern newCurPattern = curChannel.getActivePattern();

            // set fader levels
            if (predictedCorrectly) {
                // we nailed it!
                TE.log("[AUTOVJ] We predicted correctly!");

                // set these to null
                oldNextChannelName = null;
                oldNextChannel = null;
                echoMode = false; // TODO(will) maybe we want this true here? to echo out anyway

            } else {
                // we didn't predict the phrase change correctly, turn off
                // the channel we were trying to transition into
                oldNextChannelName = TEMixerUtils.getChannelNameFromPhraseType(oldNextPhrase);
                oldNextChannel = TEMixerUtils.getChannelByName(lx, oldNextChannelName);
                echoMode = (oldNextChannel != null);
                TE.log("[AUTOVJ] We didn't predict right, oldNextChannelName=%s, echoMode=%s", oldNextChannelName, echoMode);

                // pick a new pattern for our current channel, since we didn't see this coming
                // try to make it compatible with the one we were fading in, since we'll fade that mistaken one out
                if (prevChannel != null) {
                    newCurPattern = this.library.pickRandomCompatibleNextPattern(prevChannel.getActivePattern(), prevPhrase, curPhrase);
                } else {
                    newCurPattern = this.library.pickRandomPattern(curPhrase);
                }

                // make sure this newCurPattern is actually in our curChannel to activate!
                TE.log("curChannel=%s indexof pattern=%s is %d", curChannelName, newCurPattern, curChannel.patterns.indexOf(newCurPattern));

                // print the current active pattern, along with what we're going to change to
                TE.log("active pattern in current channel: %s, going to change to=%s", curChannel.getActivePattern(), newCurPattern);
                curChannel.goPattern(newCurPattern);

                // make sure we actually updated the curChannel's active pattern
                TE.log("Selected new current pattern: %s (active=%s), for channel %s", newCurPattern, newCurPattern, curChannelName);
            }

            // pick a pattern we'll start fading into on "nextChannel" during the new few bars
            LXPattern newNextPattern = this.library.pickRandomCompatibleNextPattern(newCurPattern, curPhrase, nextPhrase);
            nextChannel.goPattern(newNextPattern);
            TE.log("Selected new next pattern: %s, for channel %s", newNextPattern, nextChannelName);
        }

        TEMixerUtils.setFaderTo(lx, curChannelName, LEVEL_FULL);

        // trigger clips
        List<LXClip> clips = collectClipsToTrigger(curPhrase, isSamePhrase); // clips to start

        // run clips
        for (LXClip c : clips) {
            c.start();
        }

        // add to historical log of events
        history.logPhrase(timestamp, curPhrase, lx.engine.tempo.bpm.getValue());
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

    public void setAutoBpmSyncEnabled(boolean enableSync) {
        if (enableSync && !this.enabled) {
            TE.err("Cannot autosync BPM, requires: autopilot.setEnabled(true)!");
            return;
        }
        this.autoBpmSyncEnabled = enableSync;
    }

    public ArrayList<LXClip> collectClipsToTrigger(TEPhrase newPhrase, boolean isSamePhrase) {
        ArrayList<LXClip> clips = new ArrayList<LXClip>();
        Random rand = new Random();

        if (triggerChannel == null || strobesChannel == null) {
            // set up FX channels
            // do this here b/c doing it in constructor causes race condition with
            // initializing the LX mixer!
            triggerChannel = TEMixerUtils.getChannelByName(lx, TEChannelName.TRIGGERS);
            strobesChannel = TEMixerUtils.getChannelByName(lx, TEChannelName.STROBES);
        }

        if (isSamePhrase && rand.nextFloat() > PROB_CLIPS_ON_SAME_PHRASE) {
            // if it's the same phrase repeated, let's only trigger clips
            // certain fraction of the time
            return clips;
        }

        if (newPhrase == TEPhrase.CHORUS) {
            // pick 1 strobe clip
            LXClip strobeClip = TEMixerUtils.pickRandomClipFromChannel(lx, TEChannelName.STROBES);
            clips.add(strobeClip);
            //TODO(will) make strobe clips cycle rate same as BPM ! will look super cool.

            // pick 1 trigger clip
            LXClip triggerClip = TEMixerUtils.pickRandomClipFromChannel(lx, TEChannelName.TRIGGERS);
            clips.add(triggerClip);
            //TE.log("Chose strobe clip: %d, triggerClip: %d", strobeClip.getIndex(), triggerClip.getIndex());
        }

        // set strobes channels autocycle time fast
        //double msPerDivision = TETimeUtils.bpmToMsPerBeat(lx.engine.tempo.bpm()) / 16.; // sixteeth notes
        //strobesChannel.autoCycleTimeSecs.setValue(msPerDivision);

        return clips;
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
