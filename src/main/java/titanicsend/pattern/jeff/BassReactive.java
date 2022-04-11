package titanicsend.pattern.jeff;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEPattern;

import heronarts.lx.audio.GraphicMeter;

/**
 * One of our pattern standards is that the pattern should be audio-reactive,
 * even without accurate tempo data present from an external clock or
 * the internal `lx.engine.tempo` metronome.
 *
 * This pattern demonstrates how to use the audio input in code and
 * change pixels based on how much bass is currently in the audio, relative
 * to how much has been there recently.
 *
 * Note that the standard way to do this is probably to define a
 * CompoundParameter for the bar's height, then add a Beat modulator. In
 * the LX Studio UI you can link it's output to control the red height.
 *
 * This demonstrates a simple instant-to-running-average ratio approach, which
 * helps it auto-scale to various inputs. It would be best combined with a gate,
 * so that long periods of silence are ignored instead of establishing a low bar.
 *
 * If you find it too jumpy, try lengthening the release parameter on the audio
 * GraphicMeter (top left corner under Global)
 */

@LXCategory("Audio Examples")
public class BassReactive extends TEPattern {
    // The GraphicMeter holds the analyzed frequency content for the audio input
    protected final GraphicMeter eq = lx.engine.audio.meter;

    // Accumulate recent bass levels into a running average
    protected double runningAvgBass = 0;

    // Titanic's End wants all patterns to implement a parameter called "Energy",
    // which controls the amount of motion, light, movement, action, particles, etc.
    // We'll use it here to scale the bass meter
    public final CompoundParameter energy =
            new CompoundParameter("Energy", .1, 0, 1)
                    .setDescription("Amount of motion for various modes");


    public BassReactive(LX lx) {
        super(lx);
        addParameter("energy", energy);
    }

    public void run(double deltaMs) {
        // Average bass level of the bottom 12.5% of frequency bands.
        // The default lx.engine.audio.meter breaks up sound into 16 bands,
        // so this averages the bottom 2 bands.
        double bassLevel = eq.getAverage(0, Math.max(1, eq.numBands / 8));

        // https://en.wikipedia.org/wiki/Moving_average#Exponential_moving_average
        // where alpha = .01
        // The downside is that this approach can be frame rate dependent.
        runningAvgBass = .99 * runningAvgBass + .01 * bassLevel;

        // A bass ratio of instantaneous bass level to the recent running average.
        // .01 = 1% of recent average bass
        // 1    = Exactly the recent average bass
        // 5    = 5 times higher than the recent average bass
        double bassRatio = bassLevel / runningAvgBass;

        // Scale this ratio by the Energy param and some selected constants
        // to give a good height in 0..1 controlled by the Energy param
        double bassHeightNormalized = (bassRatio - .5) / (1.01 - energy.getNormalized()) / 3 - .2;

        clearPixels();  // Sets all pixels to transparent for starters

        for (TEEdgeModel edge : model.edgesById.values()) {
            for (LXPoint point : edge.points) {
                // If the normalized Y height of this edge pixel is below the current
                // bass level, color it red.
                if (point.yn < bassHeightNormalized)
                    colors[point.index] = LXColor.RED;
            }
        }
    }
}
