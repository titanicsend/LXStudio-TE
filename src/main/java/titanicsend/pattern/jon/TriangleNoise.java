package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

@LXCategory("Noise")
public class TriangleNoise extends DriftEnabledPattern {

  NativeShaderPatternEffect effect;
  NativeShader shader;

  public TriangleNoise(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // common controls setup
    controls.setRange(TEControlTag.SPEED, 0, -4, 4); // overall scale
    controls.setValue(TEControlTag.SPEED, 0.5);

    controls.setRange(TEControlTag.SIZE, 1.15, 2, 0.3);
    controls.setRange(TEControlTag.QUANTITY, 3, 1, 5);
    controls.setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);
    controls.setRange(TEControlTag.SPIN, 0, -0.5, 0.5);
    controls.setRange(TEControlTag.WOW1, 4.4, 3, 6.5);

    // register common controls with LX
    addCommonControls();

    effect = new NativeShaderPatternEffect("triangle_noise.fs", new PatternTarget(this));
  }

  @Override
  protected void runTEAudioPattern(double deltaMs) {

    // calculate incremental transform based on elapsed time
    shader.setUniform("iTranslate", (float) getXPosition(), (float) getYPosition());

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
