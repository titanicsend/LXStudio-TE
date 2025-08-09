package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.EnumParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Noise")
public class RadialSimplex extends GLShaderPattern {

  public enum NoiseMode {
    CLOUDS("Smoke Rings"),
    CIRCLES("Circles"),
    FOUNTAIN("Fountain");

    private final String label;

    NoiseMode(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public final EnumParameter<RadialSimplex.NoiseMode> noiseMode =
      new EnumParameter<RadialSimplex.NoiseMode>("Mode", RadialSimplex.NoiseMode.CLOUDS)
          .setDescription("Radial Noise Mode");

  public RadialSimplex(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // common controls setup
    controls.setRange(TEControlTag.SPEED, 0, -4, 4);
    controls.setValue(TEControlTag.SPEED, 0.25);

    controls.setRange(TEControlTag.SIZE, 5, 9, 2);
    controls.setRange(TEControlTag.QUANTITY, 1, 2.5, .6);

    // noiseMode (in Wow1 control position)
    controls.setControl(TEControlTag.WOW1, noiseMode);

    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));

    // register common controls with LX
    addCommonControls();

    addShader(
        GLShader.config(lx)
            .withFilename("radial_simplex.fs")
            .withUniformSource(
                (s) -> {
                  RadialSimplex.NoiseMode mode = noiseMode.getEnum();
                  s.setUniform("iWow1", (float) mode.ordinal());
                }));
  }
}
