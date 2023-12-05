package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.util.List;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

@LXCategory("Native Shaders")
public class TEAutoShaderPattern extends TEPerformancePattern {
  NativeShaderPatternEffect effect;
  NativeShader shader;

  protected TEAutoShaderPattern(LX lx) {
    this(lx, null);
  }

  protected TEAutoShaderPattern(LX lx, TEShaderView defaultView) {
    super(lx, defaultView);

    // create shader effect
    effect = new NativeShaderPatternEffect(this.getShaderFile(), new PatternTarget(this));

    // use common control configuration data from shader to set control defaults,
    // then register controls with Chromatik.
    List<ShaderConfiguration> shaderConfig = effect.getShaderConfig();
    configureCommonControls(shaderConfig);
    addCommonControls();
  }

  // Returns the shader file path. This is intended to be overridden when we create our
  // dynamic runtime class
  protected String getShaderFile() {
    return "";
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    // run the shader
    effect.run(deltaMs);
  }

  @Override
  // THIS IS REQUIRED if you're not using ConstructedPattern!
  // Initialize the NativeShaderPatternEffect and retrieve the native shader object
  // from it when the pattern becomes active
  public void onActive() {
    super.onActive();
    effect.onActive();
    shader = effect.getNativeShader();
  }

  @Override
  public void dispose() {
    // do nothing for now.
  }
}
