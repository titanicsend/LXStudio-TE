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
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import static heronarts.lx.color.LXColor.add;
import static java.lang.Math.max;
import static titanicsend.util.TEMath.clamp;

public abstract class CrossSectionsBase extends TEPerformancePattern {

    public final SinLFO x;
    public final SinLFO y;
    public final SinLFO z;
    protected float xv, yv, zv;

    protected final CompoundParameter xr = new CompoundParameter("xSpd", 0.7);
    protected final CompoundParameter yr = new CompoundParameter("ySpd", 0.6);
    protected final CompoundParameter zr = new CompoundParameter("zSpd", 0.5);

    protected final CompoundParameter xl = new CompoundParameter("xLvl", 1);
    protected final CompoundParameter yl = new CompoundParameter("yLvl", 1);
    protected final CompoundParameter zl = new CompoundParameter("zLvl", 0.5);
    protected float xlv, ylv, zlv;

    protected final CompoundParameter xw = new CompoundParameter("xSize", 0.3);
    protected final CompoundParameter yw = new CompoundParameter("ySize", 0.3);
    protected final CompoundParameter zw = new CompoundParameter("zSize", 0.3);
    protected float xwv, ywv, zwv;

    protected final LXVector maxs;
    protected final LXVector mins;
    protected final LXVector ranges;

    public CrossSectionsBase(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);
        mins = new LXVector(modelTE.boundaryPoints.minXBoundaryPoint.x, modelTE.boundaryPoints.minYBoundaryPoint.y, modelTE.boundaryPoints.minZBoundaryPoint.z);
        maxs = new LXVector(modelTE.boundaryPoints.maxXBoundaryPoint.x, modelTE.boundaryPoints.maxYBoundaryPoint.y, modelTE.boundaryPoints.maxZBoundaryPoint.z);
        ranges = new LXVector(maxs.x - mins.x, maxs.y - mins.y, maxs.z - mins.z);

        x = new SinLFO(mins.x, maxs.x, 5000);
        y = new SinLFO(mins.y, maxs.y, 6000);
        z = new SinLFO(mins.z, maxs.z, 7000);
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

    public abstract void runTEAudioPattern(double deltaMs);
}
