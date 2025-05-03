package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.*;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderEffect;

@LXCategory("Titanics End")
public class BasicShaderEffect extends GLShaderEffect {

  public final CompoundParameter amount =
      new CompoundParameter("Amount", 0.50)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Amount of effect to apply");

  public BasicShaderEffect(LX lx) {
    super(lx);

    addParameter("amount", this.amount);

    // add the first shader, passing in the effect's backbuffer and aon OnFrame function
    // setting the amount of effect to apply as the uniform "iWow2".
    addShader("demo_simple_effect.fs", this::setUniforms, getImageBuffer());
  }

  private void setUniforms(GLShader s) {
    s.setUniform("iWow2", amount.getValuef());
  }
}
