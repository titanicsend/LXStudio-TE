package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderEffect;

@LXCategory("Titanics End")
public class PixelizationShaderEffect extends GLShaderEffect {

  public final CompoundParameter size =
      new CompoundParameter("Size", 0.0, 0.0, 50.0)
          .setDescription("Size of pixels (higher = more pixelated)");

  public final CompoundParameter separation =
      new CompoundParameter("Separation", 0.0, 0.0, 1.0)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Amount of RGB channel separation");

  public final CompoundParameter speed =
      new CompoundParameter("Speed", 1.0, 0.0, 5.0)
          .setDescription("Speed of RGB animation");

  public PixelizationShaderEffect(LX lx) {
    super(lx);

    addParameter("size", this.size);
    addParameter("separation", this.separation);
    addParameter("speed", this.speed);

    addShader(
        GLShader.config(lx)
            .withFilename("pixelization_effect.fs")
            .withUniformSource(this::setUniforms));
  }

  private void setUniforms(GLShader s) {
    s.setUniform("size", size.getValuef());
    s.setUniform("separation", separation.getValuef());
    s.setUniform("speed", speed.getValuef());
  }
}
