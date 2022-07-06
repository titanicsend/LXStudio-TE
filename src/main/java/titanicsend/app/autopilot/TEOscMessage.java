package titanicsend.app.autopilot;

import heronarts.lx.osc.OscMessage;

/**
 * Struct wrapper around LX's OscMessage class.
 *
 * This is used to tag the message with the inbound time to
 * allow for better estimation of BPM and other time-oriented
 * calculations.
 */
public class TEOscMessage {
    public final OscMessage message;
    public final long timestamp;

    public TEOscMessage(OscMessage message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}
