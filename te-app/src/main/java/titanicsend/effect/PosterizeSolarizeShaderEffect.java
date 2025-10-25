package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderEffect;

@LXCategory("Titanics End")
public class PosterizeSolarizeShaderEffect extends GLShaderEffect {

  public final BoundedParameter levels =
      new BoundedParameter("Levels", 6, 2, 32)
          .setDescription("Number of tonal bands (per channel)");

  public final CompoundParameter mix =
      new CompoundParameter("Mix", 1.0, 0.0, 1.0)
          .setDescription("Blend of effect over source");

  public final BooleanParameter solarize =
      new BooleanParameter("Solarize", false)
          .setDescription("Enable solarization (invert above threshold)");

  public final BoundedParameter solThreshold =
      new BoundedParameter("Sol Thresh", 0.5, 0.0, 1.0)
          .setDescription("Solarize threshold on luma (0..1)");

  public final BooleanParameter preserveLuma =
      new BooleanParameter("Preserve Luma", true)
          .setDescription("Quantize in luma space to reduce banding artifacts");

  public PosterizeSolarizeShaderEffect(LX lx) {
    super(lx);

    addParameter("levels", this.levels);
    addParameter("mix", this.mix);
    addParameter("solarize", this.solarize);
    addParameter("solThreshold", this.solThreshold);
    addParameter("preserveLuma", this.preserveLuma);

    this.levels.setValue(6.0);
    this.mix.setValue(1.0);
    this.solarize.setValue(false);
    this.solThreshold.setValue(0.5);
    this.preserveLuma.setValue(true);

    addShader(
        GLShader.config(lx)
            .withFilename("shader_posterize_solarize.fs")
            .withUniformSource(this::setUniforms));
  }

  private void setUniforms(GLShader shader) {
    shader.setUniform("levels", Math.max(2, Math.round(this.levels.getValuef())));
    shader.setUniform("mixAmt", this.mix.getValuef());
    shader.setUniform("solarize", this.solarize.isOn() ? 1 : 0);
    shader.setUniform("solThreshold", this.solThreshold.getValuef());
    shader.setUniform("preserveLuma", this.preserveLuma.isOn() ? 1 : 0);
  }

  @Override
  public void dispose() {
    super.dispose();
  }
}
