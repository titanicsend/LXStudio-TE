package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderEffect;

@LXCategory("Titanics End")
public class SustainEffect extends GLShaderEffect {

  public final CompoundParameter sustain =
      new CompoundParameter("Sustain", 0)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Amount of sustain to apply");

  public SustainEffect(LX lx) {
    super(lx);

    addParameter("sustain", this.sustain);

    addShader(
        GLShader.config(lx).withFilename("sustain_effect.fs").withUniformSource(this::setUniforms));
  }

  @Override
  protected LXListenableNormalizedParameter primaryParam() {
    return sustain;
  }

  @Override
  protected LXListenableNormalizedParameter secondaryParam() {
    return null;
  }

  @Override
  protected void trigger() {
    // TODO
  }

  private void setUniforms(GLShader s) {
    s.setUniform("iSustain", sustain.getValuef());
  }
}
