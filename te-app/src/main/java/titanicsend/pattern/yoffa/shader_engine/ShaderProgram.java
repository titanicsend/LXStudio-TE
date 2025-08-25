package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.GL4;

public class ShaderProgram {

  private final GL4 gl4;
  public final String name;
  public final int id;

  public ShaderProgram(GL4 gl4, String shaderName) {
    this(gl4, shaderName, true);
  }

  public ShaderProgram(GL4 gl4, String shaderName, boolean tePreProcess) {
    this.gl4 = gl4;
    this.name = shaderName;
    this.id = gl4.glCreateProgram();

    boolean inCache = ShaderUtils.loadShaderFromCache(gl4, this.id, shaderName);
    if (!inCache) {
      ShaderUtils.buildShader(gl4, id, shaderName, tePreProcess);
    }
  }

  public void dispose() {
    gl4.glDeleteProgram(id);
  }
}
