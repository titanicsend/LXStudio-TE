package titanicsend.app;

import heronarts.lx.LX;
import heronarts.lx.osc.OscMessage;
import titanicsend.app.autopilot.TEDeckGroup;
import titanicsend.app.autopilot.TEOscMessage;
import titanicsend.app.autopilot.utils.TETimeUtils;
import titanicsend.util.TE;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This object will be the first line of response to ShowKontrol OSC messages.
 *
 * BPM and beats will be handled here, virtually identically to the way they are
 * in LX originally. Main reason for this is that ShowKontrol can only send to a single
 * OSC port, and this was simpler than taking a snapshot of the LX dev branch.
 *
 * Also makes it very clear in our codebase which messages go to LX, and which go to
 * TE. Mark also confirmed this route was likely best for us at this time.
 */
public class TEOscListener {
    private TEAutopilot autopilot;
    private LX lx;

    // a group of DJ decks, each of which has a fader value
    private TEDeckGroup deckGroup;

    // do not change LX tempo unless the newly received tempo
    // differs by more than this amount from current tempo
    private double TEMPO_DIFF_THRESHOLD = 0.05;

    public TEOscListener(LX lx, TEAutopilot ap) {
        this.autopilot = ap;
        this.lx = lx;
        this.deckGroup = new TEDeckGroup(TEDeckGroup.TE_DEFAULT_NUM_DECKS);
    }

    /**
     * This is the handler that will get OSC straight from ShowKontrol.
     * @param msg
     */
    public void onOscMessage(OscMessage msg) {
        // make a copy of the message, so anything we do doesn't
        // change it before passing along to autopilot. LX's OSC
        // message parsing mutates the state of the OscMessage
        // object, so I just wanted to avoid complexity there
        OscMessage copy = new OscMessage(msg.toString());
        String addr = msg.getAddressPattern().toString();
        //TE.log("Got OSC message in TEOscListener: %s", msg.toString());

        try {
            if (TEOscMessage.isTempoChange(addr)) {
                // logic from here:
                // https://github.com/heronarts/LX/blob/dev/src/main/java/heronarts/lx/Tempo.java#L217
                float newBpm = msg.getFloat();
                double bpmDiff = Math.abs(newBpm - lx.engine.tempo.bpm());
                if (TETimeUtils.isValidBPM(newBpm) && bpmDiff > TEMPO_DIFF_THRESHOLD) {
                    TE.log("Setting BPM=%f", newBpm);
                    lx.engine.tempo.setBpm(newBpm);
                }

            } else if (TEOscMessage.isStringTempoChange(addr)) {
                // logic from here:
                // https://github.com/heronarts/LX/blob/dev/src/main/java/heronarts/lx/Tempo.java#L217
                double newBpm = TEOscMessage.extractBpm(msg);
                double bpmDiff = Math.abs(newBpm - lx.engine.tempo.bpm());
                if (TETimeUtils.isValidBPM(newBpm) && bpmDiff > TEMPO_DIFF_THRESHOLD) {
                    TE.log("Setting BPM=%f (from beat)", newBpm);
                    lx.engine.tempo.setBpm(newBpm);
                }

            } else if (TEOscMessage.isFaderChange(addr)) {
                TEOscMessage teMsg = new TEOscMessage(msg);
                int deckNum = teMsg.extractDeck();
                int faderVal = teMsg.extractFaderValue();
                int newMasterDeckNum = this.deckGroup.updateFaderValue(deckNum, faderVal);
                //TODO(will) send this to ShowKontrol somehow to change master deck!
                TE.log("Master deck => deck=%d (deck%d changed fader to %d)", newMasterDeckNum, deckNum, faderVal);

            } else if (TEOscMessage.isBeat(addr)) {
                //TE.log("Got beat: %s", msg.toString());
                // logic from here
                // https://github.com/heronarts/LX/blob/dev/src/main/java/heronarts/lx/Tempo.java#L217
                lx.engine.tempo.trigger(msg.getInt() - 1);

                // then forward along to autopilot
                this.autopilot.onOscMessage(copy);

            } else if (TEOscMessage.isPhraseChange(addr)) {
                // just forward along to autopilot!
                this.autopilot.onOscMessage(copy);

            } else {
                // if we get here, this is an unsupported message!
                TE.err("Unsupported OSC message received by TE: %s", copy.toString());
            }

        } catch (Exception e) {
            TE.err("Exception in OSC message processing: %s: %s (msg=%s)"
                    , e.toString(), e.getMessage(), copy.toString());
        }
    }
}
