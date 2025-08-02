package titanicsend.pattern.glengine;

import titanicsend.pattern.yoffa.shader_engine.Uniform;

/** Shader class for use with effect shaders. In CPU mode it adds an input texture. */
public class TEEffectShader extends TEShader {

  public TEEffectShader(Config config) {
    super(config);
  }

  private FBO fbo;
  private PBO pbo;
  Uniform uniformiDst;

  private boolean initializedUniforms = false;

  @Override
  protected void allocateShaderBuffers() {
    super.allocateShaderBuffers();

    if (this.lx.engine.renderMode.cpu) {
      this.fbo = new FBO();
      this.pbo = new PBO();
    }
  }

  private void initializeUniforms(GLShader s) {
    this.uniformiDst = s.getUniformSampler2D("iDst");
  }

  @Override
  public void setUniforms(GLShader s) {
    super.setUniforms(s);

    if (!initializedUniforms) {
      initializedUniforms = true;
      initializeUniforms(s);
    }
  }
}
