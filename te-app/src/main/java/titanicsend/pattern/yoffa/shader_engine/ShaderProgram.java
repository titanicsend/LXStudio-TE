package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.GL4;

public class ShaderProgram {

  public final int id;
  private final GL4 gl4;

  public ShaderProgram(GL4 gl4, String shaderName) {
    this.gl4 = gl4;
    this.id = gl4.glCreateProgram();

    boolean inCache = ShaderUtils.loadShaderFromCache(gl4, this.id, shaderName);
    if (!inCache) {
      ShaderUtils.buildShader(gl4, id, shaderName);
    }
  }

  public void dispose() {
    gl4.glDeleteProgram(id);
  }
}
