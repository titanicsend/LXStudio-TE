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
 * <p>controls: - SPEED: Crystallization/melting speed (0-2, default 1) - SIZE: Crystal scale and
 * detail level (0.1-3, default 1) - QUANTITY: Crystal density and count (1-20, default 10) - WOW1:
 * Glint intensity and refraction (0-2, default 1) - WOW2: Temperature/color temperature (0-1,
 * default 0.5)
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

    controls.setRange(TEControlTag.SPEED, 0, 2, 1);
    controls.setRange(TEControlTag.SIZE, 0.1, 3, 1);
    controls.setRange(TEControlTag.QUANTITY, 1, 30, 15);

    // configure glint intensity
    controls.setRange(TEControlTag.WOW1, 0, 2, 1);

    // configure temp (affects color: 0=cold blue, 1=warm white)
    controls.setRange(TEControlTag.WOW2, 0, 1, 0.5);

    // disable unused controls
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));

    addCommonControls();
    addShader("ice_glint.fs");
  }
}
