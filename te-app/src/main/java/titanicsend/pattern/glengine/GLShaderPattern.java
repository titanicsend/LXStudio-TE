package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import titanicsend.color.TEColorParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/**
 * Wrapper class for OpenGL shaders. Simplifies handling of context and native memory management,
 * and provides a convenient interface for adding shaders to a pattern.
 */
public class GLShaderPattern extends TEPerformancePattern {

  // TODO - mappedBuffer functionality not yet supported for shader patterns
  protected ByteBuffer mappedBuffer;
  protected final int mappedBufferWidth = GLEngine.getMappedBufferWidth();
  protected final int mappedBufferHeight = GLEngine.getMappedBufferHeight();

  // list of shaders to run
  private final List<GLShader> mutableShaders = new ArrayList<>();
  protected final List<GLShader> shaders = Collections.unmodifiableList(this.mutableShaders);

  public GLShaderPattern(LX lx) {
    this(lx, TEShaderView.ALL_POINTS);
  }

  public GLShaderPattern(LX lx, TEShaderView view) {
    super(lx, view);
  }

  private GLShader addShader(GLShader shader) {
    this.mutableShaders.add(shader);
    return shader;
  }

  /**
   * Add a shader by fragment shader filename. The simple option for shaders that use only the
   * default TEPerformancePattern uniforms and don't require any additional computation in Java.
   */
  protected GLShader addShader(String shaderName, String... textureFilenames) {
    return addShader(new GLShader(lx, shaderName, this::setUniforms, textureFilenames));
  }

  /**
   * Add a shader by fragment shader filename. The simple option for shaders that use only the
   * default TEPerformancePattern uniforms and don't require any additional computation in Java.
   */
  protected GLShader addShader(
      String shaderName, GLShader.UniformSource uniformSource, String... textureFilenames) {
    return addShader(
        new GLShader(lx, shaderName, List.of(this::setUniforms, uniformSource), textureFilenames));
  }

  /** Add a shader by fragment shader filename, with a callback for setting custom uniforms. */
  protected GLShader addShader(String shaderName, GLShader.UniformSource uniformSource) {
    return addShader(new GLShader(lx, shaderName, List.of(this::setUniforms, uniformSource)));
  }

  /** Add a shader by fragment shader filename, with a callback for setting custom uniforms. */
  protected GLShader addShader(
      String shaderName, GLShader.UniformSource uniformSource, ByteBuffer frameBuf) {
    return addShader(
        new GLShader(lx, shaderName, List.of(this::setUniforms, uniformSource), frameBuf));
  }

  /** Add a shader by fragment shader filename */
  protected GLShader addShader(String shaderName, ByteBuffer frameBuf) {
    return addShader(new GLShader(lx, shaderName, this::setUniforms, frameBuf));
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    LXModel m = getModel();
    GLShader shader = null;
    int n = this.shaders.size();

    // run the chain of shaders, except for the last one,
    // copying the output of each to the next shader's input texture
    for (int i = 0; i < n; i++) {
      shader = this.shaders.get(i);
      shader.useViewCoordinates(m);
      shader.useProgram();
      shader.run();
    }

    // paint the final shader output to the car
    if (shader != null) {
      ShaderPainter.mapToPointsDirect(m.points, shader.getBackBuffer(), getColors());
    }
  }

  private void setUniforms(GLShader s) {
    // set standard shadertoy-style uniforms
    s.setUniform("iTime", (float) getTime());

    // color-related uniforms
    int col = calcColor();
    s.setUniform(
        "iColorRGB",
        (float) (0xff & LXColor.red(col)) / 255f,
        (float) (0xff & LXColor.green(col)) / 255f,
        (float) (0xff & LXColor.blue(col)) / 255f);
    s.setUniform("iColorHSB", LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);

    col = calcColor2();
    s.setUniform(
        "iColor2RGB",
        (float) (0xff & LXColor.red(col)) / 255f,
        (float) (0xff & LXColor.green(col)) / 255f,
        (float) (0xff & LXColor.blue(col)) / 255f);
    s.setUniform("iColor2HSB", LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);

    boolean usePalette =
        getControls().color.colorSource.getEnum() != TEColorParameter.ColorSource.STATIC;
    s.setUniform("iPaletteOffset", usePalette ? getControls().color.getOffsetf() : -1f);

    // uniforms for common controls
    s.setUniform("iSpeed", (float) getSpeed());
    s.setUniform("iScale", (float) getSize());
    s.setUniform("iQuantity", (float) getQuantity());
    s.setUniform("iTranslate", (float) getXPos(), (float) getYPos());
    s.setUniform("iSpin", (float) getSpin());
    s.setUniform("iRotationAngle", (float) getRotationAngleFromSpin());
    s.setUniform("iBrightness", (float) getBrightness());
    s.setUniform("iWow1", (float) getWow1());
    s.setUniform("iWow2", (float) getWow2());
    s.setUniform("iWowTrigger", getWowTrigger());
    s.setUniform("levelReact", (float) getLevelReactivity());
    s.setUniform("frequencyReact", (float) getFrequencyReactivity());
  }

  @Override
  public void onActive() {
    // fix exception on slow startup
    if (this.colors == null) return;
    super.onActive();
    for (GLShader shader : this.shaders) {
      shader.onActive();
    }
  }

  @Override
  public void onInactive() {
    // fix exception on slow startup
    // TODO(jkb): remove this null check?
    if (this.colors == null) return;
    for (GLShader shader : this.shaders) {
      shader.onInactive();
    }
    super.onInactive();
  }

  @Override
  public void dispose() {
    for (GLShader shader : this.shaders) {
      shader.dispose();
    }
    super.dispose();
  }
}
