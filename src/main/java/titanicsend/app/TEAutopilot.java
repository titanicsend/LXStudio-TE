package titanicsend.app;

import heronarts.lx.LX;
import heronarts.lx.LXLoopTask;
import heronarts.lx.clip.LXClip;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.osc.OscMessage;
import heronarts.lx.pattern.LXPattern;
import titanicsend.app.autopilot.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TEAutopilot implements LXLoopTask {
    private boolean enabled = true; // TODO(will) make false, users can enable from global UI toggle panel
    private boolean autoBpmSyncEnabled = true;  // should we try to sync BPM when ProDJlink seems off?
    private LX lx;

    private ConcurrentLinkedQueue<TEOscMessage> unprocessedOscMessages;
    private long lastOscMessageReceivedAt;

    private static double BPM_ERROR_ADJUST = 1.0; // if we detect BPM is off by more than this, adjust

    private TEHistorian history;

    public TEAutopilot(LX lx) {
        this.lx = lx;

        // this queue needs to be accessible from the OSC listener, which is a different thread
        unprocessedOscMessages = new ConcurrentLinkedQueue<TEOscMessage>();

        // historical logs of events for calculations
        history = new TEHistorian(BPM_ERROR_ADJUST);

        // start any logic that begins with being enabled
        setEnabled(enabled);
    }

    private void initializeTempoEMA() {
        history.resetTempoTracking(this.lx.engine.tempo.bpm());
    }

    protected void onOscMessage(OscMessage msg) {
        String address = msg.getAddressPattern().toString();
        TEOscMessage oscTE = new TEOscMessage(msg);

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

                // clear and restart history for beats/tempo
                history.resetBeatTracking();
                history.resetTempoTracking(this.lx.engine.tempo.bpm());
            }

        } else {
            //System.out.printf("Adding OSC message to queue: %s\n", address);
            unprocessedOscMessages.add(oscTE);
        }
    }

    public void handleBeatEvent(long beatAt) throws Exception {
        if (!history.isTrackingTempo())
            history.resetTempoTracking(lx.engine.tempo.bpm.getValue());

        // log beat event
        history.logBeat(beatAt);

        // Why is this section here? -- CDJ/ShowKontrol/ProDJLink is NOT
        //      consistent with delivering correct tempo change messages when slider moves,
        //      but is with beat messages. If they differ, and our calculated value
        //      seems much closer than what LX has for tempo, we should adjust the tempo
        //
        //  If you want to ignore this, simply turn `autoBpmSyncEnabled`=false
        ///
        if (autoBpmSyncEnabled && history.readyForTempoEstimation()) {
            double estBPMAvg = history.estimateBPM();
            if (Math.abs(lx.engine.tempo.bpm() - estBPMAvg) > BPM_ERROR_ADJUST) {
                System.out.printf("BPM est from beats: %f, LX BPM is: %f => overriding BPM! \n", estBPMAvg, lx.engine.tempo.bpm());
                lx.engine.tempo.setBpm(estBPMAvg);
            }
        }
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
        TEPhrase phraseType = TEPhrase.resolvePhrase(oscAddress);

        // do some bookkeeping on if the phrase is different from last,
        // and add to historical log of events
        boolean phraseIsSame = history.logPhrase(timestamp, phraseType, lx.engine.tempo.bpm.getValue());
        System.out.printf("[PHRASE]: %s\n", phraseType);

        // let's pick our channel to work with
        TEChannelName channelName = null;
        LXChannel channel = null;
        List<LXClip> clips = new ArrayList<LXClip>(); // clips to start

        if (phraseType == TEPhrase.TRO) {
            channelName = TEChannelName.TRO;

        } else if (phraseType == TEPhrase.UP) {
            channelName = TEChannelName.UP;

        } else if (phraseType == TEPhrase.DOWN) {
            channelName = TEChannelName.DOWN;

        } else if (phraseType == TEPhrase.CHORUS) {
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
