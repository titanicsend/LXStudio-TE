package titanicsend.pattern.piemonte;

import static titanicsend.util.TEColor.TRANSPARENT;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.FunctionalParameter;
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

  protected final SawLFO phase =
      new SawLFO(
          0,
          1,
          new FunctionalParameter() {
            public double getValue() {
              return 3000 / getSpeed();
            }
          });

  private Map<Integer, Float> sidePhaseOffsets = new HashMap<>();
  private Random random = new Random();

  public MothershipAfterglow(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    startModulator(this.phase);

    controls.setRange(TEControlTag.SPEED, 1, 0, 1);

    controls
        .setRange(TEControlTag.SIZE, 5, 1, 20)
        .setUnits(TEControlTag.SIZE, LXParameter.Units.INTEGER);

    controls.setRange(TEControlTag.QUANTITY, 0.5, 0, 1.0);

    controls.setRange(TEControlTag.WOW1, 0.0, 0.5, 1.0);

    controls.setRange(TEControlTag.WOW2, 1.0, 1.0, 10.0);

    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    addCommonControls();
  }

  @Override
  protected void runTEAudioPattern(double deltaMs) {
    float phase = this.phase.getValuef();
    int dotSize = (int) getSize();
    float fadeDistance = (float) getQuantity();
    int numPulses = (int) getWow2();
    float randomness = (float) getWow1();

    for (LXPoint point : model.points) {
      colors[point.index] = TRANSPARENT;
    }

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
          float adjustedPhase = (phase + pulseOffset + sidePhaseOffset) % 1.0f;

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
