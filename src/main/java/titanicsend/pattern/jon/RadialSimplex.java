package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.EnumParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

@LXCategory("Noise")
public class RadialSimplex extends TEPerformancePattern {

  NativeShaderPatternEffect effect;
  NativeShader shader;

  public enum NoiseMode {
    CLOUDS("Smoke Rings"),
    CIRCLES("Circles"),
    FOUNTAIN("Fountain");

    private final String label;

    private NoiseMode(String label) {
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

    // register common controls with LX
    addCommonControls();

    effect = new NativeShaderPatternEffect("radial_simplex.fs", new PatternTarget(this));
  }

  @Override
  protected void runTEAudioPattern(double deltaMs) {

    RadialSimplex.NoiseMode mode = noiseMode.getEnum();
    shader.setUniform("iWow1", (float) mode.ordinal());

    // run the shader
    effect.run(deltaMs);
  }

  @Override
  // THIS IS REQUIRED if you're not using ConstructedPattern!
  // Initialize the NativeShaderPatternEffect and retrieve the native shader object
  // from it when the pattern becomes active
  protected void onActive() {
    super.onActive();
    effect.onActive();
    shader = effect.getNativeShader();
  }
}
