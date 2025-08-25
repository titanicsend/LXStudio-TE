package titanicsend.pattern.piemonte;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.FunctionalParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/**
 * EdgeGlitch Pattern
 *
 * <p>WARNING: Flashing imagery. Color-inverting edge effect that reacts to bass hits.
 */
@LXCategory("Edge FG")
public class EdgeGlitch extends TEPerformancePattern {

  // Modulator for animation phase
  protected final SawLFO phase =
      new SawLFO(
          0,
          1,
          new FunctionalParameter() {
            public double getValue() {
              return 2000 / getSpeed(); // Animation speed
            }
          });

  // Glitch timing
  private double glitchTimer = 0;
  private boolean isGlitchFrame = false;
  private int frameCounter = 0;

  /**
   * Constructor for EdgeGlitch pattern
   *
   * @param lx The LX lighting engine instance
   */
  public EdgeGlitch(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    startModulator(this.phase);

    controls.setRange(TEControlTag.SPEED, 1.0, 0, 1);
    controls.setRange(TEControlTag.WOW1, 0.5, 0, 1);
    controls.setRange(TEControlTag.WOW2, 0.5, 0, 1);
    controls.setRange(TEControlTag.SIZE, 1.0, 0.1, 2.0);

    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));
    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));

    addCommonControls();
  }

  @Override
  protected void runTEAudioPattern(double deltaMs) {
    glitchTimer += deltaMs;
    frameCounter++;

    // fast frame switching for glitch effect
    double frameRate = 35.0;
    double frameDuration = 1000.0 / frameRate;
    isGlitchFrame = ((int) (glitchTimer / frameDuration) % 4) < 2;

    float phase = this.phase.getValuef();
    float glitchIntensity = (float) getWow1();
    float colorShift = (float) getWow2();
    float brightness = (float) getSize();

    int baseColor = calcColor();
    int altColor = getGradientColor(phase);

    // apply glitch color inversion
    if (isGlitchFrame && glitchIntensity > 0) {
      int r = 255 - LXColor.red(baseColor);
      int g = 255 - LXColor.green(baseColor);
      int b = 255 - LXColor.blue(baseColor);
      baseColor =
          LXColor.rgb(
              (int)
                  (LXColor.red(baseColor) * (1 - glitchIntensity * 0.7)
                      + r * glitchIntensity * 0.7),
              (int)
                  (LXColor.green(baseColor) * (1 - glitchIntensity * 0.7)
                      + g * glitchIntensity * 0.7),
              (int)
                  (LXColor.blue(baseColor) * (1 - glitchIntensity * 0.7)
                      + b * glitchIntensity * 0.7));
    }

    int fillColor = LXColor.lerp(baseColor, altColor, colorShift);
    fillColor = LXColor.scaleBrightness(fillColor, brightness);

    for (LXPoint point : model.points) {
      int pointColor = fillColor;

      if (isGlitchFrame && glitchIntensity > 0 && Math.random() < glitchIntensity * 0.3) {
        float glitchBrightness = (float) (0.5 + Math.random() * 0.5);
        pointColor = LXColor.scaleBrightness(pointColor, glitchBrightness);
      }

      colors[point.index] = pointColor;
    }

    // apply beat effect
    if (getBassLevel() > 0.7 && glitchIntensity > 0) {
      // extra glitch on bass hits - random pixel brightening
      for (LXPoint point : model.points) {
        if (Math.random() < 0.1) {
          colors[point.index] = LXColor.scaleBrightness(colors[point.index], 1.5f);
        }
      }
    }
  }
}
