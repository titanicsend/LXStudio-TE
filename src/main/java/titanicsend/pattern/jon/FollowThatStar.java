package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Combo FG")
public class FollowThatStar extends GLShaderPattern {

  public FollowThatStar(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls
        .setRange(TEControlTag.QUANTITY, 5, 1, 10)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

    controls.setRange(TEControlTag.SIZE, 1.75, 1.0, 5);
    controls.setRange(TEControlTag.WOW2, 1, 1, 4);

    // register common controls with LX
    addCommonControls();

    addShader(
        new GLShader(lx, "followthatstar.fs", this),
        new GLShaderFrameSetup() {
          @Override
          public void setUniforms(GLShader s) {
            s.setUniform("iQuantity", (float) getQuantity());
            s.setUniform("iSize", (float) getSize());
            s.setUniform("iWow2", (float) getWow2());
          }
        });
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    super.runTEAudioPattern(deltaMs);
  }
}
