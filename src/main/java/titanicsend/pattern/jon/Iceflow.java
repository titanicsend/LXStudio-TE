package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.transform.LXVector;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.util.TEMath;

// Swirling, mostly transparent partly frozen fluid thing...
//
// This is *very* music reactive - try it on something with a solid beat,
// and play with the energy setting!
@LXCategory("Other")
public class Iceflow extends TEAudioPattern {

    // pattern variables
    int iterations = 9;
    float focusVal;
    float phase;
    LXVector offsets;

    float[] expTable;

    // Controls
    protected final CompoundParameter focus = (CompoundParameter)
            new CompoundParameter("Detail", 5, 1, 20)
                    .setExponent(1)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Detail/Sharpness");

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .225, 0, 1)
                    .setDescription("Music Reactivity");

    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PANEL,
                    "Panel Color");

    public Iceflow(LX lx) {
        super(lx);
        addParameter("focus",focus);
        addParameter("energy", energy);

        // set default per iteration coordinate offset rate
        offsets = new LXVector(0.5f,0.3f,1.15f);

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
            x = Math.abs(x) / dot - offsets.x;
            y = Math.abs(y) / dot - offsets.y;
            z = Math.abs(z) / dot - (offsets.z-phase);

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

        // Sound reactivity, controlled by energy control.
        float e = energy.getValuef();
        phase = 0.05f * TEMath.wavef((float) (measure() * 2)) * e;
        float bass =  avgBass.getValuef() * e;

        // texture movement w/ bass and energy.
        // "Shaken, not stirred"
        float k = 0.2f + e * bass;
        float xOffs = k * (float) Math.sin(time);
        float yOffs = k * (float) Math.cos(time);

        // per pixel calculations
        for (LXPoint point : model.panelPoints) {

            // set origin of normalized coordinates to model centroid
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
                    alpha
            );
        }
    }
}
