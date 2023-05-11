package titanicsend.pattern.yoffa.effect;

import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import java.util.*;

@LXCategory("Panel FG")
public class AlternatingDotsEffect extends PatternEffect {

    private List<LXPoint> points;
    private static final int MAX_POINTS_DIVIDER = 25;
    private int maxPoints;

    private double minYPercent;
    private Set<LXPoint> breathingPointsPrev = new HashSet<>();
    private Set<LXPoint> breathingPointsNext = new HashSet<>();
    private final Set<LXPoint> extraShinyPoints = new HashSet<>();
    private int lastBeat = 0;

    public AlternatingDotsEffect(PatternTarget target) {
        super(target);
        this.minYPercent = 0;
        refreshPoints();
    }

    public AlternatingDotsEffect setHorizon(double minYPercent) {
        this.minYPercent = minYPercent;
        this.maxPoints = (int) (minYPercent * points.size() / MAX_POINTS_DIVIDER);
        return this;
    }

    // TODO: Also call this when LXPattern.modelChanged() to get the new view
    private void refreshPoints() {
        this.points = new ArrayList<LXPoint>(getPoints());
        this.maxPoints = points.size() / MAX_POINTS_DIVIDER;
    }

    @Override
    public void onPatternActive() {
        refreshPoints();
        breathingPointsPrev.clear();
        breathingPointsNext.clear();
        extraShinyPoints.clear();
        lastBeat = 0;
    }

    public void run(double deltaMs) {
        int baseColor = pattern.getSwatchColor(TEPattern.ColorType.PRIMARY);
        double basis = getTempo().basis();
        int beatCount = getTempo().beatCount();

        if (beatCount > lastBeat) {
            lastBeat = beatCount;
            breathingPointsPrev = breathingPointsNext;
            breathingPointsNext = new HashSet<>();
            Collections.shuffle(points);
            for (int i = 0; breathingPointsNext.size() < maxPoints && i < points.size(); i++) {
                LXPoint curPoint = points.get(i);
                if (curPoint.yn < minYPercent) {
                    continue;
                }
                if (!breathingPointsPrev.contains(curPoint)) {
                    breathingPointsNext.add(curPoint);
                    if (extraShinyPoints.size() < maxPoints / 2) {
                        extraShinyPoints.add(curPoint);
                    }
                }
            }
        }

        double breathStatusNext = basis;
        double breathStatusPrev = 1 - basis;
        for (LXPoint point : points) {
            double alpha = 100;
            double breathStatus;
            if (breathingPointsPrev.contains(point)) {
                breathStatus = breathStatusPrev;
            } else if (breathingPointsNext.contains(point)) {
                breathStatus = breathStatusNext;
            } else {
                breathStatus = 0;
                alpha = 0;
            }
            double brightness = extraShinyPoints.contains(point) ? 100 : 50;
            setColor(point, LXColor.hsba(
                    LXColor.h(baseColor),
                    LXColor.s(baseColor),
                    brightness * breathStatus,
                    alpha
            ));
        }

    }

    @Override
    public Collection<LXParameter> getParameters() {
        return List.of();
    }
}
