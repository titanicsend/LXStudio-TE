package titanicsend.pattern.jeff;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.utils.LXUtils;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEAudioPattern;

import static titanicsend.util.TEMath.wave;

/**
 * This edge pattern aims to apply all our art standards as documented:
 *
 *  https://docs.google.com/document/d/16FGnQ8jopCGwQ0qZizqILt3KYiLo0LPYkDYtnYzY7gI/edit
 *
 *   - Tempo reactive (the colors flow out sync'd to every 4 beats)
 *   - Sound reactive, sparkling white when there's lots of treble
 *   - Uses the global palette, a simple gradient from the edge
 *     foreground to the secondary color
 *   - Uses the energy parameter we want all TEPatterns to consume
 *   - Respects the geometric form - it's volumetric based on the
 *     distance from the center of the car; not a screen projection
 *
 *   It is preloaded and intended to be used from within the Audio Examples
 *   workspace project (Select this at the top center of the UI).
 */

@LXCategory("TE Examples")
public class ArtStandards extends TEAudioPattern {
    /* Titanic's End wants all patterns to implement a parameter called
     * "Energy", which controls the amount of motion (or light, movement,
     * action, particles,) etc. In this pattern, it controls the probability
     * of sparkles.
     */
    public final CompoundParameter energy =
            new CompoundParameter("Energy", .1, 0, 1)
                    .setDescription("Amount of motion - Sparkles");

    public ArtStandards(LX lx) {
        super(lx);
        addParameter("energy", energy);
    }

    public void runTEAudioPattern(double deltaMs) {
        /* Inside run(), and before iterating over the LED points, we want to do
         * anything that should be done only once per frame. We're targeting about
         * 70 frames per second, so this is a good point to check in on things
         * like UI controls that have changed (if not using callbacks), palette
         * colors changing, or processing audio values. These things would all be
         * inefficient if we computed them in the once-per-pixel loops below.
         *
         * It's also a good place to do anything that depends on the current time
         * or tempo, but doesn't vary per-pixel.
         */

        // Art standard: Respect the palette
        // We'll use the `primaryGradient` that TEPattern provides. In case the
        // palette has changed or is transitioning, this gets the new values.
        updateGradients();



        /* Art standard: Use the tempo
         * `measure` is a 0..1 normalized ramp (fraction) into the current
         * measure. Sometimes a sync'd modulator is all you need, but in this
         * pattern we want to determine hues and brightness based on both the
         * tempo progress and the position of the pixel, so we use the ramp.
         *
         * The default setting is that a measure is 4 beats.
         */
        float measure = (float) measure();

        // Art standards: Use the sound data, and incorporate the Energy parameter.

        /* We'll sparkle when treble is high compared to recent treble levels.
         * Scale the bassRatio by the Energy param and some selected constants
         * to give a good height in 0..1 controlled by the Energy param. See:
         * https://www.desmos.com/calculator/bee9cgf5mb to understand the
         * constants selected here. In my experience, each pattern takes some
         * individual fiddling to get the range correct.
         */
        double scaledTrebleRatio = LXUtils.clamp(
                (trebleRatio - .5) / (1.01 - energy.getNormalized()) / 6 -
                                    .2 + energy.getNormalized() / 2,
                0, 1);

        // Aesthetic choice to make lots of sparkles rare; by squaring this
        // normalized value which is in 0..1, higher values become less frequent.
        scaledTrebleRatio *= scaledTrebleRatio;

        for (TEEdgeModel edge : modelTE.edgesById.values()) {
            for (LXPoint point : edge.points) {
                /* This is the point to be careful about computation resources.
                 * Only compute things here that need to vary per-pixel. For this
                 * pattern that's the colors (they vary based on the pixel's
                 * position in space), and whether this pixel is sparkling white
                 * (which is probabilistic per-pixel).
                 */

                /* The color for this point is a function of both time (where we
                 * are in the current tempo's measure), and it's distance from the
                 * center of the floor of the car.
                 *
                 * `point.rn` stands for radius (from 0,0,0), normalized to 0..1.
                 * In our model, (0,0,0) is on the ground in the center of the car.
                 * Multiplying by 2 lets getEdgeGradientColor oscillate back to the
                 * start of the gradient.
                 *
                 * When lerp=0: C1 -> lerp=1: C2 -> lerp=2: C1
                 *
                 * point.rn range is 0..1, measure's range is 0..1
                 */
                int baseColor = getPrimaryGradientColor(2 * (point.rn - measure));

                // We're going to add a sparkle based on the treble in the music,
                // so we'll break this color apart into it's hsb components.
                float hue = LXColor.h(baseColor);
                float saturation = LXColor.s(baseColor);
                float brightness = LXColor.b(baseColor);

                /* If our `trebleRatio` is really high, we want to sparkle more.
                 * Pick a random number in 0..1, and if `trebleRatio` is above it,
                 * set the saturation to zero, which makes the color white as
                 * long as brightness is also nonzero.
                 */
                saturation = Math.random() < scaledTrebleRatio ? 0 : saturation;

                /* The coefficient of 2 in front of `point.rn` makes two waves
                 * visible at any time, emanating from the ground at the center of
                 * the car, and have them transit the car once every measure.
                 */
                double alphaWave = wave(2 * point.rn - measure);

                /* Alpha sets the transparency, where 1 is opaque and 0 allows
                 * any underlying color through. Notice how this mixes well with
                 * the "Edge BG" channel.
                 */
                colors[point.index] = LXColor.hsba(
                        hue,
                        saturation,
                        brightness,
                        alphaWave
                    );

                /* If we had instead used the wave as a brightness scale,
                 * the VJ would have to fade this pattern down to blend with
                 * other channels. You can uncomment to experience this in the
                 * LX Audio Examples project.
                 */
                // colors[point.index] = LXColor.hsb(
                //         hue,
                //         saturation,
                //         brightness * alphaWave
                // );
            }
        }
    }
}
