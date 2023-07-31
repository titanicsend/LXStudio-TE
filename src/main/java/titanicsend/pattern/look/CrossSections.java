package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.SinLFO;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.transform.LXVector;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import static heronarts.lx.color.LXColor.add;
import static java.lang.Math.max;
import static titanicsend.util.TEMath.clamp;

@LXCategory("Look Java Patterns")
public class CrossSections extends CrossSectionsBase {

    public CrossSections(LX lx) {
        super(lx);
        addParams();
    }

    public void runTEAudioPattern(double deltaMs) {
        clearPixels();  // Sets all pixels to transparent for starters
        updateXYZVals();

        // TODO: this requires common controls to add a gradient param - but this prevents addParams() from adding anything to the UI.
        //        int baseColor = getGradientColor(1.0f); // TODO(look): is 1.0 the right 'lerp' value?
        //        float hue = LXColor.h(baseColor);
        //        System.out.println(String.format("hue = %f", hue));

        float hue = LXColor.h(LXColor.BLUE);
        for (LXPoint p : model.points) {
            int c = 0;
            c = add(c, LXColor.hsb(
                    hue + p.x / (10 * ranges.x) + p.y / (3 * ranges.y),
                    clamp(140 - 110.0f * Math.abs(p.y - maxs.y) / ranges.y, 0, 100),
                    max(0, xlv - xwv * Math.abs(p.x - xv) / ranges.x)
            ));
            c = add(c, LXColor.hsb(
                    hue + 80 + p.y / (10 * ranges.y),
                    clamp(140 - 110.0f * Math.abs(p.x - maxs.x) / ranges.x, 0, 100),
                    max(0, ylv - ywv * Math.abs(p.y - yv) / ranges.y)
            ));
            c = add(c, LXColor.hsb(
                    hue + 160 + p.z / (10 * ranges.z) + p.y / (2 * ranges.y),
                    clamp(140 - 110.0f * Math.abs(p.z - maxs.z) / ranges.z, 0, 100),
                    max(0, zlv - zwv * Math.abs(p.z - zv) / ranges.z)
            ));
            colors[p.index] = c;
        }
    }
}
