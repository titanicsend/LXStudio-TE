package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.*;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.color.LinkedColorParameter;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.util.TEMath;

// All LED platforms, no matter how large, must have KITT!
@LXCategory("Edge FG")
public class EdgeKITT extends TEAudioPattern {
    float tailPct = 0.5f;
    float cycleFactor = 2;
    float lastTimerValue = 0;

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .1, 0, 1)
                    .setDescription("Swing!");

    protected final CompoundParameter cyclesPerMeasure = (CompoundParameter)
            new CompoundParameter("Cycles", 2, 1, 8)
                    .setExponent(1)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Cycles per Measure");
    public final CompoundParameter tail =
            new CompoundParameter("Tail", 0.5, 0, 1)
                    .setDescription("Tail length");


    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PRIMARY,
                    "Color");

    public EdgeKITT(LX lx) {
        super(lx);
        addParameter("energy", energy);
        addParameter("cyclesPerMeasure", cyclesPerMeasure);
        addParameter("tail", tail);
    }

    float square(float n, float dutyCycle) {
        return (Math.abs(TEMath.fract(n)) <= dutyCycle) ? 1.0f : 0.0f;
    }

    float trianglef(float n) {
        return 2f * (0.5f - (float) Math.abs(TEMath.fract(n) - 0.5));
    }

    public void runTEAudioPattern(double deltaMs) {
        updateGradients();

        // pick up the current color
        int baseColor = this.color.calcColor();

        // 0..1 ramp (sawtooth) of current position within a 4-beat measure.
        float measure = (float) wholeNote();  // lx.engine.tempo.basis()
        float t1 = (float) TEMath.fract(measure * cycleFactor);

        // To keep things as smooth as possible, allow time division multiplier changes only
        // at the start of a KITT back/forth cycle.  Depending on what else is running, we may
        // be off by a few ms, but this will get close enough to avoid glitchiness.
        if (t1 < lastTimerValue) {
            cycleFactor = (float) Math.floor(cyclesPerMeasure.getValuef());
        }
        lastTimerValue = t1;

        // use a little exponential magic to implement the energy control swing.
        t1 = (float) Math.pow(t1, 1 + energy.getValue());

        tailPct = tail.getValuef();

// From a discussion of frame buffer-less, multidimensional KITT patterns
// on the Pixelblaze forum.
// https://forum.electromage.com/t/kitt-without-arrays/1219
        for (TEEdgeModel edge : modelTE.getAllEdges()) {
            for (TEEdgeModel.Point point : edge.points) {

                float x = 0.5f * point.frac;
                float pct1 = x - t1;
                float pct2 = -x - t1;

                float w1 = Math.max(0f, (tailPct - 1f + trianglef(pct1) * square(pct1, .5f)) / tailPct);
                float w2 = Math.max(0f, (tailPct - 1f + trianglef(pct2) * square(pct2, .5f)) / tailPct);
                float bri = (w1 * w1) + (w2 * w2);  // gamma correct both waves before combining
                bri = bri * 255f;  // scale for output

                // clear and reset alpha channel
                baseColor = baseColor & ~LXColor.ALPHA_MASK;
                baseColor = baseColor | (((int) bri) << LXColor.ALPHA_SHIFT);
                colors[point.index] = baseColor;
            }
        }
    }
}