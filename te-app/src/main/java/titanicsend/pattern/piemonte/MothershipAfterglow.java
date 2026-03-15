package titanicsend.pattern.piemonte;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/**
 * Mothership Afterglow Pattern
 *
 * <p>Adapted from Afterglow for the Mothership geometry. Creates white dots pulsing outward from
 * window corners along window sides. Best viewed in deep playa.
 */
@LXCategory("Mothership")
public class MothershipAfterglow extends TEPerformancePattern {

  private double phase = 0;
  private Map<Integer, Float> sidePhaseOffsets = new HashMap<>();
  private Random random = new Random();

  public MothershipAfterglow(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.setRange(TEControlTag.SPEED, 0.5, -1, 1);

    controls
        .setRange(TEControlTag.SIZE, 5, 1, 20)
        .setUnits(TEControlTag.SIZE, LXParameter.Units.INTEGER);

    controls
        .setRange(TEControlTag.QUANTITY, 1, 1, 10)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

    // randomness - default to full randomization so sides fire at different times
    controls.setRange(TEControlTag.WOW1, 1.0, 0.0, 1.0);

    controls.setRange(TEControlTag.WOW2, 0.5, 0, 1.0);

    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    addCommonControls();
  }

  @Override
  protected void runTEAudioPattern(double deltaMs) {
    // Accumulate phase manually so negative speed reverses direction
    phase += (deltaMs / 3000.0) * getSpeed();
    phase = phase - Math.floor(phase); // wrap to 0..1
    int dotSize = (int) getSize();
    int numPulses = (int) getQuantity();
    float fadeDistance = (float) getWow2();
    float randomness = (float) getWow1();

    int baseColor = calcColor();

    int sideIndex = 0;
    for (LXModel window : getModel().sub("window")) {
      for (LXModel side : window.sub("s")) {
        if (!sidePhaseOffsets.containsKey(sideIndex)) {
          sidePhaseOffsets.put(sideIndex, random.nextFloat());
        }

        float sidePhaseOffset = sidePhaseOffsets.get(sideIndex) * randomness;

        for (int pulseNum = 0; pulseNum < numPulses; pulseNum++) {
          float pulseOffset = (float) pulseNum / numPulses;
          float adjustedPhase = (float) ((phase + pulseOffset + sidePhaseOffset) % 1.0);

          float travelDistance = adjustedPhase * fadeDistance;

          int dotPosition = (int) (side.size * travelDistance);
          int dotPositionReverse = side.size - dotPosition;

          int i = 0;
          for (LXPoint point : side.points) {
            if (i >= dotPosition - dotSize / 2
                && i <= dotPosition + dotSize / 2
                && dotPosition < side.size) {

              float fadeFactor = 1.0f - (travelDistance / fadeDistance);
              fadeFactor = Math.max(0, fadeFactor);

              if (Math.abs(i - dotPosition) == dotSize / 2 && dotSize > 1) {
                fadeFactor *= 0.5f;
              }

              int color = LXColor.scaleBrightness(baseColor, fadeFactor);
              colors[point.index] = LXColor.add(colors[point.index], color);
            }

            if (i >= dotPositionReverse - dotSize / 2
                && i <= dotPositionReverse + dotSize / 2
                && dotPositionReverse >= 0) {
              float fadeFactor = 1.0f - (travelDistance / fadeDistance);
              fadeFactor = Math.max(0, fadeFactor);

              if (Math.abs(i - dotPositionReverse) == dotSize / 2 && dotSize > 1) {
                fadeFactor *= 0.5f;
              }

              int color = LXColor.scaleBrightness(baseColor, fadeFactor);
              colors[point.index] = LXColor.add(colors[point.index], color);
            }

            i++;
          }
        }
        sideIndex++;
      }
    }
  }
}
