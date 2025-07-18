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
    controls.setValue(TEControlTag.SPEED, 1.0);

    controls.setRange(TEControlTag.SIZE, 1, 1.25, 0.4);
    controls.setRange(TEControlTag.QUANTITY, 60, 20, 200);
    controls.setRange(TEControlTag.WOW1, 0.5, 0.2, 1);

    controls.setValue(TEControlTag.XPOS, 0.25);
    controls.setValue(TEControlTag.YPOS, -0.25);

    // register common controls with LX
    addCommonControls();

    addShader(
        GLShader.config(lx)
            .withFilename("turbulent_noise_lines.fs")
            .withUniformSource(
                (s) -> {
                  // calculate incremental transform based on elapsed time
                  s.setUniform("iTranslate", (float) getXPosition(), (float) getYPosition());

                  // override iTime so we can speed up noise field progression while leaving the
                  // controls
                  // in a more reasonable range
                  s.setUniform("iTime", 2f * (float) getTime());
                }));
  }
}
