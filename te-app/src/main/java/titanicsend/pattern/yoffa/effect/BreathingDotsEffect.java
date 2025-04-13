package titanicsend.pattern.yoffa.effect;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import java.util.*;
import java.util.List;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;

public class BreathingDotsEffect extends PatternEffect {

  private final Random random = new Random();

  private static final int DURATION_MILLIS = 2000;
  private static final double MAX_POINTS_DIVIDER = 51;
  private final Map<LXPoint, Long> breathingPoints = new HashMap<>();
  private final Set<LXPoint> extraShinyPoints = new HashSet<>();

  public BreathingDotsEffect(PatternTarget patternTarget) {

    super(patternTarget);
    // restrict time to forward only - it simplifies breathing calculations.
    pattern.allowBidirectionalTime(false);
  }

  public void run(double deltaMs) {
    int baseColor = pattern.calcColor();
    double et = pattern.getDeltaMs();

    double maxPoints = getPoints().size() / (MAX_POINTS_DIVIDER - 50 * pattern.getQuantity());
    double pointsPerMilli = maxPoints / DURATION_MILLIS;

    List<LXPoint> availablePoints = new ArrayList<>();
    for (LXPoint point : getPoints()) {
      Double status = getBreathStatus(point);
      if (status == null) {
        setColor(point, LXColor.BLACK);
        availablePoints.add(point);
      } else {
        double brightness = extraShinyPoints.contains(point) ? 100 : 50;
        setColor(
            point,
            LXColor.hsba(
                LXColor.h(baseColor),
                LXColor.s(baseColor) * status,
                brightness * status * pattern.getBrightness(),
                100));
      }
    }

    for (int i = 0; i < pointsPerMilli * et; i++) {
      if (availablePoints.size() > 0) {
        startBreathing(availablePoints.get(random.nextInt(availablePoints.size())));
      }
    }
  }

  @Override
  public Collection<LXParameter> getParameters() {
    return List.of();
  }

  private void startBreathing(LXPoint point) {
    breathingPoints.put(point, (long) pattern.getTimeMs());
    if (random.nextBoolean()) {
      extraShinyPoints.add(point);
    }
  }

  private Double getBreathStatus(LXPoint point) {
    long currentTimestamp = (long) pattern.getTimeMs();
    Long startTimestamp = breathingPoints.get(point);
    if (startTimestamp == null) {
      return null;
    }
    if (currentTimestamp - startTimestamp > DURATION_MILLIS) {
      breathingPoints.remove(point);
      extraShinyPoints.remove(point);
      return null;
    }

    double midpoint = startTimestamp + (double) DURATION_MILLIS / 2;
    double curValue = Math.abs(currentTimestamp - midpoint) / ((double) DURATION_MILLIS / 2);
    return curValue > .5 ? 2 * (1 - curValue) : 2 * curValue;
  }

  @Override
  public void onPatternActive() {
    breathingPoints.clear();
    extraShinyPoints.clear();
    pattern.retrigger(TEControlTag.SPEED);
  }
}
