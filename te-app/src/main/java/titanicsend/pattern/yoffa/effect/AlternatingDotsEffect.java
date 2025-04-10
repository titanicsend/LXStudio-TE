package titanicsend.pattern.yoffa.effect;

import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import java.util.*;
import titanicsend.color.TEColorType;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;

@LXCategory("Panel FG")
public class AlternatingDotsEffect extends PatternEffect {

  // TODO make pattern respect the angle/spin common params when horizon is applied
  public static final double OUTRUN_HORIZON_Y = 0.6;
  private static final int MAX_POINTS_DIVIDER = 51;

  private List<LXPoint> points;

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
    refreshPoints();
    return this;
  }

  private void refreshPoints() {
    this.points = new ArrayList<>();
    for (LXPoint point : getPoints()) {
      if (point.yn >= minYPercent) {
        this.points.add(point);
      }
    }

    this.maxPoints = (int) (this.points.size() / (MAX_POINTS_DIVIDER - 50 * pattern.getQuantity()));
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
    int baseColor = pattern.getSwatchColor(TEColorType.PRIMARY);
    double basis = getTempo().basis();
    int beatCount = getTempo().beatCount();

    if (beatCount > lastBeat) {
      lastBeat = beatCount;
      breathingPointsPrev = breathingPointsNext;
      breathingPointsNext = new HashSet<>();
      Collections.shuffle(points);
      for (int i = 0; breathingPointsNext.size() < maxPoints && i < points.size(); i++) {
        LXPoint curPoint = points.get(i);
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
      brightness *= breathStatus;
      brightness *= pattern.getBrightness();
      setColor(point, LXColor.hsba(LXColor.h(baseColor), LXColor.s(baseColor), brightness, alpha));
    }
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    refreshPoints();
  }

  @Override
  public Collection<LXParameter> getParameters() {
    return List.of();
  }
}
