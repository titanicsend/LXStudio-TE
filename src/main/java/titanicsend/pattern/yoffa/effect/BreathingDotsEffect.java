package titanicsend.pattern.yoffa.effect;

import heronarts.lx.Tempo;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.LXRangeModulator;
import heronarts.lx.modulator.TriangleLFO;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.awt.*;
import java.util.*;
import java.util.List;

public class BreathingDotsEffect extends PatternEffect {

    private final Random random = new Random();

    private static final int DURATION_MILLIS = 2000;
    private static final double MAX_POINTS_DIVIDER = 25;
    private final double pointsPerMilli;
    private final Map<LXPoint, LXRangeModulator> breathingPoints = new HashMap<>();
    private final Set<LXPoint> extraShinyPoints = new HashSet<>();

    public BreathingDotsEffect(PatternTarget patternTarget) {
        super(patternTarget);
        double maxPoints = getAllPoints().size() / MAX_POINTS_DIVIDER;
        this.pointsPerMilli = maxPoints / DURATION_MILLIS;
    }

    public void run(double deltaMs) {
        int baseColor = pattern.getSwatchColor(TEPattern.ColorType.PRIMARY);

        List<LXPoint> availablePoints = new ArrayList<>(getAllPoints());
        for (LXPoint point : getAllPoints()) {
            Double status = getBreathStatus(point);
            if (status == null) {
                setColor(point, LXColor.BLACK);
                availablePoints.add(point);
            } else {
                double brightness = extraShinyPoints.contains(point) ? 100 : 50;
                setColor(point, LXColor.hsba(
                        LXColor.h(baseColor),
                        LXColor.s(baseColor) * status,
                        brightness * status,
                        100
                ));
            }
        }

        for (int i = 0; i < pointsPerMilli * deltaMs; i++) {
            startBreathing(availablePoints.get(random.nextInt(availablePoints.size())));
        }
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of();
    }

    private void startBreathing(LXPoint point) {
        LXRangeModulator phase = new TriangleLFO(0, 1, DURATION_MILLIS);
        phase.setLooping(false);
        pattern.startModulator(phase);
        breathingPoints.put(point, phase);
        if (random.nextBoolean()) {
            extraShinyPoints.add(point);
        }
    }

    private Double getBreathStatus(LXPoint point) {
        LXRangeModulator phase = breathingPoints.get(point);
        if (phase == null) {
            return null;
        }
        if (phase.finished()) {
            breathingPoints.remove(point);
            extraShinyPoints.remove(point);
            pattern.removeModulator(phase);
            return null;
        }
        double curValue = phase.getValue();
        return curValue > .5 ? 2 * (1 - curValue) : 2 * curValue;
    }

    @Override
    public void onPatternActive() {
        breathingPoints.clear();
        extraShinyPoints.clear();
    }

}
