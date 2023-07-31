package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.pattern.jon.TEControlTag;

import static heronarts.lx.color.LXColor.add;
import static java.lang.Math.max;
import static titanicsend.util.TEMath.clamp;

@LXCategory("Look Java Patterns")
public class CrossSections extends CrossSectionsBase {

    public CrossSections(LX lx) {
        super(lx);

        // unused: quantity
        // unused: spin
        controls.setRange(TEControlTag.SIZE, 0.3, 0.05, 0.75);

        addCommonControls();
        addParams();
    }

    protected void updateXYZVals() {
        // LFO vals
        xv = x.getValuef();
        yv = y.getValuef();
        zv = z.getValuef();
        // levels
        xlv = 100 * xl.getValuef();
        ylv = 100 * yl.getValuef();
        zlv = 100 * zl.getValuef();
//        // widths
        xwv = 100f / (xw.getValuef());
        ywv = 100f / (yw.getValuef());
        zwv = 100f / (zw.getValuef());
//        xwv = (float) getSize();
//        ywv = (float) getSize();
//        zwv = (float) getSize();
    }

    public void runTEAudioPattern(double deltaMs) {
        clearPixels();  // Sets all pixels to transparent for starters
        updateXYZVals();

        for (LXPoint p : model.points) {
            int c = 0;
            c = add(c, LXColor.hsb(
                    xHue(),
                    xSat(p),
                    xBrightness(p, xv)
            ));
            c = add(c, LXColor.hsb(
                    yHue(),
                    ySat(p),
                    yBrightness(p, yv)
            ));
            c = add(c, LXColor.hsb(
                    zHue(),
                    zSat(p),
                    zBrightness(p, zv)
            ));
            colors[p.index] = c;
        }
    }

    protected float xBrightness(LXPoint p, float level) {
        return max(0, xlv - xwv * Math.abs(p.x - level) / ranges.x) * (float) getBrightness();
    }
    protected float yBrightness(LXPoint p, float level) {
        return max(0, ylv - ywv * Math.abs(p.y - level) / ranges.y) * (float) getBrightness();
    }
    protected float zBrightness(LXPoint p, float level) {
        return max(0, zlv - zwv * Math.abs(p.z - level) / ranges.z) * (float) getBrightness();
    }

    protected float xHue () {
        return LXColor.h(calcColor()); // + p.x / (10 * ranges.x) + p.y / (3 * ranges.y);
    }
    protected float yHue () {
        return LXColor.h(calcColor()); // + 80 + p.y / (10 * ranges.y),
    }
    protected float zHue () {
        return LXColor.h(calcColor2()); // + 160 + p.z / (10 * ranges.z) + p.y / (2 * ranges.y),
    }

    protected float xSat(LXPoint p) {
        return clamp(140 - 110.0f * Math.abs(p.y - maxs.y) / ranges.y, 0, 100);
    }
    protected float ySat(LXPoint p) {
        return clamp(140 - 110.0f * Math.abs(p.x - maxs.x) / ranges.x, 0, 100);
    }
    protected float zSat(LXPoint p) {
        return clamp(140 - 110.0f * Math.abs(p.z - maxs.z) / ranges.z, 0, 100);
    }
}
