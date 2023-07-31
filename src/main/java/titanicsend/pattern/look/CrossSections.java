package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.SinLFO;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import static heronarts.lx.color.LXColor.add;
import static java.lang.Math.max;
import static titanicsend.util.TEMath.clamp;

@LXCategory("Look Java Patterns")
public class CrossSections extends TEPerformancePattern {

    public final SinLFO x;
    public final SinLFO y;
    public final SinLFO z;

    public final float minX;
    public final float maxX;
    public final float minY;
    public final float maxY;
    public final float minZ;
    public final float maxZ;

    public final float xRange;
    public final float yRange;
    public final float zRange;


    final CompoundParameter xl = new CompoundParameter("xLvl", 1);
    final CompoundParameter yl = new CompoundParameter("yLvl", 1);
    final CompoundParameter zl = new CompoundParameter("zLvl", 0.5);

    final CompoundParameter xr = new CompoundParameter("xSpd", 0.7);
    final CompoundParameter yr = new CompoundParameter("ySpd", 0.6);
    final CompoundParameter zr = new CompoundParameter("zSpd", 0.5);

    final CompoundParameter xw = new CompoundParameter("xSize", 0.3);
    final CompoundParameter yw = new CompoundParameter("ySize", 0.3);
    final CompoundParameter zw = new CompoundParameter("zSize", 0.3);

    public CrossSections(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        minX = modelTE.boundaryPoints.minXBoundaryPoint.x;
        maxX = modelTE.boundaryPoints.maxXBoundaryPoint.x;
        minY = modelTE.boundaryPoints.minYBoundaryPoint.y;
        maxY = modelTE.boundaryPoints.maxYBoundaryPoint.y;
        minZ = modelTE.boundaryPoints.minZBoundaryPoint.z;
        maxZ = modelTE.boundaryPoints.maxZBoundaryPoint.z;
        xRange = (maxX - minX);
        yRange = (maxY - minY);
        zRange = (maxZ - minZ);

        x = new SinLFO(minX, maxX, 5000);
        y = new SinLFO(minY, maxY, 6000);
        z = new SinLFO(minZ, maxZ, 7000);
        addParams();
//        addCommonControls();

        addModulator(x).trigger();
        addModulator(y).trigger();
        addModulator(z).trigger();
    }

    protected void addParams() {
        addParameter("xr", xr);
        addParameter("yr", yr);
        addParameter("zr", zr);
        addParameter("xl", xl);
        addParameter("yl", yl);
        addParameter("zl", zl);
        addParameter("xw", xw);
        addParameter("yw", yw);
        addParameter("zw", zw);
    }

    public void onParameterChanged(LXParameter p) {
        if (p == xr) {
            x.setPeriod(10000 - 8800 * p.getValuef());
        } else if (p == yr) {
            y.setPeriod(10000 - 9000 * p.getValuef());
        } else if (p == zr) {
            z.setPeriod(10000 - 9000 * p.getValuef());
        }
    }

    float xv, yv, zv;

    protected void updateXYZVals() {
        xv = x.getValuef();
        yv = y.getValuef();
        zv = z.getValuef();
    }

    public void runTEAudioPattern(double deltaMs) {
        clearPixels();  // Sets all pixels to transparent for starters

        updateXYZVals();

        float xlv = 100 * xl.getValuef();
        float ylv = 100 * yl.getValuef();
        float zlv = 100 * zl.getValuef();

        float xwv = 100f / (xw.getValuef());
        float ywv = 100f / (yw.getValuef());
        float zwv = 100f / (zw.getValuef());

        // TODO: this requires common controls to add a gradient param - but this prevents addParams() from adding anything to the UI.
        //        int baseColor = getGradientColor(1.0f); // TODO(look): is 1.0 the right 'lerp' value?
        //        float hue = LXColor.h(baseColor);
        //        System.out.println(String.format("hue = %f", hue));

        float hue = LXColor.h(LXColor.BLUE);


        for (LXPoint p : model.points) {
//            System.out.printf("Math.abs(p.y - maxY) / yRange = %f\n", Math.abs(p.y - maxY) / yRange);
//            System.out.printf("Math.abs(p.x - maxX) / xRange = %f\n", Math.abs(p.x - maxX) / xRange);
//            System.out.printf("Math.abs(p.z - maxZ) / zRange = %f\n", Math.abs(p.z - maxZ) / zRange);
//                System.out.printf("xlv=%f, xwv=%f, p.x=%f, xv=%f, Math.abs(p.x-xv)=%f\n", xlv, xwv, p.x, xv, Math.abs(p.x-xv) / maxX);
//                System.out.printf("xlv - xwv * Math.abs(p.x - xv) = %f, max(0, xlv - xwv * Math.abs(p.x - xv)) = %f\n\n", xlv - xwv * Math.abs(p.x - xv) / maxX, max(0, xlv - xwv * Math.abs(p.x - xv) / maxX));
            int c = 0;
            c = add(c, LXColor.hsb(
                    hue + p.x / (10 * xRange) + p.y / (3 * yRange),
                    clamp(140 - 110.0f * Math.abs(p.y - maxY) / yRange, 0, 100),
                    max(0, xlv - xwv * Math.abs(p.x - xv) / xRange)
            ));

            c = add(c, LXColor.hsb(
                    hue + 80 + p.y / (10 * yRange), //LXColor.h(LXColor.RED),
                    clamp(140 - 110.0f * Math.abs(p.x - maxX) / xRange, 0, 100),
                    max(0, ylv - ywv * Math.abs(p.y - yv) / yRange)
            ));
            c = add(c, LXColor.hsb(
                    hue + 160 + p.z / (10 * zRange) + p.y / (2 * yRange), //LXColor.h(LXColor.GREEN),
                    clamp(140 - 110.0f * Math.abs(p.z - maxZ) / zRange, 0, 100),
                    max(0, zlv - zwv * Math.abs(p.z - zv) / zRange)
            ));
            colors[p.index] = c;
        }
    }
}
