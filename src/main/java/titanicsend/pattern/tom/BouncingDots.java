package titanicsend.pattern.tom;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.SinLFO;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.FunctionalParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEPattern;

import static titanicsend.util.TEColor.TRANSPARENT;

@LXCategory("Edge FG")
public class BouncingDots extends TEPattern {
    public final DiscreteParameter dotWidth =
            new DiscreteParameter("Width", 5, 1, 25)
                    .setDescription("Dot width");

    protected final CompoundParameter rate = (CompoundParameter)
            new CompoundParameter("Rate", .25, .01, 2)
                    .setExponent(2)
                    .setUnits(LXParameter.Units.HERTZ)
                    .setDescription("Rate of the rotation");

    protected final SinLFO phase = new SinLFO(0, 1, new FunctionalParameter() {
        public double getValue() {
            return 1000 / rate.getValue();
        }
    });

    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PRIMARY,
                    "Color of the dots");

    public BouncingDots(LX lx) {
        super(lx);
        startModulator(this.phase);
        addParameter("rate", this.rate);
        addParameter("width", this.dotWidth);
    }

    public void run(double deltaMs) {
        float phase = this.phase.getValuef();

        int dotColor = this.color.calcColor();
        int dotWidth = this.dotWidth.getValuei();
        for (TEEdgeModel edge : model.edgesById.values()) {
            int target = (int) (edge.size * phase);
            int i = 0;
            for (LXPoint point : edge.points) {
                int noHigherThan = target + dotWidth / 2;
                int tooLow = noHigherThan - dotWidth;
                if (i <= tooLow || i > noHigherThan) {
                    colors[point.index] = TRANSPARENT;
                } else {
                    colors[point.index] = dotColor;
                }
                i++;
            }
        }
    }
}
