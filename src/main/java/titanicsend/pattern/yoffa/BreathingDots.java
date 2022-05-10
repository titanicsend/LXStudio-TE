package titanicsend.pattern.yoffa;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.LXRangeModulator;
import heronarts.lx.modulator.TriangleLFO;
import titanicsend.pattern.TEPattern;

import java.util.*;

@LXCategory("Edge FG")
public class BreathingDots extends TEPattern {

    private final Random random = new Random();

    private static final int DURATION_MILLIS = 2000;
    private final int maxPoints = model.panelPoints.size() / 25;
    private final double pointsPerMilli = (double) maxPoints / DURATION_MILLIS;

    private final Map<LXPoint, LXRangeModulator> breathingPoints = new HashMap<>();
    private final Set<LXPoint> extraShinyPoints = new HashSet<>();

    public BreathingDots(LX lx) {
        super(lx);
    }

    @Override
    public void onActive() {
        breathingPoints.clear();
        extraShinyPoints.clear();
    }

    public void run(double deltaMs) {
        int baseColor = getSwatchColor(ColorType.PANEL);

        List<LXPoint> availablePoints = new ArrayList<>(model.panelPoints);
        for (LXPoint point : model.panelPoints) {
            Double status = getBreathStatus(point);
            if (status == null) {
                colors[point.index] = LXColor.BLACK;
                availablePoints.add(point);
            } else {
                double brightness = extraShinyPoints.contains(point) ? 100 : 50;
                colors[point.index] = LXColor.hsba(
                        LXColor.h(baseColor),
                        LXColor.s(baseColor) * status,
                        brightness * status,
                        100
                );
            }
        }

        for (int i = 0; i < pointsPerMilli * deltaMs; i++) {
            startBreathing(availablePoints.get(random.nextInt(availablePoints.size())));
        }
    }

    private void startBreathing(LXPoint point) {
        LXRangeModulator phase = new TriangleLFO(0, 1, DURATION_MILLIS);
        phase.setLooping(false);
        startModulator(phase);
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
            removeModulator(phase);
            return null;
        }
        double curValue = phase.getValue();
        return curValue > .5 ? 2 * (1 - curValue) : 2 * curValue;
    }

}
