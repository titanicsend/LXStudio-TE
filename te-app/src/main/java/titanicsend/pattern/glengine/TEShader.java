package titanicsend.pattern.glengine;

import heronarts.lx.model.LXModel;
import heronarts.lx.parameter.LXParameter;
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

  private static class TEShaderUniforms {
    private Uniform.Int1 audio;
    private Uniform.Int1 lxModelCoords;
    private Uniform.Int1 backBuffer;
    private Uniform.Int1 mappedBuffer;
  }

  private final TEShaderUniforms uniforms = new TEShaderUniforms();
  private boolean initializedUniforms = false;

  public TEShader(GLShader.Config config) {
    super(config);

    // Uniform callback for TE common controls
    addUniformSource(this::setUniforms);
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

  private void initializeUniforms() {
    this.uniforms.audio = getUniformInt1(UniformNames.AUDIO_CHANNEL);
    this.uniforms.lxModelCoords = getUniformInt1(UniformNames.LX_MODEL_COORDS);
    this.uniforms.backBuffer = getUniformInt1(UniformNames.BACK_BUFFER);
    this.uniforms.mappedBuffer = getUniformInt1(UniformNames.MAPPED_BUFFER);
  }

  private void setUniforms(GLShader s) {
    // Use Uniform objects to track locations and values
    if (!initializedUniforms) {
      this.initializedUniforms = true;
      initializeUniforms();
    }

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
    this.uniforms.audio.setValue(TEXTURE_UNIT_AUDIO);

    // use the current view's model coordinates texture which
    // has already been loaded to the GPU by the texture cache manager.
    // All we need to do is bind it to right GL texture unit.
    bindTextureUnit(TEXTURE_UNIT_COORDS, this.modelCoordsTextureHandle);
    this.uniforms.lxModelCoords.setValue(TEXTURE_UNIT_COORDS);

    // Use older FBO as backbuffer
    bindTextureUnit(TEXTURE_UNIT_BACKBUFFER, this.ppFBOs.copy.getTextureHandle());
    this.uniforms.backBuffer.setValue(TEXTURE_UNIT_BACKBUFFER);

    // GL will complain if you don't assign a unit to the sampler2D...
    this.uniforms.mappedBuffer.setValue(TEXTURE_UNIT_BACKBUFFER);

    // Bind shadertoy textures to corresponding shader-specific texture units.
    for (TextureInfo ti : textures) {
      bindTextureUnit(ti.unit, ti.handle);
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

    // Unbind textures (except for audio, which stays bound for all patterns)
    unbindTextureUnit(TEXTURE_UNIT_COORDS);
    unbindTextureUnit(TEXTURE_UNIT_BACKBUFFER);
    for (TextureInfo ti : textures) {
      unbindTextureUnit(ti.unit);
    }
    activateDefaultTextureUnit();

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
