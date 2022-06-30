package titanicsend.app.autopilot;

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
