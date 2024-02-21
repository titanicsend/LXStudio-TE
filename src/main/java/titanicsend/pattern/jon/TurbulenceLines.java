package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Noise")
public class TurbulenceLines extends DriftEnabledPattern {


  public TurbulenceLines(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // common controls setup
    controls.setRange(TEControlTag.SPEED, 0, -4, 4);
    controls.setValue(TEControlTag.SPEED, 0.5);

    controls.setRange(TEControlTag.SIZE, 1, 0.4, 1.25);
    controls.setRange(TEControlTag.QUANTITY, 60, 20, 200);
    controls.setRange(TEControlTag.WOW1, 0.5, 0, 1);

    controls.setValue(TEControlTag.XPOS, 0.25);
    controls.setValue(TEControlTag.YPOS, -0.25);

    // register common controls with LX
    addCommonControls();

    addShader(
        "turbulent_noise_lines.fs",
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            // calculate incremental transform based on elapsed time
            s.setUniform("iTranslate", (float) getXPosition(), (float) getYPosition());
          }
        });
  }
}
