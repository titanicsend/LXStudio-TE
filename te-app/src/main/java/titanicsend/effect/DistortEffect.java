package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.*;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderEffect;

@LXCategory("Titanics End")
public class DistortEffect extends GLShaderEffect {
  double effectDepth;

  public final CompoundParameter depth =
      new CompoundParameter("Depth", 0.0, 0.0, 10.0)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Depth of the effect");

  public final BoundedParameter size =
      new BoundedParameter("Size", 0, 0, 10).setDescription("Explosion block size");

  public final CompoundParameter speed =
      new CompoundParameter("Speed", 0.05)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setExponent(2)
          .setDescription("Speed of the effect");

  public DistortEffect(LX lx) {
    super(lx);

    addParameter("speed", this.speed); // TODO(look): is this affecting iTime?
    addParameter("depth", this.depth);
    addParameter("size", this.size);

    addShader(
        GLShader.config(lx).withFilename("distort_effect.fs").withUniformSource(this::setUniforms));
  }

  private void setUniforms(GLShader shader) {
    shader.setUniform("size", size.getValuef());
    shader.setUniform("depth", depth.getValuef());
  }

  @Override
  public void run(double deltaMs, double enabledAmount) {
    effectDepth = enabledAmount * depth.getValue();
    super.run(deltaMs, enabledAmount);
  }

  @Override
  public void dispose() {
    super.dispose();
  }
}
