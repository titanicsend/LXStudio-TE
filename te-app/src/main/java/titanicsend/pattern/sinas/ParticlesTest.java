package titanicsend.pattern.sinas;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXModel;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Combo FG")
public class ParticlesTest extends GLShaderPattern {

  private GLShader shader;
  private float modelPointCount = 0f;

  public ParticlesTest(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // Controls
    controls
        .setRange(TEControlTag.QUANTITY, 10000.0, 0.0, 200000.0)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);
    addCommonControls();

    // Shader
    this.shader = addShader("particles.fs");
  }

  @Override
  protected void onModelChanged(LXModel model) {
    super.onModelChanged(model);
    this.modelPointCount =
        (model == null || model.points == null) ? 0f : (float) model.points.length;
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    float totalParticles = (float) getQuantity();
    float ppp = (this.modelPointCount > 0f) ? (totalParticles / this.modelPointCount) : 0f;

    if (this.shader != null) {
      this.shader.setUniform("iModelPointCount", this.modelPointCount);
      this.shader.setUniform("iParticlesPerPixel", ppp);
    }

    super.runTEAudioPattern(deltaMs);
  }
}
