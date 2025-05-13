package titanicsend.pattern.glengine;

import static com.jogamp.opengl.GL.*;

import com.jogamp.opengl.*;
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

  private static final int UNINITIALIZED = -1;

  // texture buffers
  private class TextureInfo {
    public String name;
    public int channel;
    public String uniformName;
    public int handle;

    /** This texture is always bound to the following unit on this shader */
    public int unit;
  }

  private final ArrayList<TextureInfo> textures = new ArrayList<>();

  // Render buffers: ping-pong FBOs and textures
  private PingPongFBO ppFBOs;

  // Texture handle for the current view model coordinate texture
  private int modelCoordsTextureHandle = UNINITIALIZED;

  // Welcome to the Land of 1000 Constructors!

  public TEShader(
      LX lx,
      FragmentShader fragmentShader,
      ByteBuffer frameBuf,
      List<UniformSource> uniformSources) {
    super(lx, fragmentShader);

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
  @Override
  protected void allocateShaderBuffers() {
    super.allocateShaderBuffers();

    // Create ping-pong FBOs (framebuffers) and textures for rendering
    this.ppFBOs = new PingPongFBO();

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
      ti.name = textureInput.getValue();
      ti.channel = textureInput.getKey();
      ti.uniformName = UniformNames.CHANNEL + ti.channel;
      ti.handle = this.glEngine.textureCache.useTexture(textureInput.getValue());
      ti.unit = getNextTextureUnit();
      textures.add(ti);
    }
  }

  // Run loop

  private void setUniforms(GLShader s) {
    // Swap render/copy FBOs
    this.ppFBOs.swap();

    // Set audio waveform and fft data as a 512x2 texture on the specified audio
    // channel if it's a shadertoy shader, or iChannel0 if it's a local shader.

    // By Imperial Decree, the audio texture will heretofore always use texture unit
    // GL_TEXTURE0, the model coordinate array will use GL_TEXTURE1 and the backbuffer
    // texture will use GL_TEXTURE2. Other (shader-specific) textures will be automatically
    // bound to sequential ids starting with GL_TEXTURE3.
    //
    // The audio texture can be used by all shaders, and stays bound to texture
    // unit 0 throughout the Chromatik run. All we have to do to use it is add the uniform.
    setUniform(UniformNames.AUDIO_CHANNEL, TEXTURE_UNIT_AUDIO);

    // use the current view's model coordinates texture which
    // has already been loaded to the GPU by the texture cache manager.
    // All we need to do is bind it to right GL texture unit.
    gl4.glActiveTexture(GL_TEXTURE0 + TEXTURE_UNIT_COORDS);
    gl4.glEnable(GL_TEXTURE_2D);
    gl4.glBindTexture(GL_TEXTURE_2D, this.modelCoordsTextureHandle);
    setUniform(UniformNames.LX_MODEL_COORDS, TEXTURE_UNIT_COORDS);

    // Use older FBO as backbuffer
    gl4.glActiveTexture(GL_TEXTURE0 + TEXTURE_UNIT_BACKBUFFER);
    gl4.glEnable(GL_TEXTURE_2D);
    gl4.glBindTexture(GL_TEXTURE_2D, this.ppFBOs.copy.getTextureHandle());
    setUniform(UniformNames.BACK_BUFFER, TEXTURE_UNIT_BACKBUFFER);

    // GL will complain if you don't assign a unit to the sampler2D...
    setUniform(UniformNames.MAPPED_BUFFER, TEXTURE_UNIT_BACKBUFFER);

    // Bind shadertoy textures to corresponding shader-specific texture units.
    for (TextureInfo ti : textures) {
      gl4.glActiveTexture(GL_TEXTURE0 + ti.unit);
      gl4.glEnable(GL_TEXTURE_2D);
      gl4.glBindTexture(GL4.GL_TEXTURE_2D, ti.handle);
      setUniform(ti.uniformName, ti.unit);
    }

    // Add all preprocessed LX parameters from the shader code as uniforms
    for (LXParameter customParameter : fragmentShader.parameters) {
      setUniform(
          customParameter.getLabel() + UniformNames.LX_PARAMETER_SUFFIX,
          customParameter.getValuef());
    }
  }

  @Override
  protected void render() {
    // Bind vertex array object
    bindVAO();

    // Bind framebuffer object (FBO)
    this.ppFBOs.render.bind();

    // render a frame
    drawElements();

    // No need to unbind VAO.
    // Also not unbinding the FBO here, as other shader render passes will change it.
    // And GLMixer will unbind the last FBO at the end of postMix().
  }

  /** Called by GLMixer to retrieve the current render texture handle */
  public int getRenderTexture() {
    return this.ppFBOs.render.getTextureHandle();
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
    this.modelCoordsTextureHandle = this.glEngine.textureCache.getCoordinatesTexture(model);
  }

  // Releases native resources allocated by this shader.
  // Should be called by the pattern's dispose() function
  // when the pattern is unloaded. (Not when just
  // deactivated.)
  @Override
  public void dispose() {
    // release all OpenGL GPU resources we've allocated
    if (isInitialized()) {
      this.ppFBOs.dispose();

      // free any textures on ShaderToy channels
      for (TextureInfo ti : textures) {
        this.glEngine.textureCache.releaseStaticTexture(ti.name);
      }
    }

    super.dispose();
  }
}
