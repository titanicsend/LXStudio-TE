package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.util.TEMath;
import processing.core.*;

@LXCategory("Other")
public class Iceberg extends TEAudioPattern {

    // pattern variables
    int iterations = 9;
    float focusVal;

    float[] expTable;

    // Controls
    protected final CompoundParameter focus = (CompoundParameter)
            new CompoundParameter("Focus", 5, 1, 20)
                    .setExponent(1)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Image Sharpness");

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .5, 0, 1)
                    .setDescription("Stars sparkle and move");

    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PANEL,
                    "Panel Color");

    public Iceberg(LX lx) {
        super(lx);
        addParameter("focus",focus);
        addParameter("energy", energy);

        // build lookup table for texture layer strength
        expTable = new float[iterations];
        for (int i = 0; i < iterations; i++) {
            expTable[i] = (float) Math.exp(-(double)i / focus.getValue());
        }
    }

    // get the "ice" field density at the specified point
    float mapDensity( float x,float y, float z) {
        float prev = 0f;
        float acc = 0f;
        float totalWeight = 0f;
        for (int i = 0; i < iterations; i++) {

            // normalize(ish) and offset our offset coordinate vector
            float dot = x * x + y * y + z * z;
            x = Math.abs(x) / dot - 0.5f;
            y = Math.abs(y) / dot - 0.3f;
            z = Math.abs(z) / dot - 1.15f;

            float layerWeight = expTable[i];
            float diff = Math.abs(dot-prev);  diff = diff * diff;
            acc += layerWeight * Math.exp(focusVal * diff);
            totalWeight += layerWeight;
            prev = dot + layerWeight;
        }
        return (float) Math.max(0., acc / totalWeight);
    }

    public void runTEAudioPattern(double deltaMs) {

        // whole system is constantly moving over time.
        double time = ((double) System.currentTimeMillis()) / 1000;
        focusVal = -focus.getValuef();

        // pick up the current set color
        int baseColor = color.calcColor();
        float baseHue = LXColor.h(baseColor);
        float baseSat = LXColor.s(baseColor);
        float baseBri = LXColor.b(baseColor);
        float alpha = 100f;

        // Sound reactivity, amount controlled by energy control setting.
        double e = energy.getValue();
        double beat = lx.engine.tempo.bpm();
        double t1 = time * beat;

        double phase = lx.engine.tempo.basis() * e;
        double bass =  avgBass.getValue() * e;

        // texture movement - icebergs are
        float xOffs = .2f * (float) Math.sin(time);
        float yOffs = .2f * (float) Math.cos(time);

        // per pixel calculations
        for (LXPoint point : model.panelPoints) {

            // translate and rescale normalized coords from -1 to 1
            float u = (point.zn - 0.5f);
            float v = (point.yn - 0.5f);
            float density = 0.0125f;             // minimum brightness level

            for (float i = 1; i < 4; i++) {
                float x = u + (xOffs / i);
                float y = v + (yOffs / i);
                density += mapDensity(x,y,density);
            }




            density = Math.max(0f,Math.min(1f,density));   // clamp to 0 to 1 range
            density *= density;
            colors[point.index] = LXColor.hsba(
                    baseHue,
                    (1-density) * baseSat,
                    density * baseBri,
                    100f
            );
        }
    }
}
