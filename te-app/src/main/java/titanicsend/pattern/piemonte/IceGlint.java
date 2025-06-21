package titanicsend.pattern.piemonte;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/**
 * Ice Glint Pattern
 *
 * <p>A crystalline pattern that simulates the complex optical properties of ice and crystal
 * formations. Features include: - Sharp, angular geometric structures with realistic ice-like
 * faceting - Dynamic fracturing and crystallization effects - Multi-layer refraction and reflection
 * simulation - Shimmering and glinting effects that respond to virtual lighting - Temperature-based
 * color shifts from blue to white The pattern uses advanced noise functions and geometric
 * algorithms to create realistic ice crystal formations that grow, fracture, and catch light in
 * natural-looking ways. Particularly effective for winter-themed displays
 *
 * <p>or creating cold, crystalline atmospheres.
 *
 * <p>Controls: - SPEED: Crystallization/melting speed (0-2, default 1) - SIZE: Crystal scale and
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
