package titanicsend.pattern.piemonte;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/**
 * Ice Glint Pattern
 *
 * <p>a crystalline ice pattern for the deep playa... (features various optical properties that make
 * it sparkle, refract, and glint)
 *
 * <p>controls: - SPEED: crystallization/melting speed - SIZE: crystal scale and detail level -
 * QUANTITY: crystal density and count - WOW1: glint intensity and refraction - WOW2:
 * temperature/color temperature
 */
@LXCategory("piemonte shaders")
public class IceGlint extends GLShaderPattern {

  /**
   * Constructor for IceGlint pattern
   *
   * @param lx The LX lighting engine instance
   */
  public IceGlint(LX lx) {
    super(lx, TEShaderView.DOUBLE_LARGE);

    controls.setRange(TEControlTag.SPEED, 0.4, 0, 1);

    controls.setRange(TEControlTag.SIZE, 1, 1, 3);

    controls.setRange(TEControlTag.QUANTITY, 17, 10, 25);

    // configure glint intensity
    controls.setRange(TEControlTag.WOW1, 1.5, 0, 3);

    // configure temp
    controls.setRange(TEControlTag.WOW2, 0.5, 0, 1);

    // disable unused controls
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));

    addCommonControls();
    addShader("ice_glint.fs");
  }
}
