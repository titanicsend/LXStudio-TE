package titanicsend.pattern.glengine;

import static com.jogamp.opengl.GL.GL_BGRA;
import static com.jogamp.opengl.GL.GL_LINEAR;
import static com.jogamp.opengl.GL.GL_RGBA8;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;
import static titanicsend.pattern.glengine.GLShaderPattern.NO_TEXTURE;

import com.jogamp.opengl.GL4;
import heronarts.lx.GpuDevice;
import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import titanicsend.color.TEColorType;
import titanicsend.effect.TEEffect;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.pattern.yoffa.shader_engine.Uniform;

/**
 * Wrapper class for OpenGL shaders. Simplifies handling of context and native memory management,
 * and provides a convenient interface for adding shaders to a pattern.
 */
public class GLShaderEffect extends TEEffect implements GpuDevice {

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
    private Uniform.Float1 iBrightness;

    private Uniform.Float1 iTime;
    private Uniform.Float3 iColorRGB;
    private Uniform.Float3 iColorHSB;
    private Uniform.Float3 iColor2RGB;
    private Uniform.Float3 iColor2HSB;
  }

  private final TEEffectUniforms uniforms = new TEEffectUniforms();
  private boolean initializedUniforms = false;

  // Temporary flag to avoid a LX bug
  private boolean onEnableCalled = false;

  public GLShaderEffect(LX lx) {
    super(lx);
  }

  protected TEShader addShader(GLShader.Config config) {
    TEShader shader = new TEShader(config.withUniformSource(this.uniformSource));
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

  private GL4 gl4;
  private boolean cpuTextureInitialized = false;
  private int cpuDstTexture = -1;
  private ByteBuffer cpuByteBuffer;

  private void initializeCpuTexture() {
    this.gl4 = GLEngine.current.getCanvas().getGL().getGL4();

    int width = GLEngine.current.getWidth();
    int height = GLEngine.current.getHeight();

    this.cpuByteBuffer =
        ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder());
    cpuByteBuffer.rewind();

    int[] cpuDstHandles = new int[1];
    this.gl4.glGenTextures(1, cpuDstHandles, 0);
    this.cpuDstTexture = cpuDstHandles[0];
    gl4.glActiveTexture(GL_TEXTURE0);
    gl4.glBindTexture(GL_TEXTURE_2D, this.cpuDstTexture);
    this.gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    this.gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    this.gl4.glTexImage2D(
        GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_BGRA, GL_UNSIGNED_BYTE, cpuByteBuffer);
    this.gl4.glBindTexture(GL_TEXTURE_2D, 0);

    this.iDst = cpuDstTexture;
  }

  protected void run(double deltaMs, double enabledAmount) {
    // Temporarily avoid LX bug: don't run if onEnable() was never called
    if (!this.onEnableCalled) {
      return;
    }

    // Safety check: bail if the effect contains no shaders
    if (this.shaders.isEmpty()) {
      return;
    }

    iTime.tick();

    // In CPU mode, use dedicated input texture
    if (this.lx.engine.renderMode.cpu) {
      // Allocate texture on first run
      if (!this.cpuTextureInitialized) {
        this.cpuTextureInitialized = true;
        initializeCpuTexture();
        onEnable();
      }

      // Load colors[] into texture
      GLEngine.current.bindTextureUnit(0, this.cpuDstTexture);
      this.cpuByteBuffer.rewind();
      for (int i = 0; i < this.colors.length; i++) {
        this.cpuByteBuffer.putInt(this.colors[i]);
      }
      this.cpuByteBuffer.rewind();

      // Update texture without re-allocating
      this.gl4.glTexSubImage2D(
          GL_TEXTURE_2D,
          0,
          0,
          0,
          GLEngine.current.getWidth(),
          GLEngine.current.getHeight(),
          GL_BGRA,
          GL_UNSIGNED_BYTE,
          this.cpuByteBuffer);
      this.gl4.glBindTexture(GL_TEXTURE_2D, 0);
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
    if (this.lx.engine.renderMode.cpu) {
      this.shaders.getLast().setCpuBuffer(this.colors);
    } else {
      this.shaders.getLast().setCpuBuffer(null);
    }

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

    // TODO: confirm shader template for TE Effect matches these uniforms:
    this.uniforms.iDst = s.getUniformSampler2D("iDst");
    this.uniforms.iBrightness = s.getUniformFloat1("iBrightness");
    this.uniforms.iTime = s.getUniformFloat1("iTime");
    this.uniforms.iColorRGB = s.getUniformFloat3("iColorRGB");
    this.uniforms.iColorHSB = s.getUniformFloat3("iColorHSB");
    this.uniforms.iColor2RGB = s.getUniformFloat3("iColor2RGB");
    this.uniforms.iColor2HSB = s.getUniformFloat3("iColor2HSB");
  }

  private GLShader.UniformSource uniformSource =
      new GLShader.UniformSource() {
        /** Send a subset of the controls we use with patterns */
        @Override
        public void setUniforms(GLShader s) {
          if (!initializedUniforms) {
            initializedUniforms = true;
            initializeUniforms(s);
          }

          // Shaders are run in sequence.  Pass the current iDst value, which may be the output of
          // the
          // previous shader
          uniforms.iDst.setValue(currentShaderDst);
          uniforms.iBrightness.setValue(
              1.0f); // TODO: provide control for this or use effect level?
          uniforms.iTime.setValue((float) getTime());

          // get current primary and secondary colors
          // TODO - we're just grabbing swatch colors here.  Do we need to worry about modulation?
          int col = getColor1();
          uniforms.iColorRGB.setValue(
              (float) (0xff & LXColor.red(col)) / 255f,
              (float) (0xff & LXColor.green(col)) / 255f,
              (float) (0xff & LXColor.blue(col)) / 255f);
          uniforms.iColorHSB.setValue(
              LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);

          col = getColor2();
          uniforms.iColor2RGB.setValue(
              (float) (0xff & LXColor.red(col)) / 255f,
              (float) (0xff & LXColor.green(col)) / 255f,
              (float) (0xff & LXColor.blue(col)) / 255f);
          uniforms.iColor2HSB.setValue(
              LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);
        }

        @Override
        public void unbindTextures() {
          uniforms.iDst.unbind();
        }
      };

  @Override
  protected void onEnable() {
    super.onEnable();
    for (TEShader shader : this.shaders) {
      shader.onActive();
    }

    // Temporary bug fix:
    this.onEnableCalled = true;
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
