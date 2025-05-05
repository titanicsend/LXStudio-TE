package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import titanicsend.color.TEColorType;
import titanicsend.effect.TEEffect;
import titanicsend.pattern.jon.VariableSpeedTimer;

/**
 * Wrapper class for OpenGL shaders. Simplifies handling of context and native memory management,
 * and provides a convenient interface for adding shaders to a pattern.
 */
public class GLShaderEffect extends TEEffect {

  private final VariableSpeedTimer iTime = new VariableSpeedTimer();
  protected ByteBuffer imageBuffer;
  protected ByteBuffer mappedBuffer;
  protected final int mappedBufferWidth = GLEngine.getMappedBufferWidth();
  protected final int mappedBufferHeight = GLEngine.getMappedBufferHeight();

  // list of shaders to run
  private final List<TEShader> mutableShaders = new ArrayList<>();
  protected final List<TEShader> shaders = Collections.unmodifiableList(this.mutableShaders);

  private boolean modelChanged = true;

  public GLShaderEffect(LX lx) {
    super(lx);

    imageBuffer = TEShader.allocateBackBuffer();
    mappedBuffer = TEShader.allocateMappedBuffer(mappedBufferWidth, mappedBufferHeight);
    // zero mappedBuffer
    mappedBuffer.rewind();
    for (int i = 0; i < mappedBuffer.capacity(); i++) {
      mappedBuffer.put(i, (byte) 0);
    }
  }

  private TEShader addShader(TEShader shader) {
    this.mutableShaders.add(shader);
    return shader;
  }

  /**
   * Add a shader by fragment shader filename. The simple option for shaders that use only the
   * default TEPerformancePattern uniforms and don't require any additional computation in Java.
   */
  protected TEShader addShader(String shaderName, String... textureFilenames) {
    return addShader(new TEShader(lx, shaderName, this::setUniforms, textureFilenames));
  }

  /**
   * Add a shader by fragment shader filename. The simple option for shaders that use only the
   * default TEPerformancePattern uniforms and don't require any additional computation in Java.
   */
  protected TEShader addShader(
      String shaderName, TEShader.UniformSource uniformSource, String... textureFilenames) {
    return addShader(
        new TEShader(lx, shaderName, List.of(this::setUniforms, uniformSource), textureFilenames));
  }

  /** Add a shader by fragment shader filename, with a callback for setting custom uniforms. */
  protected TEShader addShader(String shaderName, TEShader.UniformSource uniformSource) {
    return addShader(new TEShader(lx, shaderName, List.of(this::setUniforms, uniformSource)));
  }

  /** Add a shader by fragment shader filename, with a callback for setting custom uniforms. */
  protected TEShader addShader(
      String shaderName, TEShader.UniformSource uniformSource, ByteBuffer frameBuf) {
    return addShader(
        new TEShader(lx, shaderName, List.of(this::setUniforms, uniformSource), frameBuf));
  }

  /** Add a shader by fragment shader filename */
  protected TEShader addShader(String shaderName, ByteBuffer frameBuf) {
    return addShader(new TEShader(lx, shaderName, this::setUniforms, frameBuf));
  }

  protected ByteBuffer getImageBuffer() {
    return imageBuffer;
  }

  public double getTime() {
    return iTime.getTime();
  }

  @Override
  protected void onModelChanged(LXModel model) {
    super.onModelChanged(model);
    this.modelChanged = true;
  }

  protected void run(double deltaMs, double enabledAmount) {
    LXModel m = getModel();
    iTime.tick();

    // Update the model coords texture only when changed (and the first run)
    if (this.modelChanged) {
      this.modelChanged = false;
      for (TEShader shader : this.shaders) {
        shader.setModelCoordinates(m);
      }
    }

    // set up rectangular texture buffers for effects that need them
    ShaderPainter.mapToBufferDirect(m.points, imageBuffer, colors);
    ShaderPainter.mapFromLinearBuffer(
        m.points, mappedBufferWidth, mappedBufferHeight, mappedBuffer, colors);

    // run the chain of shaders, except for the last one,
    // copying the output of each to the next shader's input texture
    TEShader shader = null;
    int n = this.shaders.size();
    for (int i = 0; i < n; i++) {
      shader = this.shaders.get(i);
      shader.run();
    }

    // paint the final shader output to the car.
    if (shader != null) {
      ShaderPainter.mapToPointsDirect(m.points, imageBuffer, getColors());
    }
  }

  protected int getColor1() {
    return lx.engine.palette.getSwatchColor(TEColorType.PRIMARY.swatchIndex()).getColor();
  }

  protected int getColor2() {
    return lx.engine.palette.getSwatchColor(TEColorType.SECONDARY.swatchIndex()).getColor();
  }

  // send a subset of the controls we use with patterns
  private void setUniforms(GLShader s) {
    s.setUniform("iTime", (float) getTime());

    // get current primary and secondary colors
    // TODO - we're just grabbing swatch colors here.  Do we need to worry about modulation?
    int col = getColor1();
    s.setUniform(
        "iColorRGB",
        (float) (0xff & LXColor.red(col)) / 255f,
        (float) (0xff & LXColor.green(col)) / 255f,
        (float) (0xff & LXColor.blue(col)) / 255f);
    s.setUniform("iColorHSB", LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);

    col = getColor2();
    s.setUniform(
        "iColor2RGB",
        (float) (0xff & LXColor.red(col)) / 255f,
        (float) (0xff & LXColor.green(col)) / 255f,
        (float) (0xff & LXColor.blue(col)) / 255f);
    s.setUniform("iColor2HSB", LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);
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
