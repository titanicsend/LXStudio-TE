package titanicsend.pattern.yoffa.edges;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.utils.LXUtils;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.TEPattern;
import titanicsend.util.Dimensions;

@LXCategory("Edge FG")
public class PowerGridPattern extends TEAudioPattern {

    private static final double pulseLength = .1;
    private final Dimensions dimensions;

    private int direction;
    private int lastBeat;

    public PowerGridPattern(LX lx) {
        super(lx);
        this.dimensions = Dimensions.fromModels(model.edgesById.values());
    }

    @Override
    public void onActive() {
        lastBeat = 0;
        direction = 0;
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        int baseColor = getSwatchColor(ColorType.EDGE);
        double basis = lx.engine.tempo.basis();
        int beatCount = lx.engine.tempo.beatCount();
        int beatsPerMeasure = lx.engine.tempo.beatsPerMeasure.getValuei();

        if (beatCount > lastBeat && beatCount % beatsPerMeasure == 0) {
            lastBeat = lx.engine.tempo.beatCount();
            direction = direction == 3 ? 0 : direction + 1;
        }

        for (TEEdgeModel edge : model.edgesById.values()) {
            for (LXPoint point : edge.points) {
                double distanceFromTarget = getDistanceFromTarget(point, basis);
                double alpha = 100;
                if ((distanceFromTarget > 0 && beatCount % beatsPerMeasure == 0) ||
                        (distanceFromTarget < 0 && beatCount % beatsPerMeasure == beatsPerMeasure - 1)) {
                    alpha = 0;
                }
                double brightness = Math.abs(distanceFromTarget) > pulseLength ? 50 :
                        50 + 50 * (1 - (Math.abs(distanceFromTarget) / pulseLength));
                colors[point.index] = LXColor.hsba(
                        LXColor.h(baseColor),
                        LXColor.s(baseColor),
                        brightness,
                        alpha
                        );
            }
        }
    }

    private double getDistanceFromTarget(LXPoint point, double basis) {
        double current;
        double target;
        if (direction == 0 || direction == 2) {
            target = (dimensions.getMaxYn() - dimensions.getMinYn()) * basis + dimensions.getMinYn();
            current = point.yn;
        } else {
            target = (dimensions.getMaxZn() - dimensions.getMinZn()) * basis + dimensions.getMinZn();
            current = point.zn;
        }
        if (direction > 1) {
            current = 1 - current;
        }
        return current - target;
    }

}
