package titanicsend.pattern.jeff;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEAudioPattern;


/**
 * One of our pattern standards is that the pattern should be audio-reactive,
 * even without accurate tempo data present from an external clock or
 * the internal `lx.engine.tempo` metronome.
 *
 * TEAudioPattern provides bassRatio and other fields and methods for processing
 * the live audio stream data. Example patterns are preloaded within the Audio
 * Examples workspace project (Select this at the top center of the UI).
 *
 * This pattern demonstrates how to use the audio input within pattern code to
 * activate pixels based on how much bass is currently in the audio, relative
 * to how much has been there recently.
 *
 * An alternate way to do this within the LX Studio UI would be to define a
 * CompoundParameter in this pattern that controls the bar's height, then add
 * a Beat Modulator that is linked to control that height parameter.
 *
 * Be sure to try this pattern with an audio input source selected. In the top
 * left of the UI, click "GLOBAL", enable the Audio with the green top-left toggle,
 * and select an input.
 *
 * If you find it too jumpy, try lengthening the release parameter on the audio
 * GraphicMeter.
 */

@LXCategory("Audio Examples")
public class BassReactive extends TEAudioPattern {
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


    @Override
    public void runTEAudioPattern(double deltaMs) {
        /* Scale the bassRatio by the Energy param and some selected constants
         * to give a good height in 0..1 controlled by the Energy param. See:
         * https://www.desmos.com/calculator/bee9cgf5mb to understand selected
         * constants below. In my experience, each pattern takes some
         * individual fiddling to get the range correct.
         */
        double bassHeightNormalized = (bassRatio - .5) / (1.01 - energy.getNormalized()) / 3 - .2;

        clearPixels();  // Sets all pixels to transparent for starters

        for (TEEdgeModel edge : model.edgesById.values()) {
            for (LXPoint point : edge.points) {
                // If the normalized Y height of this edge pixel is below the current
                // bass level, color it red. If not, it stays transparent.
                // Red is used for brevity. For real show patterns use LinkedColorParameters.
                if (point.yn < bassHeightNormalized) colors[point.index] = LXColor.RED;
            }
        }
    }
}
