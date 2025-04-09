package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Noise")
public class SimplexPosterized extends DriftEnabledPattern {

  public SimplexPosterized(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // common controls setup
    controls.setRange(TEControlTag.SPEED, 0, -4, 4);
    controls.setValue(TEControlTag.SPEED, 0.5);

    controls.setRange(TEControlTag.SIZE, 5, 2, 9);
    controls.setRange(TEControlTag.QUANTITY, 1.5, 3, 0.5);

    // register common controls with LX
    addCommonControls();

    addShader(
        "simplex_posterized.fs",
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            // calculate incremental transform based on elapsed time
            s.setUniform("iTranslate", (float) getXPosition(), (float) getYPosition());
          }
        });
  }
}
