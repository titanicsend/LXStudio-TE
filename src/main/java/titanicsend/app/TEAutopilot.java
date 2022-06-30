package titanicsend.app;

import heronarts.lx.LX;
import heronarts.lx.LXLoopTask;
import heronarts.lx.clip.LXClip;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.osc.OscMessage;
import heronarts.lx.pattern.LXPattern;
import titanicsend.app.autopilot.*;
import titanicsend.util.CircularArray;
import titanicsend.util.TEMath;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TEAutopilot implements LXLoopTask {

    private static final int BEAT_MAX_WINDOW = 128; // max num beats to keep in history
    private static final int BEAT_START_ESTIMATION_AT = 16; // start narrowing in BPM after this many samples
    private static final int PHRASE_EVENT_MAX_WINDOW = 32; // max num phrase changes to keep in history
    private boolean enabled = true; // TODO(will) make false, users can enable from global UI toggle panel
    private boolean autoBpmSyncEnabled = true;  // should we try to sync BPM when ProDJlink seems off?
    private LX lx;

    private ConcurrentLinkedQueue<TEOscMessage> unprocessedOscMessages;
    private long lastOscMessageReceivedAt;

    private CircularArray<TEPhraseEvent> phraseEvents; // past log of phrase changes with timestamps
    private CircularArray<TEBeatEvent> beatEvents; // past log of beat timestamps

    private TEMath.EMA tempoEMA = null;
    private static double TEMPO_EMA_ALPHA = 0.2;
    private static double BPM_ERROR_ADJUST = 1.0; // if we detect BPM is off by more than this, adjust

    private long lastOscBeatAt;

    public TEAutopilot(LX lx) {
        this.lx = lx;

        // this queue needs to be accessible from the OSC listener, which is a different thread
        unprocessedOscMessages = new ConcurrentLinkedQueue<TEOscMessage>();

        // historical logs of events for calculations
        phraseEvents = new CircularArray<TEPhraseEvent>(TEPhraseEvent.class, PHRASE_EVENT_MAX_WINDOW);
        beatEvents = new CircularArray<TEBeatEvent>(TEBeatEvent.class, BEAT_MAX_WINDOW);

        // start any logic that begins with being enabled
        setEnabled(enabled);
    }

    private void initializeTempoEMA() {
        tempoEMA = new TEMath.EMA(this.lx.engine.tempo.bpm(), TEMPO_EMA_ALPHA);
    }

    protected void onOscMessage(OscMessage msg) {
        String address = msg.getAddressPattern().toString();
        TEOscMessage oscTE = new TEOscMessage(msg);

        //System.out.printf("HANDLER: OSC message received: %s\n", address);

        // First, let's check for global OSC messages that don't concern autopilot, or that
        // we should act on immediately
        // TODO(will) go back to using built-in OSC listener for setBPM messages once:
        // 1. Mark merges his commit for utilizing the main OSC listener
        // 2. Mark adds protection on input checking for setBPM = 0.0 messages (https://github.com/heronarts/LX/blob/e3d0d11a7d61c73cd8dde0c877f50ea4a58a14ff/src/main/java/heronarts/lx/Tempo.java#L201)
        if (TEOscPath.isTempoChange(address)) {
            double newTempo = msg.getDouble(0);
            if (TETimeUtils.isValidBPM(newTempo)) {  // lots of times the CDJ will send 0.0 for new tempo...
                System.out.printf("Changing LX tempo to %f\n", msg.getDouble(0));
                this.lx.engine.tempo.setBpm((float) newTempo);
                this.beatEvents.clear();
                initializeTempoEMA();
            }

        } else {
            //System.out.printf("Adding OSC message to queue: %s\n", address);
            unprocessedOscMessages.add(oscTE);
        }
    }

    public boolean shouldCheckForBpmDrift() {
        return autoBpmSyncEnabled && beatEvents.getSize() >= BEAT_START_ESTIMATION_AT;
    }

    public void handleBeatEvent(long beatAt) throws Exception {
        if (tempoEMA == null)
            initializeTempoEMA();

        // log beat event
        TEBeatEvent beat = new TEBeatEvent(beatAt);
        beatEvents.add(beat);

        // Why is this section here? -- CDJ/ShowKontrol/ProDJLink is NOT
        //      consistent with delivering correct tempo change messages when slider moves,
        //      but is with beat messages. If they differ, and our calculated value
        //      seems much closer than what LX has for tempo, we should adjust the tempo
        if (shouldCheckForBpmDrift()) {
            double estBPM = TETimeUtils.estBPMFromBeatEvents(beatEvents, beatAt);
            if (Double.isNaN(estBPM)) return;
            double estBPMAvg = tempoEMA.update(estBPM);
            if (Math.abs(lx.engine.tempo.bpm() - estBPMAvg) > BPM_ERROR_ADJUST) {
                System.out.printf("BPM est from beats: %f (avg=%f), LX BPM is: %f => overriding BPM! \n",
                        estBPM, estBPMAvg, lx.engine.tempo.bpm());
                lx.engine.tempo.setBpm(estBPMAvg);
            }
        }

        lastOscBeatAt = beatAt;
    }

    @Override
    public void loop(double deltaMs) {
        long now = System.currentTimeMillis();
        try {
            // if autopilot isn't enabled, just ignore for now
            if (!isEnabled()) return;

            // collect audio/FFT statistics
            // TODO(will) for when we want to act without OSC messages -- just based on pure audio

            // check for new OSC messages
            while (unprocessedOscMessages.size() > 0) {
                // grab a new message off the queue
                TEOscMessage oscTE = unprocessedOscMessages.poll();
                if (oscTE == null)
                    // this should never happen, since we test for size() of queue, but good to check
                    continue;

                // grab message & update with the most recent OscMessage received at
                String address = oscTE.message.getAddressPattern().toString();

                // handle OSC message based on type
                if (TEOscPath.isPhraseChange(address)) {
                    changePhrase(address, oscTE.timestamp, deltaMs);

                } else if (TEOscPath.isDownbeat(address)) {
                    // nothing to do yet

                } else if (TEOscPath.isBeat(address)) {
                    handleBeatEvent(oscTE.timestamp);

                } else {
                    // unrecognized OSC message!
                    System.out.printf("Don't recognize OSC message: %s\n", address);
                }
            }

        } catch (Exception e) {
            System.out.println("ERROR - unexpected exception in Autopilot.run()");
            e.printStackTrace(System.out);
        }
    }

    private void changePhrase(String oscAddress, long timestamp, double deltaMs) {
        // which phrase type is this?
        TEPhrase phrase = TEPhrase.resolvePhrase(oscAddress);

        // do some bookkeeping on if the phrase is different from last,
        // and add to historical log of events
        TEPhraseEvent phraseEvent = new TEPhraseEvent(timestamp, phrase, lx.engine.tempo.bpm.getValue());
        TEPhraseEvent prevPhraseEvent = phraseEvents.get(0);
        boolean phraseIsSame = (prevPhraseEvent != null && phrase == prevPhraseEvent.getPhraseType());
        phraseEvents.add(phraseEvent);
        System.out.printf("[PHRASE]: %s\n", phrase);

        // let's pick our channel to work with
        TEChannelName channelName = null;
        LXChannel channel = null;
        List<LXClip> clips = new ArrayList<LXClip>(); // clips to start

        if (phrase == TEPhrase.TRO) {
            channelName = TEChannelName.TRO;

        } else if (phrase == TEPhrase.UP) {
            channelName = TEChannelName.UP;

        } else if (phrase == TEPhrase.DOWN) {
            channelName = TEChannelName.DOWN;

        } else if (phrase == TEPhrase.CHORUS) {
            channelName = TEChannelName.CHORUS;

            // strobe
            LXClip strobeClip = TEMixerUtils.pickRandomClipFromChannel(lx, TEChannelName.STROBES);
            clips.add(strobeClip);

            // triggers
            LXClip triggerClip = TEMixerUtils.pickRandomClipFromChannel(lx, TEChannelName.TRIGGERS);
            clips.add(triggerClip);

        } else {
            // same as unknown
        }

        // activate chosen channel for this new phrase
        channel = (LXChannel) lx.engine.mixer.channels.get(channelName.getIndex());
        channel.triggerPatternCycle.setValue(true);
        LXPattern pattern = channel.getActivePattern();
        System.out.printf("[PATTERN]: id=%d, label=%s, idx=%d, path=%s\n",
                pattern.getId(), pattern.getCanonicalLabel(), pattern.getIndex(), pattern.getCanonicalPath());
        TEMixerUtils.setChannelExclusivelyVisible(lx, channelName);

        // run clips
        for (LXClip c : clips) {
            c.start();
            //System.out.printf("[CLIP]: %s ...\n", c.label);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (this.enabled) {
            System.out.println("VJ autoilot enabled!");
        } else {
            System.out.println("VJ autoilot disabled!");
        }
    }
}
