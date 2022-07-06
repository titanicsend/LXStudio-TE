package titanicsend.app.autopilot;

/**
 * Struct for tracking OSC beat events. Useful for
 * computing BPM or performing analysis on previous
 * audio or other signal events to predict future ones.
 */
public class TEBeatEvent {
    private final long timestamp;

    public TEBeatEvent() {
        this.timestamp = System.currentTimeMillis();
    }

    public TEBeatEvent(long startedAt) {
        this.timestamp = startedAt;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
