package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Native Shaders Panels")
public class FourStar extends GLShaderPattern {

  public FourStar(LX lx) {
    super(lx, TEShaderView.ALL_PANELS);

    controls.setRange(TEControlTag.WOW1, 0.5, 0, 1);
    addCommonControls();
    addShader("fourstar.fs");
  }
}
