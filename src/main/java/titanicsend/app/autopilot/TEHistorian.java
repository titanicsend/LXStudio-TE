package titanicsend.app.autopilot;

import org.apache.commons.collections4.queue.CircularFifoQueue;
import titanicsend.app.autopilot.events.TEBeatEvent;
import titanicsend.app.autopilot.events.TEMasterChangeEvent;
import titanicsend.app.autopilot.events.TEPhraseEvent;
import titanicsend.app.autopilot.utils.TETimeUtils;
import titanicsend.util.TE;

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

    // start checking for tempo deviations after this many beat events
    public static final int BEAT_START_ESTIMATION_AT = 16;

    /*
        Phrase related constants
    */
    // max num phrase changes to keep in history
    public static final int PHRASE_EVENT_MAX_WINDOW = 32;

    public static final int DECK_CHANGE_WINDOW = 16;

    /*
        Phrase history
    */
    // past log of phrase changes with timestamps and tempo at the time
    public CircularFifoQueue<TEPhraseEvent> phraseEvents;
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
    public CircularFifoQueue<TEBeatEvent> beatEvents;
    // timestamp of when we last saw an OSC beat at
    private long lastBeatAt;
    private long lastDownbeatAt;

    // last time we saw any OSC message
    private long lastOscMsgAt;

    // timestamp of when we saw OSC phrase event last at
    private long lastOscPhraseAt;

    // timestamp of when we last triggered a synthetic phrase (no OSC)
    private long lastSynthethicPhraseAt;

    // timestamp of when we started the most recent palette
    private long paletteStartedAt;

    public CircularFifoQueue<TEMasterChangeEvent> deckChangeEvents;
    private long lastMasterChangeAt = System.currentTimeMillis();

    public TEHistorian() {
        resetBeatTracking();
        resetPhraseTracking();
        resetDeckChangeTracking();
    }

    /**
     * How long since the last change in master deck?
     * @return ms
     */
    public int calcMsSinceLastDeckChange() {
        return (int)(System.currentTimeMillis() - lastMasterChangeAt);
    }

    /**
     * How long since the last downbeat? (beat where count was 0)
     * @return ms
     */
    public int calcMsSinceLastDownbeat() {
        return (int)(System.currentTimeMillis() - lastDownbeatAt);
    }

    /**
     * How long since last OSC change phrase that we acted on?
     * @return ms
     */
    public int calcMsSinceLastOscPhraseChange() {
        return (int)(System.currentTimeMillis() - lastOscPhraseAt);
    }

    public void logMasterDeckChange(long timestamp, int deckNum, int faderVal) {
        TEMasterChangeEvent e = new TEMasterChangeEvent(timestamp, deckNum, faderVal);
        deckChangeEvents.add(e);
        //TE.log("Setting lastMasterChangeAt=%d, now=%d, old=%d, diffFromold=%d",
        //       timestamp, System.currentTimeMillis(), lastMasterChangeAt, this.calcMsSinceLastDeckChange());
        lastMasterChangeAt = timestamp;
    }

    public void logBeat(long beatAt, int beatCount) {
        TEBeatEvent beat = new TEBeatEvent(beatAt);
        beatEvents.add(beat);
        lastBeatAt = beatAt;

        // was this a downbeat?
        if (beatCount == 0)
            lastDownbeatAt = beatAt;

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
        this.lastOscPhraseAt = timestamp;

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

    public void resetBeatTracking() {
        beatEvents = new CircularFifoQueue<TEBeatEvent>(BEAT_MAX_WINDOW);
    }

    public void resetPhraseTracking() {
        phraseEvents = new CircularFifoQueue<TEPhraseEvent>(PHRASE_EVENT_MAX_WINDOW);
        curPhraseEvent = null;
    }

    public void resetDeckChangeTracking() {
        deckChangeEvents = new CircularFifoQueue<>(DECK_CHANGE_WINDOW);
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

    /**
     * This is the timestamp of the last time we received an OSC phrase message.
     *
     * @return timestamp long
     */
    public long getLastOscPhraseAt() {
        return lastOscPhraseAt;
    }
    public void setLastOscPhraseAt(long ts) {
        lastOscPhraseAt = ts;
    }

    public void startPaletteTimer() {
        paletteStartedAt = System.currentTimeMillis();
    }

    public long getPaletteStartedAt() {
        return paletteStartedAt;
    }

    public void setLastSynthethicPhraseAt(long timestamp) {
        lastSynthethicPhraseAt = timestamp;
    }
    public long getLastSynthethicPhraseAt() { return lastSynthethicPhraseAt; }

    public void setLastOscMsgAt(long timestamp) {
        lastOscMsgAt = timestamp;
    }
    public long getLastOscMsgAt() {
        return lastOscMsgAt;
    }
}
