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
public class KaleidoscopeShaderEffect extends GLShaderEffect {

  // Number of mirrored wedges (>= 1)
  public final BoundedParameter segments =
      new BoundedParameter("Segments", 6, 1, 64).setDescription("Number of kaleidoscope segments");

  // Global rotation of the kaleidoscope (radians)
  public final CompoundParameter angle =
      new CompoundParameter("Angle", 0.0)
          .setUnits(Units.RADIANS)
          .setDescription("Rotate the kaleidoscope wedge layout");

  // Additional rotation of the sampled image (radians)
  public final CompoundParameter rotate =
      new CompoundParameter("Rotate", 0.0)
          .setUnits(Units.RADIANS)
          .setDescription("Rotate sampled texture within each wedge");

  // Zoom factor on radial distance (1.0 = original)
  public final CompoundParameter zoom =
      new CompoundParameter("Zoom", 1.0, 0.25, 4.0).setDescription("Zoom of the texture sample");

  // Kaleidoscope center in normalized coords [0..1]
  public final BoundedParameter x =
      new BoundedParameter("X", 0.5, 0.0, 1.0).setDescription("Kaleidoscope center X (normalized)");

  public final BoundedParameter y =
      new BoundedParameter("Y", 0.5, 0.0, 1.0).setDescription("Kaleidoscope center Y (normalized)");

  // Mirror reflection inside wedge (true) vs wrap (false)
  public final BooleanParameter mirror =
      new BooleanParameter("Mirror", true)
          .setDescription("Reflect within wedges (on) or wrap angle (off)");

  // Soft edge at frame borders (feather in pixels)
  public final BoundedParameter edgeFeather =
      new BoundedParameter("Feather", 0.0, 0.0, 20.0)
          .setDescription("Feather edges to black near frame bounds (pixels)");

  // Effect blend amount (0 = original, 1 = full kaleidoscope)
  public final CompoundParameter mix =
      new CompoundParameter("Mix", 1.0, 0.0, 1.0)
          .setDescription("Mix: 0 = original image, 1 = full kaleidoscope");

  public KaleidoscopeShaderEffect(LX lx) {
    super(lx);

    addParameter("segments", this.segments);
    addParameter("angle", this.angle);
    addParameter("rotate", this.rotate);
    addParameter("zoom", this.zoom);
    addParameter("x", this.x);
    addParameter("y", this.y);
    addParameter("mirror", this.mirror);
    addParameter("feather", this.edgeFeather);
    addParameter("mix", this.mix);

    this.segments.setValue(6);
    this.angle.setValue(0.0);
    this.rotate.setValue(0.0);
    this.zoom.setValue(1.0);
    this.x.setValue(0.5);
    this.y.setValue(0.5);
    this.mirror.setValue(true);
    this.edgeFeather.setValue(0.0);
    this.mix.setValue(1.0);

    addShader(
        GLShader.config(lx)
            .withFilename("shader_kaleidoscope.fs")
            .withUniformSource(this::setUniforms));
  }

  private void setUniforms(GLShader shader) {
    shader.setUniform("segments", Math.max(1, Math.round(this.segments.getValuef())));
    shader.setUniform("angle", this.angle.getValuef());
    shader.setUniform("rotate", this.rotate.getValuef());
    shader.setUniform("zoom", this.zoom.getValuef());
    shader.setUniform("center", this.x.getValuef(), this.y.getValuef());
    shader.setUniform("mirror", this.mirror.isOn() ? 1 : 0);
    shader.setUniform("featherPx", this.edgeFeather.getValuef());
    shader.setUniform("mixAmt", this.mix.getValuef());
  }

  @Override
  public void dispose() {
    super.dispose();
  }
}
