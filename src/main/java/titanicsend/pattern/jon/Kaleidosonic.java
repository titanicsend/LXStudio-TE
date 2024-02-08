package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Native Shaders Panels")
public class Kaleidosonic extends GLShaderPattern {

  public Kaleidosonic(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.setRange(TEControlTag.WOW1, 0.04, 0, 0.08); // bass response
    controls.setRange(TEControlTag.WOW2, 0.5, 0.2, 3); // overall audio level adjustment
    controls.setRange(TEControlTag.SIZE, 1., 5, 0.2); // scale
    controls.setRange(TEControlTag.QUANTITY, 7, 1, 13); // number of kaleidoscope slices
    controls.setValue(TEControlTag.SPIN, 0.125);

    // register common controls with LX
    addCommonControls();

    addShader(
        new GLShader(lx, "kaleidosonic.fs", controlData, "color_noise.png"),
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            s.setUniform("avgVolume", avgVolume.getValuef());
          }
        });
  }
}
