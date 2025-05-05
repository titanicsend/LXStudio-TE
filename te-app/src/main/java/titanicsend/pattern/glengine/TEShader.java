package titanicsend.pattern.glengine;

import static com.jogamp.opengl.GL.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.GL4;
import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import heronarts.lx.parameter.LXParameter;
import java.nio.*;
import java.util.*;
import titanicsend.pattern.yoffa.shader_engine.*;

/**
 * Shader class for TE patterns and effects. Adds image file textures and uniforms for TE common
 * controls.
 */
public class TEShader extends GLShader {

  // texture buffers
  private class TextureInfo {
    public String name;
    public int channel;
    public int textureUnit;
    public int uniformLocation;
  }

  private final ArrayList<TextureInfo> textures = new ArrayList<>();
  private final ByteBuffer backBuffer;
  private final int[] backbufferHandle = new int[1];

  // support for optional texture mapped (as opposed to the new linear format) buffer
  // containing the last rendered frame.
  private boolean useMappedBuffer = false;
  private ByteBuffer mappedBuffer = null;
  private final int[] mappedBufferHandle = new int[1];
  private int mappedBufferUnit = -1;
  private int mappedBufferWidth = 640;
  private int mappedBufferHeight = 640;

  // the GL texture unit to which the current view model coordinate
  // texture is bound.
  private int modelCoordsTextureUnit = -1;
  private boolean modelCoordsChanged = false;

  // Welcome to the Land of 1000 Constructors!

  public TEShader(
      LX lx,
      FragmentShader fragmentShader,
      ByteBuffer frameBuf,
      List<UniformSource> uniformSources) {
    super(lx, fragmentShader);

    // allocate default buffer for reading offscreen surface to cpu memory
    this.backBuffer = frameBuf != null ? frameBuf : allocateBackBuffer();

    // Uniform callback for TE common controls
    addUniformSource(this::setUniforms);
    // Children set uniforms last, giving user the option to override any default values
    for (UniformSource uniformSource : uniformSources) {
      if (uniformSource != null) {
        addUniformSource(uniformSource);
      }
    }
  }

  /**
   * Create new OpenGL shader effect
   *
   * @param lx LX instance
   * @param fragmentShader fragment shader object to use
   * @param frameBuf native (GL compatible) ByteBuffer to store render results for use in shaders
   *     that need to read the previous frame. If null, a buffer will be automatically allocated.
   * @param uniformSource callback that will set uniforms on this shader
   */
  public TEShader(
      LX lx, FragmentShader fragmentShader, ByteBuffer frameBuf, UniformSource uniformSource) {
    this(lx, fragmentShader, frameBuf, List.of(uniformSource));
  }

  /**
   * Create new shader object with default backbuffer
   *
   * @param lx LX instance
   * @param fragmentShader fragment shader object shader to use
   * @param uniformSource callback that will set uniforms on this shader
   */
  public TEShader(LX lx, FragmentShader fragmentShader, UniformSource uniformSource) {
    this(lx, fragmentShader, null, uniformSource);
  }

  /**
   * Creates new shader object with additional texture support
   *
   * @param lx LX instance
   * @param shaderFilename shader to use
   * @param frameBuf native (GL compatible) ByteBuffer to store render results for use in shaders
   *     that need to read the previous frame. If null, a buffer will be automatically allocated. *
   * @param uniformSource callback that will set uniforms on this shader
   * @param textureFilenames (optional) texture files to load
   */
  public TEShader(
      LX lx,
      String shaderFilename,
      UniformSource uniformSource,
      ByteBuffer frameBuf,
      String... textureFilenames) {
    this(lx, shaderFilename, List.of(uniformSource), frameBuf, textureFilenames);
  }

  /**
   * Creates new shader object with additional texture support
   *
   * @param lx LX instance
   * @param shaderFilename shader to use
   * @param frameBuf native (GL compatible) ByteBuffer to store render results for use in shaders
   *     that need to read the previous frame. If null, a buffer will be automatically allocated. *
   * @param uniformSources list of callbacks that will set uniforms on this shader
   * @param textureFilenames (optional) texture files to load
   */
  public TEShader(
      LX lx,
      String shaderFilename,
      List<UniformSource> uniformSources,
      ByteBuffer frameBuf,
      String... textureFilenames) {
    this(lx, newFragmentShader(shaderFilename, textureFilenames), frameBuf, uniformSources);
  }

  /**
   * Creates new shader object with additional texture support
   *
   * @param lx LX instance
   * @param shaderFilename shader to use
   * @param uniformSource callback that will set uniforms on this shader
   * @param textureFilenames (optional) texture files to load
   */
  public TEShader(
      LX lx, String shaderFilename, UniformSource uniformSource, String... textureFilenames) {
    this(lx, shaderFilename, uniformSource, null, textureFilenames);
  }

  /**
   * Creates new shader object with additional texture support
   *
   * @param lx LX instance
   * @param shaderFilename shader to use
   * @param uniformSources list of callbacks that will set uniforms on this shader
   * @param textureFilenames (optional) texture files to load
   */
  public TEShader(
      LX lx,
      String shaderFilename,
      List<UniformSource> uniformSources,
      String... textureFilenames) {
    this(lx, shaderFilename, uniformSources, null, textureFilenames);
  }

  // Setup

  // get the active GL profile so the calling entity can work with
  // GL textures and buffers if necessary.  (NDI support
  // requires this, for example.)
  public GLProfile getGLProfile() {
    return gl4.getGLProfile();
  }

  /**
   * Set the buffer to be used as a rectangular texture backbuffer for this shader. (As opposed to
   * iBackbuffer, which is a linear list of colors corresponding to LX 3D model points and can't be
   * used for algorithms that need the ability to access neighboring pixels.) NOTE: MUST BE CALLED
   * BEFORE THE SHADER IS INITIALIZED. (i.e. in the pattern's constructor.) TODO - at present, this
   * buffer is only used by shader effects. It should eventually TODO - be optional for shader
   * patterns as well.
   *
   * @param buffer previously allocated ByteBuffer of sufficient size to hold the desired texture
   */
  public void setMappedBuffer(ByteBuffer buffer, int width, int height) {
    this.mappedBufferWidth = width;
    this.mappedBufferHeight = height;
    this.mappedBuffer = buffer;
    this.useMappedBuffer = true;
  }

  public List<ShaderConfiguration> getShaderConfig() {
    return fragmentShader.getShaderConfig();
  }

  /** List of LX control parameters from the shader code */
  public List<LXParameter> getParameters() {
    return this.fragmentShader.parameters;
  }

  // Initialization

  /**
   * Called at pattern initialization time to allocate and configure GPU buffers that are common to
   * all shaders.
   */
  protected void allocateShaderBuffers() {
    super.allocateShaderBuffers();

    // backbuffer texture object
    gl4.glActiveTexture(GL_TEXTURE2);
    gl4.glEnable(GL_TEXTURE_2D);
    gl4.glGenTextures(1, backbufferHandle, 0);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, backbufferHandle[0]);

    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    gl4.glBindTexture(GL_TEXTURE_2D, 0);

    // create mapped buffer texture object, if needed
    if (useMappedBuffer) {
      this.mappedBufferUnit = glEngine.getNextTextureUnit();

      gl4.glActiveTexture(GL_TEXTURE0 + mappedBufferUnit);
      gl4.glEnable(GL_TEXTURE_2D);
      gl4.glGenTextures(1, mappedBufferHandle, 0);
      gl4.glBindTexture(GL4.GL_TEXTURE_2D, mappedBufferHandle[0]);

      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

      gl4.glBindTexture(GL_TEXTURE_2D, 0);
    }

    // assign shared uniform blocks to the shader's binding points
    int perRunBlockIndex = gl4.glGetUniformBlockIndex(shaderProgram.id, "PerRunBlock");
    gl4.glUniformBlockBinding(
        shaderProgram.id, perRunBlockIndex, GLEngine.perRunUniformBlockBinding);

    int perFrameBlockIndex = gl4.glGetUniformBlockIndex(shaderProgram.id, "PerFrameBlock");
    gl4.glUniformBlockBinding(
        shaderProgram.id, perFrameBlockIndex, GLEngine.perFrameUniformBlockBinding);

    loadTextureFiles();
  }

  private void loadTextureFiles() {
    for (Map.Entry<Integer, String> textureInput :
        this.fragmentShader.getChannelToTexture().entrySet()) {

      TextureInfo ti = new TextureInfo();
      ti.textureUnit = glEngine.useTexture(this.gl4, textureInput.getValue());
      ti.name = textureInput.getValue();
      ti.channel = textureInput.getKey();
      ti.uniformLocation =
          this.gl4.glGetUniformLocation(this.shaderProgram.id, "iChannel" + ti.channel);

      textures.add(ti);
    }
  }

  // Run loop

  private void setUniforms(GLShader s) {
    // Set audio waveform and fft data as a 512x2 texture on the specified audio
    // channel if it's a shadertoy shader, or iChannel0 if it's a local shader.

    // By Imperial Decree, the audio texture will heretofore always use texture unit
    // GL_TEXTURE0, the model coordinate array will use GL_TEXTURE1 and the backbuffer
    // texture will use GL_TEXTURE2. Other (shader-specific) textures will be automatically
    // bound to sequential ids starting with GL_TEXTURE3.
    //
    // The audio texture can be used by all shaders, and stays bound to texture
    // unit 0 throughout the Chromatik run. All we have to do to use it is add the uniform.
    setUniform(UniformNames.AUDIO_CHANNEL, 0);

    // use the current view's model coordinates texture which
    // has already been loaded to the GPU by the texture cache manager.
    // All we need to do is point at the right GL texture unit.
    if (this.modelCoordsChanged) {
      this.modelCoordsChanged = false;
      setUniform(UniformNames.LX_MODEL_COORDS, this.modelCoordsTextureUnit);
    }

    // Update backbuffer texture data. This buffer contains the result of the
    // previous render pass.  It is always bound to texture unit 2.
    gl4.glActiveTexture(GL_TEXTURE2);
    gl4.glEnable(GL_TEXTURE_2D);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, backbufferHandle[0]);

    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D,
        0,
        GL4.GL_RGBA,
        width,
        height,
        0,
        GL4.GL_BGRA,
        GL_UNSIGNED_BYTE,
        backBuffer);

    setUniform(UniformNames.BACK_BUFFER, 2);

    // if necessary, update the mapped buffer texture data
    if (useMappedBuffer) {
      gl4.glActiveTexture(GL_TEXTURE0 + mappedBufferUnit);
      gl4.glEnable(GL_TEXTURE_2D);
      gl4.glBindTexture(GL4.GL_TEXTURE_2D, mappedBufferHandle[0]);

      gl4.glTexImage2D(
          GL4.GL_TEXTURE_2D,
          0,
          GL4.GL_RGBA,
          mappedBufferWidth,
          mappedBufferHeight,
          0,
          GL4.GL_BGRA,
          GL_UNSIGNED_BYTE,
          mappedBuffer);

      setUniform(UniformNames.MAPPED_BUFFER, mappedBufferUnit);
    }

    // add shadertoy texture channels. These textures already statically bound to
    // texture units so all we have to do is tell the shader which texture unit to use.
    for (TextureInfo ti : textures) {
      setUniform(UniformNames.CHANNEL + ti.channel, ti.textureUnit);
      gl4.glUniform1i(ti.uniformLocation, ti.textureUnit);
    }

    // Add all preprocessed LX parameters from the shader code as uniforms
    for (LXParameter customParameter : fragmentShader.parameters) {
      setUniform(
          customParameter.getLabel() + UniformNames.LX_PARAMETER_SUFFIX,
          customParameter.getValuef());
    }
  }

  protected void render() {
    gl4.glBindVertexArray(getVaoHandle());
    gl4.glDrawElements(GL2.GL_TRIANGLES, INDICES.length, GL2.GL_UNSIGNED_INT, 0);

    saveBackBuffer();
  }

  private void saveBackBuffer() {
    backBuffer.rewind();
    gl4.glReadBuffer(GL_BACK);

    // using BGRA byte order lets us read int values from the buffer and pass them
    // directly to LX as colors, without any additional work on the Java side.
    gl4.glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_BYTE, backBuffer);
  }

  public ByteBuffer getBackBuffer() {
    return backBuffer;
  }

  // Staging Uniforms: LX Model

  /**
   * Copy LXPoints' normalized coordinates into textures for use by shaders. Must be called by the
   * parent pattern or effect at least once before the first frame is rendered. And should be called
   * by the pattern's frametime run() function if the model has changed since the last frame.
   *
   * @param model Current LXModel of the calling context, which is a LXView or the global model
   */
  public void setModelCoordinates(LXModel model) {
    this.modelCoordsTextureUnit = this.glEngine.getCoordinatesTexture(model);
    this.modelCoordsChanged = true;
  }

  // Releases native resources allocated by this shader.
  // Should be called by the pattern's dispose() function
  // when the pattern is unloaded. (Not when just
  // deactivated.)
  public void dispose() {
    // if we've been fully initialized, we need to release all
    // OpenGL GPU resources we've allocated.
    if (isInitialized()) {

      // Back Buffer
      gl4.glDeleteTextures(1, backbufferHandle, 0);

      // Mapped Buffer
      if (useMappedBuffer) {
        gl4.glDeleteTextures(1, mappedBufferHandle, 0);
        glEngine.releaseTextureUnit(mappedBufferUnit);
      }

      // free any textures on ShaderToy channels
      for (TextureInfo ti : textures) {
        glEngine.releaseTexture(ti.name);
      }
    }

    super.dispose();
  }
}
