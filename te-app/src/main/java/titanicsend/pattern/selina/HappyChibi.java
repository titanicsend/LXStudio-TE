package titanicsend.pattern.selina;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/**
 * Happy Chibi Pattern
 *
 * <p>A bouncing kawaii-style smiley face with animated eye highlights, cheek blush, and a
 * tongue-out expression.
 */
@LXCategory("Selina FX")
public class HappyChibi extends GLShaderPattern {

  /**
   * Constructor for HappyChibi pattern
   *
   * @param lx The LX lighting engine instance
   */
  public HappyChibi(LX lx) {
    super(lx, TEShaderView.DOUBLE_LARGE);

    controls.setRange(TEControlTag.SPEED, 1.0, 0.01, 6.0);
    controls.setRange(TEControlTag.SIZE, 1.0, 0.5, 2.0);
    controls.setRange(TEControlTag.QUANTITY, 15, 1, 40);

    // beat reactivity for pulsing
    controls.setRange(TEControlTag.WOW1, 0.5, 0, 2);

    // color gradient mix amount
    controls.setRange(TEControlTag.WOW2, 0.3, 0, 1);

    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    addCommonControls();
    addShader("happy_chibi.fs");
  }
}
