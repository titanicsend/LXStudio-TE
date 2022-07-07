package titanicsend.app.autopilot;

import titanicsend.util.CircularArray;
import titanicsend.util.TEMath;

/**
 * This is a record keeper for all things VJ autopilot.
 *
 * Beat, downbeat, phrase OSC messages are all tracked here, along with
 * some releated logic for computing BPM or manipulating this historical
 * data.
 *
 * TEAutopilot is the orchestrator, but TEHistorian is where the data
 * is tracked and retrieved from.
 */
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
    private TEPhraseEvent curPhraseEvent;

    /*
        Beat history
    */
    // past log of beat timestamps
    public CircularArray<TEBeatEvent> beatEvents;
    // moving average object for tempo estimates
    private TEMath.EMA tempoEMA;
    // timestamp of when we last saw an OSC beat at
    private long lastBeatAt;

    public TEHistorian() {
        resetBeatTracking();
        resetPhraseTracking();
    }

    public void logBeat(long beatAt) {
        TEBeatEvent beat = new TEBeatEvent(beatAt);
        beatEvents.add(beat);
        lastBeatAt = beatAt;

        if (curPhraseEvent != null)
            //TODO(will) think about this race condition. We may see the OSC
            // beat event before we see the phrase event. This will cause our
            // counter for beats in a phrase to be slightly off. This might be
            // fine, but we should know that 15 beats should round up to 16 in
            // such cases
            curPhraseEvent.addBeat();
    }

    public void logPhrase(long timestamp, TEPhrase phraseType, double currentBPM) {
        TEPhraseEvent phraseEvent = new TEPhraseEvent(timestamp, phraseType, currentBPM);
        phraseEvents.add(phraseEvent);
        curPhraseEvent = phraseEvent;
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
        curPhraseEvent = null;
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
