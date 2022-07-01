package titanicsend.app.autopilot;

import titanicsend.util.CircularArray;
import titanicsend.util.TEMath;

public class TEHistorian {
    /*
        Beat related constants
    */
    // max num of beat events to track in past
    public static final int BEAT_MAX_WINDOW = 128;
    // alpha for EMA on BPM estimates to smooth estimation
    private static double TEMPO_EMA_ALPHA = 0.2;

    // start checking for tempo deviations after this many beat events
    public static final int BEAT_START_ESTIMATION_AT = 16;

    /*
        Phrase related constants
    */
    // max num phrase changes to keep in history
    public static final int PHRASE_EVENT_MAX_WINDOW = 32;

    /*
        Phrase history
    */
    // past log of phrase changes with timestamps and tempo at the time
    public CircularArray<TEPhraseEvent> phraseEvents;

    /*
        Beat history
    */
    // past log of beat timestamps
    public CircularArray<TEBeatEvent> beatEvents;
    // moving average object for tempo estimates
    public TEMath.EMA tempoEMA;
    // timestamp of when we last saw an OSC beat at
    public long lastBeatAt;

    public TEHistorian() {
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
