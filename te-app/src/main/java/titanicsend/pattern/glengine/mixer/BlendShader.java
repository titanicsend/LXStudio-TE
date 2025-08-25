package titanicsend.pattern.glengine.mixer;

import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.yoffa.shader_engine.Uniform;

public class BlendShader extends GLShader implements GLShader.UniformSource {

  // Framebuffer object (FBO) for rendering
  private FBO fbo;

  // Input texture handles (src = blending from, dst = blending into)
  public int iDst = -1;
  public int iSrc = -1;

  // Amount of src to blend into dst
  private float level = 0f;

  private static class BlendUniforms {
    private Uniform.Sampler2D iDst;
    private Uniform.Sampler2D iSrc;
    private Uniform.Float1 level;
  }

  private final BlendUniforms uniforms = new BlendUniforms();
  private boolean initializedUniforms = false;

  //  public BlendShader(LX lx, String fragmentShaderFilename) {
  public BlendShader(GLShader.Config config) {
    super(config);

    addUniformSource(this);
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

  public void setDst(int iDst) {
    this.iDst = iDst;
  }

  public void setSrc(int iSrc) {
    this.iSrc = iSrc;
  }

  public void setLevel(float level) {
    this.level = level;
  }

  private void initializeUniforms() {
    // These will take the first two "rotating" texture unit slots, avoiding our few reserved units
    this.uniforms.iDst = getUniformSampler2D("iDst");
    this.uniforms.iSrc = getUniformSampler2D("iSrc");
    this.uniforms.level = getUniformFloat1("level");
  }

  /** Stage new uniform values that need to be sent to the shader */
  @Override
  public void setUniforms(GLShader s) {
    // Use Uniform objects to track locations and values
    if (!initializedUniforms) {
      this.initializedUniforms = true;
      initializeUniforms();
    }

    // Stage uniform values for updating
    this.uniforms.iDst.setValue(this.iDst);
    this.uniforms.iSrc.setValue(this.iSrc);
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

    // No need to unbind VAO.
    // Also not unbinding the FBO here, as other shader render passes will change it.
    // And GLMixer will unbind the last FBO at the end of postMix().
  }

  @Override
  public void unbindTextures() {
    this.uniforms.iDst.unbind();
    this.uniforms.iSrc.unbind();
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
