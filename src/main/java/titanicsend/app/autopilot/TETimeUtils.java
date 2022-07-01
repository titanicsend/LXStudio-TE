package titanicsend.app.autopilot;

import titanicsend.util.CircularArray;
import titanicsend.util.TEMath;

import java.util.ArrayList;

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
        return (long) (System.currentTimeMillis() - samplingIntervalMs/2.0);
    }

    public static double msPerBeatToBpm(double msPerBeat) {
        return 1. / (msPerBeat * (1. / MS_PER_MIN));
    }

    public static double bpmToMsPerBeat(double bpm) {
        return 1. / (bpm / MS_PER_MIN);
    }

    public static double estBPMFromBeatEvents(CircularArray<TEBeatEvent> beatEvents) {
        TEBeatEvent[] beats = beatEvents.getAll();

        long curBeatStartAt = beats[0].getTimestamp(); // we'll change this as loop goes on
        int maxWindowSize = beatEvents.getSize();
        ArrayList<Long> diffs = new ArrayList<Long>();

        for (int i = 0; i < beats.length; i++) {
            TEBeatEvent be = beats[i];

            if (maxWindowSize == 0)
                break;

            // compute diffs and add up
            long diffMs = curBeatStartAt - be.getTimestamp();
            if (!isValidBeatPeriod(diffMs))
                // check for sane period length in ms, if not, forget this point
                continue;

            //System.out.printf("%d - %d = %d (BPM equiv=%f)\n", curBeatStartAt, be.getTimestamp(), diffMs, TETimeUtils.msPerBeatToBpm(diffMs));
            diffs.add(diffMs);

            // maintain loop invariants
            curBeatStartAt = be.getTimestamp();
            maxWindowSize--;
        }

        double avgMsPerDownbeat = TEMath.calcRecencyWeightedMean(diffs);
        double estBPM = TETimeUtils.msPerBeatToBpm(avgMsPerDownbeat);
        //System.out.printf("avgMsPerDownbeat=%f, estBPM=%f\n", avgMsPerDownbeat, estBPM);
        return estBPM;
    }
}
