package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Other")
public class Iceflow extends GLShaderPattern {
  public Iceflow(LX lx) {
    super(lx, TEShaderView.ALL_PANELS);

    controls.setRange(TEControlTag.QUANTITY, 5, 4, 8);
    controls.setRange(TEControlTag.WOW1, 0.225, 0, 1);

    // register common controls with the UI
    addCommonControls();

    addShader("iceflow.fs");
  }
}
