package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderEffect;

@LXCategory("Titanics End")
public class DistortEffect extends GLShaderEffect {
  double effectDepth;

  public final CompoundParameter speed =
      new CompoundParameter("Speed", 0.15)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setExponent(2)
          .setDescription("Speed of the effect");

  public final CompoundParameter depth =
      new CompoundParameter("Depth", 0.0, 0.0, 1.0)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Depth of the effect");

  public final BoundedParameter size =
      new BoundedParameter("Size", 0.15, 0.0, 2.0).setDescription("Distortion frequency");

  public DistortEffect(LX lx) {
    super(lx);

    addParameter("speed", this.speed);
    addParameter("depth", this.depth);
    addParameter("size", this.size);

    addShader(
        GLShader.config(lx).withFilename("distort_effect.fs").withUniformSource(this::setUniforms));
  }

  private void setUniforms(GLShader shader) {
    shader.setUniform("size", size.getValuef());
    shader.setUniform("depth", depth.getValuef());
    shader.setUniform("speed", speed.getValuef());
    // effects get iTime automatically.
    // shader.setUniform("iTime", (float) getTime());
  }

  @Override
  public void run(double deltaMs, double enabledAmount) {
    effectDepth = enabledAmount;
    super.run(deltaMs, enabledAmount);
  }
}
