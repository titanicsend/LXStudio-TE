package titanicsend.pattern.piemonte;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/**
 * Special Kube Pattern
 *
 * <p>dimensional cubes from tomorrow, today on a trip through space–best viewed in futuristic
 * dystopian dreamscapes or at 10 & F
 */
@LXCategory("PIEMONTE fx")
public class SpecialKube extends GLShaderPattern {

  /**
   * Constructor for SpecialKube pattern
   *
   * @param lx The LX lighting engine instance
   */
  public SpecialKube(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.setRange(TEControlTag.QUANTITY, 15, 5, 30);
    controls.setRange(TEControlTag.SPEED, 0.69, -4.0, 4.0);

    // cube size
    controls.setRange(TEControlTag.SIZE, 1.0, 0.5, 3.0);

    // glow intensity
    controls.setRange(TEControlTag.WOW1, 0.25, 0.0, 1.0);

    // special cube frequency (0 = none, 1 = all special)
    controls.setRange(TEControlTag.WOW2, 0.2, 0.0, 1.0);

    // rotation speed
    controls.setRange(TEControlTag.SPIN, 1.0, -4.0, 4.0);

    // rotation angle offset
    // controls.setRange(TEControlTag.ANGLE, 0.0, 0.0, 6.28);

    controls.setRange(TEControlTag.YPOS, -0.28, -1.0, 1.0);

    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    addCommonControls();

    addShader("specialkube.fs");
  }
}
