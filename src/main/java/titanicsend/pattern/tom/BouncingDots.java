package titanicsend.pattern.tom;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.SinLFO;
import heronarts.lx.parameter.FunctionalParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import static titanicsend.util.TEColor.TRANSPARENT;

@LXCategory("Edge FG")
public class BouncingDots extends TEPerformancePattern {
    final int MIN_DOT_SIZE = 1;
    final int MAX_DOT_SIZE = 50;
    final int DEFAULT_DOT_SIZE = 6;

    protected final SinLFO phase = new SinLFO(0, 1, new FunctionalParameter() {
        public double getValue() {
            return 1000 / getSpeed();
        }
    });

    public BouncingDots(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        // start our sine modulator
        startModulator(this.phase);

        // add common controls
        controls.setRange(TEControlTag.SIZE, DEFAULT_DOT_SIZE, MIN_DOT_SIZE, MAX_DOT_SIZE)
                .setUnits(LXParameter.Units.INTEGER);
        addCommonControls();
    }

    @Override
    protected void runTEAudioPattern(double deltaMs) {
        float phase = this.phase.getValuef();

        int dotColor = calcColor();
        int dotWidth = (int)(getSize());
        for (TEEdgeModel edge : modelTE.edgesById.values()) {
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
