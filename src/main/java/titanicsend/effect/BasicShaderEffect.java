package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.Tempo;
import heronarts.lx.modulator.LXWaveshape;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.*;
import heronarts.lx.utils.LXUtils;
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
    GLShader shader =
        new GLShader(
            lx, "demo_simple_effect.fs", controlData, getDefaultImageBuffer());
    addShader(
        shader,
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader shader) {
            shader.setUniform("iWow2", amount.getValuef());
          }
        });
  }
}
