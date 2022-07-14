package titanicsend.app.autopilot;

import titanicsend.app.autopilot.events.TEBeatEvent;
import titanicsend.app.autopilot.events.TEPhraseEvent;
import titanicsend.app.autopilot.utils.TETimeUtils;
import titanicsend.util.CircularArray;
import titanicsend.util.TE;
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
    public TEPhraseEvent curPhraseEvent;
    private int repeatedPhraseCount = 1;
    private TEPhrase curPhraseType;
    private long repeatedPhraseStartAt;
    private long repeatedPhraseLengthMs = 0;
    private double repeatedPhraseLengthBars = 0.0;

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

        if (curPhraseEvent != null && phraseEvent.getPhraseType() == curPhraseEvent.getPhraseType()) {
            // keep track of how many consecutive times we've seen this phrase
            repeatedPhraseCount++;

            // add to counter for ms in length of phrase
            long subPhraseLength = (timestamp - curPhraseEvent.getStartedAtMs());
            repeatedPhraseLengthMs += subPhraseLength;

            // add to counter for num bars in phrase
            double msPerBeat =  TETimeUtils.calcMsPerBeat(phraseEvent.getBpm());
            double subPhraseLengthBars = 0.25 / msPerBeat * subPhraseLength;
            repeatedPhraseLengthBars += subPhraseLengthBars;

            //TE.log("After this phrase (%s), repeatedPhraseCount=%d, repeatedPhraseLengthMs=%d, repeatedPhraseLengthBars=%f",
            //        phraseEvent.getPhraseType(), repeatedPhraseCount, repeatedPhraseLengthMs, repeatedPhraseLengthBars);
        } else {
            // different phrase! reset state
            repeatedPhraseCount = 1;
            repeatedPhraseStartAt = timestamp;
            repeatedPhraseLengthMs = 0;
            repeatedPhraseLengthBars = 0.0;
            //TE.log("New phrase type (%s)! Reseting counters.", phraseEvent.getPhraseType());
        }

        // update current phrase event
        curPhraseEvent = phraseEvent;
        curPhraseType = phraseEvent.getPhraseType();
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

    /**
     * Including the current phrase, how many times in a row have
     * we hit this phrase?
     *
     * @return int
     */
    public int getRepeatedPhraseCount() {
        return repeatedPhraseCount;
    }

    /**
     * Over the repeated phrases we can see (starting with current phrase),
     * how many bars did this span? Returns a fractional number of bars.
     *
     * This INCLUDES current time into the phrase since it started, whereas
     * getRepeatedPhraseLengthBars() just includes completed phrases.
     *
     * @return double
     */
    public double getRepeatedPhraseBarProgress(double currentBpm) {
        if (curPhraseEvent == null)
            // we haven't encountered a phrase yet!
            return 0.0;

        long timeElapsedMs = (System.currentTimeMillis() - curPhraseEvent.getStartedAtMs());
        double msPerBeat =  TETimeUtils.calcMsPerBeat(curPhraseEvent.getBpm());
        double subPhraseLengthBars = 0.25 / msPerBeat * timeElapsedMs;
        return repeatedPhraseLengthBars + subPhraseLengthBars;
    }

    public double getRepeatedPhraseLengthBars() {
        return repeatedPhraseLengthBars;
    }
}
