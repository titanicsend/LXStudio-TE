package titanicsend.app.autopilot.utils;

/**
 * Useful utility functions for TE around timing and BPM.
 */
public class TETimeUtils {

    public static final int BEATS_PER_BAR = 4;

    public static final int NUM_BARS_SHORT_PHRASE = 8;
    public static final int NUM_BARS_MEDIUM_PHRASE = 16;
    public static final int NUM_BARS_LONG_PHRASE = 32;

    public static final int MS_PER_MIN = 60000;

    public static final double MIN_BPM = 50.0;
    public static final double MAX_BPM = 200.0;

    public static boolean isValidBPM(double bpm) {
        return bpm > MIN_BPM && bpm < MAX_BPM;
    }

    public static boolean isValidBeatPeriod(long periodMs) {
        double periodBPM = msPerBeatToBpm(periodMs);
        return periodBPM > MIN_BPM && periodBPM < MAX_BPM;
    }

    /*
       Given our BPM, how many milliseconds are there in a beat?
    */
    public static double calcMsPerBeat(double bpm) {
        double minPerSecond = 1. / 60.;
        double secPerMs = 1. / 1000.;
        double beatsPerMs = bpm * minPerSecond * secPerMs;
        return 1. / beatsPerMs;
    }

    /*
       Given a number of bars and a BPM, calculate how many milliseconds long this phrase will be.
    */
    public static double calcPhraseLengthMs(double bpm, int numBars) {
        return calcMsPerBeat(bpm) * BEATS_PER_BAR * numBars;
    }

    /*
       Given when a phrase started and the current time (now) in milliseconds, use the BPM to calculate the
       fraction of the way to the end of a number of bars we are.
    */
    public static float calcProgressToFutureBar(long phraseStartMs, long nowMs, double bpm, int phraseLengthInBars) {
        double phraseLengthEstimatedMs = calcPhraseLengthMs(bpm, phraseLengthInBars);
        float fractionCompleted = (float) ((nowMs - phraseStartMs) / phraseLengthEstimatedMs);
        return fractionCompleted;
    }

    /*
       This is dumb now, but in time if we want to manually tweak latency, we can do this in one place here.

       The reason for this is in the case we clear out OSC messages at
           t=100

        and the last time we sampled was
           t=90

        then we likely introduce the least error by guessing the event took place at
           t=95

        but this is the central place to adjust this if we can do better.
    */
    public static long calcEstimatedEventStartTimeMs(double samplingIntervalMs) {
        return (long) (System.currentTimeMillis() - samplingIntervalMs / 2.0);
    }

    public static double msPerBeatToBpm(double msPerBeat) {
        return 1. / (msPerBeat * (1. / MS_PER_MIN));
    }

    public static double bpmToMsPerBeat(double bpm) {
        return 1. / (bpm / MS_PER_MIN);
    }
}
