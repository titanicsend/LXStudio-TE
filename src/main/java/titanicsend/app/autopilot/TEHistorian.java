package titanicsend.app.autopilot;

import titanicsend.util.CircularArray;
import titanicsend.util.TEMath;

public class TEHistorian {
    // beat related constants
    public static final int BEAT_MAX_WINDOW = 128; // max num beats to keep in history
    private static double TEMPO_EMA_ALPHA = 0.2;

    public static final int BEAT_START_ESTIMATION_AT = 16; // start narrowing in BPM after this many samples

    // phrase related constants
    public static final int PHRASE_EVENT_MAX_WINDOW = 32; // max num phrase changes to keep in history

    // phrase history
    public CircularArray<TEPhraseEvent> phraseEvents; // past log of phrase changes with timestamps

    // beat history
    public CircularArray<TEBeatEvent> beatEvents; // past log of beat timestamps
    public TEMath.EMA tempoEMA;
    public long lastBeatAt;
    public double tempoErrorAdjustRange;

    public TEHistorian(double tempoErrorAdjustRange) {
        this.tempoErrorAdjustRange = tempoErrorAdjustRange;

        resetBeatTracking();
        resetPhraseTracking();
    }

    public void logBeat(long beatAt) {
        TEBeatEvent beat = new TEBeatEvent(beatAt);
        beatEvents.add(beat);
        lastBeatAt = beatAt;
    }

    public boolean logPhrase(long timestamp, TEPhrase phraseType, double currentBPM) {
        TEPhraseEvent phraseEvent = new TEPhraseEvent(timestamp, phraseType, currentBPM);
        boolean phraseIsSame = false;
        try {
            TEPhraseEvent prevPhraseEvent = phraseEvents.get(0);
            phraseIsSame = (phraseType == prevPhraseEvent.getPhraseType());
        } catch (IndexOutOfBoundsException e) {
            // this was the first phrase, phraseIsSame=false, no action needed
        }
        phraseEvents.add(phraseEvent);
        return phraseIsSame;
    }

    public double estimateBPM() {
        double estBPM = TETimeUtils.estBPMFromBeatEvents(beatEvents);
        double estBPMAvg = tempoEMA.update(estBPM);
        return estBPMAvg;
    }

    public void resetBeatTracking() {
        beatEvents = new CircularArray<TEBeatEvent>(TEBeatEvent.class, BEAT_MAX_WINDOW);
    }

    public void resetPhraseTracking() {
        phraseEvents = new CircularArray<TEPhraseEvent>(TEPhraseEvent.class, PHRASE_EVENT_MAX_WINDOW);
    }

    public void resetTempoTracking(double startingBpm) {
        tempoEMA = new TEMath.EMA(startingBpm, TEMPO_EMA_ALPHA);
    }

    public boolean isTrackingTempo() {
        return tempoEMA != null;
    }

    public boolean readyForTempoEstimation() {
        return beatEvents.getSize() >= BEAT_START_ESTIMATION_AT;
    }
}
