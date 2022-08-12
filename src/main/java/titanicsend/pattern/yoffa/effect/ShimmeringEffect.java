package titanicsend.pattern.yoffa.effect;

import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.util.Dimensions;

import java.util.Collection;
import java.util.List;

@LXCategory("Edge FG")
public class ShimmeringEffect extends PatternEffect {

    private static final double pulseLength = .1;
    private int direction;
    private int lastBeat;

    public ShimmeringEffect(PatternTarget target) {
        super(target);
    }


    @Override
    public void onPatternActive() {
        lastBeat = 0;
        direction = 0;
    }

    @Override
    public void run(double deltaMs) {
        //todo pattern effects should infer colors, not specify
        int baseColor = pattern.getSwatchColor(TEPattern.ColorType.PRIMARY);
        double basis = getTempo().basis();
        int beatCount = getTempo().beatCount();
        int beatsPerMeasure = getTempo().beatsPerMeasure.getValuei();

        if (beatCount > lastBeat && beatCount % beatsPerMeasure == 0) {
            lastBeat = getTempo().beatCount();
            direction = direction == 3 ? 0 : direction + 1;
        }

        for (LXPoint point : getAllPoints()) {
            double distanceFromTarget = getDistanceFromTarget(point, basis, pointsToCanvas.get(point));
            double alpha = 100;
            if ((distanceFromTarget > 0 && beatCount % beatsPerMeasure == 0) ||
                    (distanceFromTarget < 0 && beatCount % beatsPerMeasure == beatsPerMeasure - 1)) {
                alpha = 0;
            }
            double brightness = Math.abs(distanceFromTarget) > pulseLength ? 50 :
                    50 + 50 * (1 - (Math.abs(distanceFromTarget) / pulseLength));
            setColor(point, LXColor.hsba(
                    LXColor.h(baseColor),
                    LXColor.s(baseColor),
                    brightness,
                    alpha
                    ));
        }
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of();
    }

    private double getDistanceFromTarget(LXPoint point, double basis, Dimensions dimensions) {
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
