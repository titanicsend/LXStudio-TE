package titanicsend.pattern.glengine;

import static titanicsend.pattern.glengine.GLShaderPattern.NO_TEXTURE;

import heronarts.lx.GpuDevice;
import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import titanicsend.color.TEColorType;
import titanicsend.effect.TEPerformanceEffect;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.pattern.yoffa.shader_engine.Uniform;

/**
 * Wrapper class for OpenGL shaders. Simplifies handling of context and native memory management,
 * and provides a convenient interface for adding shaders to a pattern.
 */
public class GLShaderEffect extends TEPerformanceEffect implements GpuDevice {

  private final VariableSpeedTimer iTime = new VariableSpeedTimer();

  // list of shaders to run
  private final List<TEShader> mutableShaders = new ArrayList<>();
  protected final List<TEShader> shaders = Collections.unmodifiableList(this.mutableShaders);

  private boolean modelChanged = true;

  // Input texture handle that is the output from the pattern or preceding effect
  public int iDst = -1;
  // If there are multiple shaders in one effect, the iDst texture of each will be the output
  // of the previous shader
  private int currentShaderDst = -1;

  private static class TEEffectUniforms {
    private Uniform.Sampler2D iDst;
    private Uniform.Float1 level; // Is this universal for LXEffects?

    private Uniform.Float1 iTime;
    private Uniform.Float3 iColorRGB;
    private Uniform.Float3 iColorHSB;
    private Uniform.Float3 iColor2RGB;
    private Uniform.Float3 iColor2HSB;
  }

  private final TEEffectUniforms uniforms = new TEEffectUniforms();
  private boolean initializedUniforms = false;

  public GLShaderEffect(LX lx) {
    super(lx);
  }

  @Override
  protected LXListenableNormalizedParameter primaryParam() {
    throw new RuntimeException("Subclasses must override");
  }

  @Override
  protected LXListenableNormalizedParameter secondaryParam() {
    throw new RuntimeException("Subclasses must override");
  }

  @Override
  protected void trigger() {
    throw new RuntimeException("Subclasses must override");
  }

  protected TEShader addShader(GLShader.Config config) {
    TEShader shader = new TEShader(config.withUniformSource(this::setUniforms));
    this.mutableShaders.add(shader);
    return shader;
  }

  protected TEShader addShader(String shaderFilename) {
    return addShader(GLShader.config(lx).withFilename(shaderFilename));
  }

  public double getTime() {
    return iTime.getTime();
  }

  protected int getColor1() {
    return lx.engine.palette.getSwatchColor(TEColorType.PRIMARY.swatchIndex()).getColor();
  }

  protected int getColor2() {
    return lx.engine.palette.getSwatchColor(TEColorType.SECONDARY.swatchIndex()).getColor();
  }

  @Override
  protected void onModelChanged(LXModel model) {
    super.onModelChanged(model);
    this.modelChanged = true;
  }

  public void setDst(int iDst) {
    this.iDst = iDst;
  }

  protected void run(double deltaMs, double enabledAmount) {
    // Safety check: bail if the effect contains no shaders
    if (this.shaders.isEmpty()) {
      return;
    }

    iTime.tick();

    // Update the model coords texture only when changed (and the first run)
    if (this.modelChanged) {
      this.modelChanged = false;
      LXModel m = getModel();
      for (TEShader shader : this.shaders) {
        shader.setModelCoordinates(m);
      }
    }

    // Set the CPU buffer for any non-last shader to be null. These will be chained.
    for (int i = 0; i < (this.shaders.size() - 1); i++) {
      this.shaders.get(i).setCpuBuffer(null);
    }
    // Set the CPU buffer for the last shader, if using CPU mixer
    this.shaders.getLast().setCpuBuffer(this.lx.engine.renderMode.cpu ? this.colors : null);

    // Run the chain of shaders,
    // mapping the output texture of each to the next shader's input texture
    this.currentShaderDst = this.iDst;
    for (TEShader shader : this.shaders) {
      shader.run();
      this.currentShaderDst = shader.getRenderTexture();
    }
  }

  /**
   * Retrieve the render(output) texture handle for the effect.
   *
   * @return The output texture handle of the last shader, or NO_TEXTURE if no shaders exist
   */
  public int getRenderTexture() {
    if (!this.shaders.isEmpty()) {
      return this.shaders.getLast().getRenderTexture();
    } else {
      return NO_TEXTURE;
    }
  }

  private void initializeUniforms(GLShader s) {
    // Keep direct references to each Uniform, saves hashmap lookup.
    this.uniforms.iDst = s.getUniformSampler2D("iDst");
    this.uniforms.iTime = s.getUniformFloat1("iTime");
    this.uniforms.iColorRGB = s.getUniformFloat3("iColorRGB");
    this.uniforms.iColorHSB = s.getUniformFloat3("iColorHSB");
    this.uniforms.iColor2RGB = s.getUniformFloat3("iColor2RGB");
    this.uniforms.iColor2HSB = s.getUniformFloat3("iColor2HSB");
  }

  // send a subset of the controls we use with patterns
  private void setUniforms(GLShader s) {
    if (!this.initializedUniforms) {
      this.initializedUniforms = true;
      initializeUniforms(s);
    }

    // Shaders are run in sequence.  Pass the current iDst value, which may be the output of the
    // previous shader
    this.uniforms.iDst.setValue(this.currentShaderDst);

    this.uniforms.iTime.setValue((float) getTime());

    // get current primary and secondary colors
    // TODO - we're just grabbing swatch colors here.  Do we need to worry about modulation?
    int col = getColor1();
    this.uniforms.iColorRGB.setValue(
        (float) (0xff & LXColor.red(col)) / 255f,
        (float) (0xff & LXColor.green(col)) / 255f,
        (float) (0xff & LXColor.blue(col)) / 255f);
    this.uniforms.iColorHSB.setValue(
        LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);

    col = getColor2();
    this.uniforms.iColor2RGB.setValue(
        (float) (0xff & LXColor.red(col)) / 255f,
        (float) (0xff & LXColor.green(col)) / 255f,
        (float) (0xff & LXColor.blue(col)) / 255f);
    this.uniforms.iColor2HSB.setValue(
        LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);
  }

  @Override
  protected void onEnable() {
    super.onEnable();
    for (TEShader shader : this.shaders) {
      shader.onActive();
    }
  }

  @Override
  protected void onDisable() {
    for (TEShader shader : this.shaders) {
      shader.onInactive();
    }
    super.onDisable();
  }

  @Override
  public void dispose() {
    for (TEShader shader : this.shaders) {
      shader.dispose();
    }
    super.dispose();
  }
}
