package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Look Shader Patterns")
public class TriangleInfinityWaveform extends GLShaderPattern {

  public TriangleInfinityWaveform(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    //        controls.setValue(TEControlTag.YPOS, -0.17);
    controls.setRange(TEControlTag.SIZE, 1.35, 0.2, 2.0);
    controls.setRange(TEControlTag.SPEED, 0.01, 0.00, 0.5);
    controls.setRange(TEControlTag.QUANTITY, 6.0, 2.0, 12.0);
    controls.setRange(TEControlTag.WOW1, 0.09, 0.0, 0.5);
    controls.setRange(TEControlTag.WOW2, 0.04, 0.0, 0.5);

    addCommonControls();

    addShader(
        "triangle_infinity.fs",
        new GLShaderPattern.GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            s.setUniform("brightnessDampening", 0.5f);
          }
        });
  }
}
