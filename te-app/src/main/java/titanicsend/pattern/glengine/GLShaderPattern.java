package titanicsend.pattern.glengine;

import heronarts.lx.GpuDevice;
import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import titanicsend.color.TEColorParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.Uniform;

/**
 * Wrapper class for OpenGL shaders. Simplifies handling of context and native memory management,
 * and provides a convenient interface for adding shaders to a pattern.
 */
public class GLShaderPattern extends TEPerformancePattern implements GpuDevice {

  public static final int NO_TEXTURE = -1;

  // list of shaders to run
  private final List<TEShader> mutableShaders = new ArrayList<>();
  public final List<TEShader> shaders = Collections.unmodifiableList(this.mutableShaders);

  private boolean modelChanged = true;

  private static class TEUniforms {
    private Uniform.Float1 iTime;
    private Uniform.Float3 iColorRGB;
    private Uniform.Float3 iColorHSB;
    private Uniform.Float3 iColor2RGB;
    private Uniform.Float3 iColor2HSB;
    private Uniform.Float1 iPaletteOffset;
    private Uniform.Float1 iSpeed;
    private Uniform.Float1 iScale;
    private Uniform.Float1 iQuantity;
    private Uniform.Float2 iTranslate;
    private Uniform.Float1 iSpin;
    private Uniform.Float1 iRotationAngle;
    private Uniform.Float1 iBrightness;
    private Uniform.Float1 iWow1;
    private Uniform.Float1 iWow2;
    private Uniform.Boolean1 iWowTrigger;
    private Uniform.Float1 levelReact;
    private Uniform.Float1 frequencyReact;
  }

  private final TEUniforms uniforms = new TEUniforms();
  private boolean initializedUniforms = false;

  public GLShaderPattern(LX lx) {
    this(lx, TEShaderView.ALL_POINTS);
  }

  public GLShaderPattern(LX lx, TEShaderView view) {
    super(lx, view);
  }

  protected TEShader addShader(GLShader.Config config) {
    TEShader shader = new TEShader(config.withUniformSource(this::setUniforms));
    this.mutableShaders.add(shader);
    return shader;
  }

  protected TEShader addShader(String shaderFilename) {
    return addShader(GLShader.config(lx).withFilename(shaderFilename));
  }

  @Override
  protected void onModelChanged(LXModel model) {
    super.onModelChanged(model);
    this.modelChanged = true;
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    // Safety check: bail if the pattern contains no shaders
    if (this.shaders.isEmpty()) {
      return;
    }

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
    for (TEShader shader : this.shaders) {
      // TODO: map output of each shader to the next shader's input
      shader.run();
    }
  }

  private void initializeUniforms(GLShader s) {
    // Keep direct references to each Uniform, saves hashmap lookup.
    this.uniforms.iTime = s.getUniformFloat1("iTime");
    this.uniforms.iColorRGB = s.getUniformFloat3("iColorRGB");
    this.uniforms.iColorHSB = s.getUniformFloat3("iColorHSB");
    this.uniforms.iColor2RGB = s.getUniformFloat3("iColor2RGB");
    this.uniforms.iColor2HSB = s.getUniformFloat3("iColor2HSB");
    this.uniforms.iPaletteOffset = s.getUniformFloat1("iPaletteOffset");
    this.uniforms.iSpeed = s.getUniformFloat1("iSpeed");
    this.uniforms.iScale = s.getUniformFloat1("iScale");
    this.uniforms.iQuantity = s.getUniformFloat1("iQuantity");
    this.uniforms.iTranslate = s.getUniformFloat2("iTranslate");
    this.uniforms.iSpin = s.getUniformFloat1("iSpin");
    this.uniforms.iRotationAngle = s.getUniformFloat1("iRotationAngle");
    this.uniforms.iBrightness = s.getUniformFloat1("iBrightness");
    this.uniforms.iWow1 = s.getUniformFloat1("iWow1");
    this.uniforms.iWow2 = s.getUniformFloat1("iWow2");
    this.uniforms.iWowTrigger = s.getUniformBoolean1("iWowTrigger");
    this.uniforms.levelReact = s.getUniformFloat1("levelReact");
    this.uniforms.frequencyReact = s.getUniformFloat1("frequencyReact");
  }

  private void setUniforms(GLShader s) {
    if (!this.initializedUniforms) {
      this.initializedUniforms = true;
      initializeUniforms(s);
    }

    // set standard shadertoy-style uniforms
    this.uniforms.iTime.setValue((float) getTime());

    // color-related uniforms
    int col = calcColor();
    this.uniforms.iColorRGB.setValue(
        (float) (0xff & LXColor.red(col)) / 255f,
        (float) (0xff & LXColor.green(col)) / 255f,
        (float) (0xff & LXColor.blue(col)) / 255f);
    this.uniforms.iColorHSB.setValue(
        LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);

    col = calcColor2();
    this.uniforms.iColor2RGB.setValue(
        (float) (0xff & LXColor.red(col)) / 255f,
        (float) (0xff & LXColor.green(col)) / 255f,
        (float) (0xff & LXColor.blue(col)) / 255f);
    this.uniforms.iColor2HSB.setValue(
        LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);

    boolean usePalette =
        getControls().color.colorSource.getEnum() != TEColorParameter.ColorSource.STATIC;
    this.uniforms.iPaletteOffset.setValue(usePalette ? getControls().color.getOffsetf() : -1f);

    // uniforms for common controls
    this.uniforms.iSpeed.setValue((float) getSpeed());
    this.uniforms.iScale.setValue((float) getSize());
    this.uniforms.iQuantity.setValue((float) getQuantity());
    this.uniforms.iTranslate.setValue((float) getXPos(), (float) getYPos());
    this.uniforms.iSpin.setValue((float) getSpin());
    this.uniforms.iRotationAngle.setValue((float) getRotationAngleFromSpin());
    this.uniforms.iBrightness.setValue((float) getBrightness());
    this.uniforms.iWow1.setValue((float) getWow1());
    this.uniforms.iWow2.setValue((float) getWow2());
    this.uniforms.iWowTrigger.setValue(getWowTrigger());
    this.uniforms.levelReact.setValue((float) getLevelReactivity());
    this.uniforms.frequencyReact.setValue((float) getFrequencyReactivity());
  }

  /**
   * Retrieve the render(output) texture handle for the pattern.
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

  @Override
  public void onActive() {
    super.onActive();
    for (TEShader shader : this.shaders) {
      shader.onActive();
    }
  }

  @Override
  public void onInactive() {
    for (TEShader shader : this.shaders) {
      shader.onInactive();
    }
    super.onInactive();
  }

  @Override
  public void dispose() {
    for (TEShader shader : this.shaders) {
      shader.dispose();
    }
    super.dispose();
  }
}
