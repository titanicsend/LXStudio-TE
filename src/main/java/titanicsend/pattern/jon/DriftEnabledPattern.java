package titanicsend.pattern.jon;

import heronarts.lx.LX;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/**
 * Class for patterns that need controllable continuous, unbounded x/y movement. Overrides the
 * default XOffset/YOffset control behavior so the controls set a direction vector, and the position
 * is smoothly changed. The maximum movment rate is based on the real-time clock, independent of the
 * speed control.
 *
 * <p>Note that patterns must be "drift aware" - they must know that the offset controls now set a
 * direction and speed, and that the current position is available via the getXPosition() and
 * getYPosition() functions.
 *
 * <p>GLSL shaders using this class should define #TE_NOTRANSLATE in their code to disable the
 * default control behavior in the shader engine.
 */
public abstract class DriftEnabledPattern extends TEPerformancePattern {
  private double xOffset = 0;
  private double yOffset = 0;

  protected DriftEnabledPattern(LX lx, TEShaderView defaultView) {
    super(lx, defaultView);
  }

  private void updateTranslation(double deltaMs) {
    // calculate change in position since last frame.
    xOffset += getXPos() * deltaMs / 1000.;
    yOffset += getYPos() * deltaMs / 1000.;
  }

  protected double getXPosition() {
    return xOffset;
  }

  protected double getYPosition() {
    return yOffset;
  }

  protected void resetPosition() {
    xOffset = 0;
    yOffset = 0;
  }

  @Override
  protected void run(double deltaMs) {
    updateTranslation(deltaMs);
    super.run(deltaMs);
  }
}
