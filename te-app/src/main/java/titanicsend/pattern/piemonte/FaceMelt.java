package titanicsend.pattern.piemonte;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/**
 * Face Melt Pattern
 *
 * <p>a smiley face pattern with two-tone color glitch animation best viewed in deep playa.
 */
@LXCategory("PIEMONTE fx")
public class FaceMelt extends GLShaderPattern {

  /**
   * Constructor for FaceMelt pattern
   *
   * @param lx The LX lighting engine instance
   */
  public FaceMelt(LX lx) {
    super(lx, TEShaderView.DOUBLE_LARGE);

    controls.setRange(TEControlTag.SPEED, 0.75, 0, 1);
    controls.setRange(TEControlTag.SIZE, 1.5, 0.1, 3);
    controls.setRange(TEControlTag.QUANTITY, 15, 5, 40);

    // configure brightness pulsing
    controls.setRange(TEControlTag.WOW1, 1.5, 1, 2);

    // configure color intensity
    controls.setRange(TEControlTag.WOW2, 0.5, 0, 1);

    // disable unused controls
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));

    addCommonControls();
    addShader("face_melt.fs");
  }
}
