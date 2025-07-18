package titanicsend.pattern.piemonte;

import static titanicsend.util.TEColor.TRANSPARENT;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.FunctionalParameter;
import heronarts.lx.parameter.LXParameter;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/**
 * Afterglow Pattern
 *
 * <p>An edge pattern that creates white dots pulsing out from joints, fading as they travel along
 * edges. Best viewed in deep playa.
 */
@LXCategory("Edge FG")
public class Afterglow extends TEPerformancePattern {

  protected final SawLFO phase =
      new SawLFO(
          0,
          1,
          new FunctionalParameter() {
            public double getValue() {
              return 3000 / getSpeed(); // rate
            }
          });

  // store random phase offset for each edge
  private Map<Integer, Float> edgePhaseOffsets = new HashMap<>();
  private Random random = new Random();

  /**
   * Constructor for Afterglow pattern
   *
   * @param lx The LX lighting engine instance
   */
  public Afterglow(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    startModulator(this.phase);

    controls.setRange(TEControlTag.SPEED, 1, 0, 1);

    // dot size
    controls
        .setRange(TEControlTag.SIZE, 5, 1, 20)
        .setUnits(TEControlTag.SIZE, LXParameter.Units.INTEGER);

    // fade distance
    controls.setRange(TEControlTag.QUANTITY, 0.5, 0, 1.0);

    // spawning dot spacing
    controls.setRange(TEControlTag.WOW1, 0.0, 0.0, 1.0);

    // pulse density (number of concurrent)
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
    float randomness = (float) getWow1();
    int numPulses = (int) getWow2();

    // clear all points first
    for (LXPoint point : model.points) {
      colors[point.index] = TRANSPARENT;
    }

    // white color for dots (full brightness)
    int whiteColor = LXColor.gray(100);

    // for each edge
    int edgeIndex = 0;
    for (TEEdgeModel edge : this.modelTE.getEdges()) {
      if (!edgePhaseOffsets.containsKey(edgeIndex)) {
        edgePhaseOffsets.put(edgeIndex, random.nextFloat());
      }

      float edgePhaseOffset = edgePhaseOffsets.get(edgeIndex) * randomness;

      // create pulses
      for (int pulseNum = 0; pulseNum < numPulses; pulseNum++) {
        float pulseOffset = (float) pulseNum / numPulses;
        float adjustedPhase = (phase + pulseOffset + edgePhaseOffset) % 1.0f;

        float travelDistance = adjustedPhase * fadeDistance;

        int dotPosition = (int) (edge.size * travelDistance);

        int dotPositionReverse = edge.size - dotPosition;

        int i = 0;
        for (LXPoint point : edge.points) {
          if (i >= dotPosition - dotSize / 2
              && i <= dotPosition + dotSize / 2
              && dotPosition < edge.size) {

            float fadeFactor = 1.0f - (travelDistance / fadeDistance);
            fadeFactor = Math.max(0, fadeFactor);

            if (Math.abs(i - dotPosition) == dotSize / 2 && dotSize > 1) {
              fadeFactor *= 0.5f;
            }

            int color = LXColor.scaleBrightness(whiteColor, fadeFactor);
            colors[point.index] = LXColor.add(colors[point.index], color);
          }

          // check if point is within dot from v1
          if (i >= dotPositionReverse - dotSize / 2
              && i <= dotPositionReverse + dotSize / 2
              && dotPositionReverse >= 0) {
            float fadeFactor = 1.0f - (travelDistance / fadeDistance);
            fadeFactor = Math.max(0, fadeFactor);

            if (Math.abs(i - dotPositionReverse) == dotSize / 2 && dotSize > 1) {
              fadeFactor *= 0.5f;
            }

            int color = LXColor.scaleBrightness(whiteColor, fadeFactor);
            colors[point.index] = LXColor.add(colors[point.index], color);
          }

          i++;
        }
      }
      edgeIndex++;
    }
  }
}
