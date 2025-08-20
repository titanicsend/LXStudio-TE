package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Native Shaders Panels")
public class Electric extends GLShaderPattern {

  public Electric(LX lx) {
    super(lx, TEShaderView.DOUBLE_LARGE);

    controls.setRange(TEControlTag.SPEED, 0.6, -1, 1);
    controls.setRange(TEControlTag.WOW1, 0, 0, 2.6);
    controls.setRange(TEControlTag.QUANTITY, 0.2, 0.075, 0.3);
    controls.setValue(TEControlTag.SPIN, 0.125);

    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    // register common controls with LX
    addCommonControls();

    addShader("electric.fs");
  }
}
