package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderEffect;

@LXCategory("Titanics End")
public class ShaderEdgeGlow extends GLShaderEffect {

  // Edge detect controls
  public final BoundedParameter edgeThreshold =
      new BoundedParameter("Threshold", 0.15, 0.0, 1.0)
          .setDescription("Edge magnitude threshold");

  public final CompoundParameter edgeGain =
      new CompoundParameter("Edge Gain", 3.0, 0.5, 10.0)
          .setDescription("Gain applied to Sobel magnitude");

  // Glow controls
  public final BoundedParameter glowRadiusPx =
      new BoundedParameter("Radius", 8.0, 0.0, 32.0)
          .setDescription("Approximate blur radius in pixels");

  public final CompoundParameter glowBlur =
      new CompoundParameter("Blur", 0.6, 0.0, 1.0)
          .setDescription("Softness of the glow blur (0 = hard, 1 = soft)");

  public final CompoundParameter glowStrength =
      new CompoundParameter("Strength", 0.8, 0.0, 2.0)
          .setDescription("Intensity of glow added back over the image");

  // Output mode and mix
  public final BooleanParameter edgeOnly =
      new BooleanParameter("Edge Only", false)
          .setDescription("Show outlines only (ignore source image)");

  public final CompoundParameter mix =
      new CompoundParameter("Mix", 1.0, 0.0, 1.0)
          .setDescription("Blend of the effect over source");

  public ShaderEdgeGlow(LX lx) {
    super(lx);

    addParameter("threshold", this.edgeThreshold);
    addParameter("edgeGain", this.edgeGain);
  addParameter("radius", this.glowRadiusPx);
  addParameter("blur", this.glowBlur);
    addParameter("strength", this.glowStrength);
    addParameter("edgeOnly", this.edgeOnly);
    addParameter("mix", this.mix);

    this.edgeThreshold.setValue(0.15);
    this.edgeGain.setValue(3.0);
    this.glowRadiusPx.setValue(8.0);
  this.glowBlur.setValue(0.6);
    this.glowStrength.setValue(0.8);
    this.edgeOnly.setValue(false);
    this.mix.setValue(1.0);

    addShader(
        GLShader.config(lx)
            .withFilename("shader_edge_glow.fs")
            .withUniformSource(this::setUniforms));
  }

  private void setUniforms(GLShader shader) {
    shader.setUniform("edgeThreshold", this.edgeThreshold.getValuef());
    shader.setUniform("edgeGain", this.edgeGain.getValuef());
    shader.setUniform("glowRadiusPx", this.glowRadiusPx.getValuef());
  shader.setUniform("blurAmt", this.glowBlur.getValuef());
    shader.setUniform("glowStrength", this.glowStrength.getValuef());
    shader.setUniform("edgeOnly", this.edgeOnly.isOn() ? 1 : 0);
    shader.setUniform("mixAmt", this.mix.getValuef());
  }

  @Override
  public void dispose() {
    super.dispose();
  }
}
