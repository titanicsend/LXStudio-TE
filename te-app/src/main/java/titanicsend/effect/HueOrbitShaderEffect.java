package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter.Units;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderEffect;

@LXCategory("Titanics End")
public class HueOrbitShaderEffect extends GLShaderEffect {

  public final CompoundParameter angle =
      new CompoundParameter("Angle", 0.0)
          .setUnits(Units.RADIANS)
          .setDescription("Hue rotation angle (radians)");

  public final CompoundParameter depth =
      new CompoundParameter("Depth", 1.0, 0.0, 1.0)
          .setDescription("Blend amount of the hue rotation");

  public final BooleanParameter preserveLightness =
      new BooleanParameter("Keep L", true)
          .setDescription("Preserve HSL lightness value");

  public final BoundedParameter satGain =
      new BoundedParameter("Sat Gain", 1.0, 0.0, 2.0)
          .setDescription("Scale saturation after rotation");

  public HueOrbitShaderEffect(LX lx) {
    super(lx);

    addParameter("angle", this.angle);
    addParameter("depth", this.depth);
    addParameter("keepL", this.preserveLightness);
    addParameter("satGain", this.satGain);

    this.depth.setValue(1.0);
    this.preserveLightness.setValue(true);
    this.satGain.setValue(1.0);

    addShader(
        GLShader.config(lx)
            .withFilename("shader_hue_orbit.fs")
            .withUniformSource(this::setUniforms));
  }

  private void setUniforms(GLShader shader) {
    shader.setUniform("angle", this.angle.getValuef());
    shader.setUniform("mixAmt", this.depth.getValuef());
    shader.setUniform("keepL", this.preserveLightness.isOn() ? 1 : 0);
    shader.setUniform("satGain", this.satGain.getValuef());
  }

  @Override
  public void dispose() {
    super.dispose();
  }
}
