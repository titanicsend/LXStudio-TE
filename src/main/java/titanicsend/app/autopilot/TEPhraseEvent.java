package titanicsend.app.autopilot;

public class TEPhraseEvent {
    private final long startedAtMs;
    private final TEPhrase phraseType;
    private final double bpm;

    public TEPhraseEvent(TEPhrase phraseType, double bpm) {
        this.startedAtMs = System.currentTimeMillis();
        this.phraseType = phraseType;
        this.bpm = bpm;
    }

    public TEPhraseEvent(long startedAt, TEPhrase phraseType, double bpm) {
        this.startedAtMs = startedAt;
        this.phraseType = phraseType;
        this.bpm = bpm;
    }

    public double getBpm() {
        return bpm;
    }

    public TEPhrase getPhraseType() {
        return phraseType;
    }

    public long getStartedAtMs() {
        return startedAtMs;
    }
}
