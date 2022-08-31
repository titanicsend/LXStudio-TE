package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.transform.LXProjection;
import heronarts.lx.transform.LXVector;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;
import titanicsend.util.TEColor;

import java.util.ArrayList;

@LXCategory(LXCategory.TEXTURE)
public class EdgeSieve extends BasicEffect {
    private final DiscreteParameter gap = new DiscreteParameter("Gap", 0, 0, 11)
            .setDescription("Number of off pixels between on pixels");
    private final DiscreteParameter length = new DiscreteParameter("Length", 1, 1, 7)
            .setDescription("Number of on pixels between gaps");

    private final CompoundParameter offsetFrac = new CompoundParameter("Offset", 0, 0, 1)
            .setDescription("Phase offset, in fraction of overall length");

    protected int runLength;
    protected int offset;

    public EdgeSieve(LX lx) {
        super(lx);
        addParameter("gap", this.gap);
        addParameter("length", this.length);
        addParameter("offset", this.offsetFrac);
        gap.bang();
    }

    @Override
    protected void run(double deltaMs, double enabledAmount) {
        if (enabledAmount > 0) {
            for (TEEdgeModel edge : this.model.edgesById.values()) {
                for (TEEdgeModel.Point point : edge.points) {
                    if (Math.floorMod(point.i - offset, runLength) >= length.getValuei()) {
                        colors[point.index] = LXColor.BLACK;
                    }
                }
            }
        }
    }

    public void onParameterChanged(LXParameter parameter) {
        super.onParameterChanged(parameter);
        runLength = gap.getValuei() + length.getValuei();
        offset = (int) (offsetFrac.getNormalized() * runLength);
    }
}