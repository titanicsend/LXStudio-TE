package titanicsend.pattern.jeff;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.modulator.DampedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.transform.LXVector;
import heronarts.lx.utils.LXUtils;
import titanicsend.pattern.TEAudioPattern;

/* Smoke
 * Adapted from https://www.shadertoy.com/view/NllBzl#
 * by https://www.shadertoy.com/user/Maadahmed
 * Which is released under https://creativecommons.org/licenses/by-nc-sa/3.0/deed.en_US
 *
 * Several hours spent boosting FPS. Lessons learned:
 *   - Use floats, avoid frequent casting
 *   - Memoize parameter values for frequent inner loops
 *   - Cache everything; compute everything at the highest level (least frequent)
 */

@LXCategory("Combo FG")
public class Smoke extends TEAudioPattern {
    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PRIMARY,
                    "Primary color of the field");

    // Magic numbers for parameter ranges come from testing and are pattern-specific.
    // A better practice might be to transform human parameter units (0-100)
    // to the arbitrary (e.g. (-.5..2)) or other ranges desired.
    public final CompoundParameter energy =
            new CompoundParameter("Energy", 1, -.5, 2)
                    .setDescription("Gain, starting from the top of car.");

    public final CompoundParameter contrast =
            new CompoundParameter("Contrast", 1, 2.6, 0)
                    .setDescription("Contrast between dark and light areas");

    public final CompoundParameter detail =
            new CompoundParameter("Detail", .85, 0, 1)
                    .setDescription("Detail (iteration octaves). Costs FPS.");

    public final CompoundParameter scale =
            new CompoundParameter("Scale", 1, 10, .3)
                    .setExponent(1./3)
                    .setDescription("Zoom in/out on the field");
    // Sensitivity to scale highlights midi controller's low, "steppy" resolution,
    // so we damp this parameter over time.
    public final DampedParameter smoothScale =
            new DampedParameter(scale, 100 , .7, .7);

    public final CompoundParameter audioResponsiveness =
            new CompoundParameter("Audio", 1, 0, 2)
                    .setDescription("Audio responsiveness");

    // Precompute transformed points in space to speed FPS
    SmokePoint[] transformedPoints;

    // Memoizing these boosts FPS significantly due to the many iterations
    protected float scaleValue, frequency;
    static final float PI = (float) Math.PI;

    public Smoke(LX lx) {
        super(lx);
        addParameter("energy", energy);
        addParameter("contrast", contrast);
        addParameter("detail", detail);
        addParameter("scale", scale);
        startModulator(smoothScale);
        addParameter("audioResponsiveness", audioResponsiveness);
        transformedPoints = transformPoints();
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        // Ramp to 1 across 8 beats, which is the nominal period for this pattern
        float time = (float) lx.engine.tempo.getCompositeBasis() % 8;

        // Octaves of the pattern to build in via iteration
        float octaves = (float) detail.getNormalized() * 8;

        // The motion of the field is modulated by audio level
        float sinFieldPhase = (time + (getVolumeRatiof() - 1) * audioResponsiveness.getValuef()) * PI / 8.f;

        scaleValue = smoothScale.getValuef();

        for (SmokePoint point : transformedPoints) {
            if (point == null) continue;  // Skip gap pixels
            float yOut = point.y;

            // For all octaves except the last, build the iterated sine field
            for (float i = 1.f; i <= octaves - 1; i++) {
                frequency = i * i * scaleValue;
                yOut += 0.1 * Math.sin(yOut * frequency + sinFieldPhase) *
                              Math.sin(point.z * frequency + sinFieldPhase);
            }
            // Lerp into last octave. Boosts FPS to dupe this code from the loop above.
            int lastOctave = (int) octaves;
            if (octaves - lastOctave < 1.f) {
                frequency = lastOctave * lastOctave * scaleValue;;
                yOut += 0.1 * (octaves - lastOctave) *
                        Math.sin(yOut * frequency + sinFieldPhase) *
                        Math.sin(point.z * frequency + sinFieldPhase);
            }

            // Normalize yOut to 0..1. Magic numbers for parameter ranges come from testing.
            // Repeat iterations of += Sin(), for up to 8 octaves yield yOut in (-1.3..1.3).
            float yOutNorm  = LXUtils.clampf(
                    (energy.getValuef() + yOut) / contrast.getValuef(),
                    0, 1);

            colors[point.index] = LXColor.hsa(
                    LXColor.h(color.getColor()),

                    // Make the selected color white (icy!) for the top half.
                    LXUtils.clampf(LXColor.s(color.getColor()) - point.topDesat, 0, 100),

                    // The yOut value is the alpha. Low values let through the background (or black).
                    yOutNorm
            );
        }

    }

    // This enables adding a custom field to the points just for this pattern
    protected static class SmokePoint extends LXVector {
        public float topDesat = 0;

        public SmokePoint(LXVector that) {
            super(that);
        }
    }

    // Cache the point coordinates transform to boost frame rate
    protected SmokePoint[] transformPoints() {
        SmokePoint[] result = new SmokePoint[model.points.length];
        for (int i = 0; i < model.points.length; i++) {
            LXVector point = new LXVector(model.points[i]);
            // Centered vertical coordinates, with y in range [-1,1]:
            LXVector transformedPoint = point.mult(2).sub(0, model.yRange, 0).div(model.yMax);
            SmokePoint thisPoint = new SmokePoint(transformedPoint);

            // topDesat makes the top points white. Return 0 for the bottom half of this pattern
            // where y is in [-1..0], then slowly ease into 1 when y is in [0..1]
            float yClamped = LXUtils.clampf(thisPoint.y, -1, 1);
            thisPoint.topDesat = (yClamped < 0) ? 0 : (float) Math.sqrt(yClamped) * 100;
            if (this.modelTE.isGapPoint(model.points[i]))
                result[i] = null;
            else
                result[i] = thisPoint;
        }
        return result;
    }
}
