package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Look Shader Patterns")
public class TriangleInfinityLevels extends GLShaderPattern {

  public TriangleInfinityLevels(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);
    //        controls.setValue(TEControlTag.YPOS, -0.14);
    controls.setRange(TEControlTag.SIZE, 1.20, 0.00, 3.0);
    controls.setRange(TEControlTag.SPEED, 0.01, 0.00, 0.5);
    controls.setRange(TEControlTag.QUANTITY, 8.0, 2.0, 12.0);
    controls.setRange(TEControlTag.WOW1, 0.04, 0.0, 0.5);
    controls.setRange(TEControlTag.WOW2, 1.1, 1.0, 3.0);
    controls.setValue(TEControlTag.BRIGHTNESS, 0.5);

    addCommonControls();
    addShader("triangle_infinity.fs", (s) -> {
      s.setUniform("brightnessDampening", 0.5f);
    });
  }
}
