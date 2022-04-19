package titanicsend.pattern;

import heronarts.lx.LX;
import heronarts.lx.audio.GraphicMeter;
import titanicsend.util.TEMath;

/**
 * Patterns should inherit from this if they wish to make use of live audio
 * data and several useful derived audio attributes, such as normalized
 * bass or treble levels.
 *
 * In the future we can implement gates, thresholds, beat detection, or
 * tempo inference.
 */
public abstract class TEAudioPattern extends TEPattern {
    // The GraphicMeter holds the analyzed frequency content for the audio input
    protected final GraphicMeter eq = lx.engine.audio.meter;

    // Fractions in 0..1 for the instantaneous frequency level this frame.
    // If we find this useful and track many more bands, a collection of ratio
    // tracker objects would make sense.
    protected double bassLevel;
    protected double trebleLevel;

    // One of the demo patterns allows the VJ to vary how many bass bands
    // are tracked.
    protected int bassBandCount;

    // Accumulate recent frequency band measurements into an exponentially
    // weighted moving average.
    protected TEMath.EMA avgBass = new TEMath.EMA(0.2, .01);
    protected TEMath.EMA avgTreble = new TEMath.EMA(0.2, .01);

    /* Ratios of the instantaneous frequency levels in bands to their recent
     * running average. Using a ratio like this helps auto-scale to various
     * input levels. It would be best combined with a gate, so that long periods
     * of silence are ignored and return 0 instead of establishing a low bar.
     *
     * For example, with bassRatio:
     * .01 = 1% of recent average bass
     * 1    = Exactly the recent average bass
     * 5    = 5 times higher than the recent average bass
     *
     * Values depend greatly on the audio content, but 0.2 to 3 are common.
     */
    protected double bassRatio = .2;
    protected double trebleRatio = .2;

    protected TEAudioPattern(LX lx) {
        super(lx);
        bassBandCount = 2;
    }

    /** Call computeAudio() in a TEAudioPattern's run() once per frame to
     * update values that analyze and process the audio stream.
     *
     * @param deltaMs elapsed time since last frame, as provided in run(deltaMs)
     */
    public void computeAudio(double deltaMs) {
        /* Average bass level of the bottom `bassBands` frequency bands.
         * The default lx.engine.audio.meter breaks up sound into 16 bands,
         * so a `bassBandCount` of 2 averages the bottom 12.5% of frequencies.
         */
        bassLevel = eq.getAverage(0, bassBandCount);

        // Instantaneous average level of the top half of the frequency bins
        trebleLevel = eq.getAverage(eq.numBands / 2, eq.numBands / 2);

        /* Compute the ratio of the current instantaneous frequency levels to
         * their new, updated moving averages.
         */
        bassRatio = bassLevel / avgBass.update(bassLevel, deltaMs);
        trebleRatio = trebleLevel / avgTreble.update(trebleLevel, deltaMs);
    }
}
