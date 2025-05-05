package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.GL4;

public class ShaderProgram {

  private final int programId;
  private final GL4 gl4;

  public ShaderProgram(GL4 gl4, String shaderName) {
    this.gl4 = gl4;
    programId = gl4.glCreateProgram();

    boolean inCache = ShaderUtils.loadShaderFromCache(gl4, programId, shaderName);
    if (!inCache) {
      ShaderUtils.buildShader(gl4, programId, shaderName);
    }
  }

  public void dispose() {
    gl4.glDeleteProgram(programId);
  }

  public int getProgramId() {
    return programId;
  }
}
