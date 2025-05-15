package titanicsend.pattern.glengine.mixer;

import titanicsend.pattern.glengine.GLShader;

public class BlendShader extends GLShader {

  // Framebuffer object (FBO) for rendering
  private FBO fbo;

  // Input texture units
  private final int textureUnitSrc;
  private final int textureUnitDst;

  // Input texture handles (src = blending from, dst = blending into)
  public int iSrc = -1;
  public int iDst = -1;

  // Amount of src to blend into dst
  private float level = 0f;

  //  public BlendShader(LX lx, String fragmentShaderFilename) {
  public BlendShader(GLShader.Config config) {
    super(config);

    addUniformSource(this::setUniforms);

    // Use the first two "rotating" texture unit slots, avoiding our few reserved units
    this.textureUnitSrc = getNextTextureUnit();
    this.textureUnitDst = getNextTextureUnit();
  }

  @Override
  protected boolean useTEPreProcess() {
    return false;
  }

  @Override
  protected void allocateShaderBuffers() {
    super.allocateShaderBuffers();

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

  /** Stage new uniform values that need to be sent to the shader */
  private void setUniforms(GLShader s) {
    if (iSrc < 0 || iDst < 0) {
      throw new IllegalStateException("BlendShader: input textures have not been set");
    }

    // Bind input textures to texture units
    bindTextureUnit(this.textureUnitSrc, this.iSrc);
    setUniform("iSrc", this.textureUnitSrc);

    bindTextureUnit(this.textureUnitDst, this.iDst);
    setUniform("iDst", this.textureUnitDst);

    setUniform("level", this.level);
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
