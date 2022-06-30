package titanicsend.app.autopilot;

import heronarts.lx.osc.OscMessage;

public class TEOscMessage {
    public final OscMessage message;
    public final long timestamp;

    public TEOscMessage(OscMessage message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}
