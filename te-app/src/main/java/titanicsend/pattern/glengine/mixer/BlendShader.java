package titanicsend.pattern.glengine.mixer;

import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.yoffa.shader_engine.Uniform;

public class BlendShader extends GLShader {

  // Framebuffer object (FBO) for rendering
  private FBO fbo;

  // Input texture units
  private int textureUnitSrc;
  private int textureUnitDst;

  // Input texture handles (src = blending from, dst = blending into)
  public int iSrc = -1;
  public int iDst = -1;

  // Amount of src to blend into dst
  private float level = 0f;

  private static class BlendUniforms {
    private Uniform.Int1 iSrc;
    private Uniform.Int1 iDst;
    private Uniform.Float1 level;
  }

  private final BlendUniforms uniforms = new BlendUniforms();
  private boolean initializedUniforms = false;

  //  public BlendShader(LX lx, String fragmentShaderFilename) {
  public BlendShader(GLShader.Config config) {
    super(config);

    addUniformSource(this::setUniforms);
  }

  @Override
  protected boolean useTEPreProcess() {
    return false;
  }

  @Override
  protected void allocateShaderBuffers() {
    super.allocateShaderBuffers();

    // Use the first two "rotating" texture unit slots, avoiding our few reserved units
    this.textureUnitSrc = getNextTextureUnit();
    this.textureUnitDst = getNextTextureUnit();

    // FBO (framebuffer and texture) for rendering
    this.fbo = new FBO();
  }

  public void setSrc(int iSrc) {
    this.iSrc = iSrc;
  }

  public void setDst(int iDst) {
    this.iDst = iDst;
  }

  public void setLevel(float level) {
    this.level = level;
  }

  private void initializeUniforms() {
    this.uniforms.iSrc = getUniformInt1("iSrc");
    this.uniforms.iDst = getUniformInt1("iDst");
    this.uniforms.level = getUniformFloat1("level");
  }

  /** Stage new uniform values that need to be sent to the shader */
  private void setUniforms(GLShader s) {
    if (iSrc < 0 || iDst < 0) {
      throw new IllegalStateException("BlendShader: input textures have not been set");
    }

    // Use Uniform objects to track locations and values
    if (!initializedUniforms) {
      this.initializedUniforms = true;
      initializeUniforms();
    }

    // Bind input textures to texture units
    bindTextureUnit(this.textureUnitSrc, this.iSrc);
    bindTextureUnit(this.textureUnitDst, this.iDst);

    // Stage uniform values for updating
    this.uniforms.iSrc.setValue(this.textureUnitSrc);
    this.uniforms.iDst.setValue(this.textureUnitDst);
    this.uniforms.level.setValue(this.level);
  }

  @Override
  protected void render() {
    // Bind vertex array object
    bindVAO();

    // Bind framebuffer object (FBO)
    this.fbo.bind();

    // Render frame to FBO
    drawElements();

    // Unbind textures
    unbindTextureUnit(this.textureUnitSrc);
    unbindTextureUnit(this.textureUnitDst);
    activateDefaultTextureUnit();

    // No need to unbind VAO.
    // Also not unbinding the FBO here, as other shader render passes will change it.
    // And GLMixer will unbind the last FBO at the end of postMix().
  }

  /** Retrieve the output texture handle */
  public int getRenderTexture() {
    return this.fbo.getTextureHandle();
  }

  @Override
  public void dispose() {
    if (isInitialized()) {
      this.fbo.dispose();
    }
    super.dispose();
  }
}
