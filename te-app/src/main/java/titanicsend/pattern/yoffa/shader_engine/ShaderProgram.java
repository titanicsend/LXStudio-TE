package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.GL4;
import java.util.HashMap;
import java.util.Map;

public class ShaderProgram {

  private final Map<ShaderAttribute, Integer> shaderAttributeLocations = new HashMap<>();
  private final int programId;
  private final GL4 gl4;

  public ShaderProgram(GL4 gl4, String shaderName) {
    this.gl4 = gl4;
    programId = gl4.glCreateProgram();

    boolean inCache = ShaderUtils.loadShaderFromCache(gl4, programId, shaderName);
    if (!inCache) {
      ShaderUtils.buildShader(gl4, programId, shaderName);
    }

    shaderAttributeLocations.put(
        ShaderAttribute.POSITION,
        gl4.glGetAttribLocation(programId, ShaderAttribute.POSITION.getAttributeName()));

    // NOTE: Uncomment when we make the geometry complex enough that we need the index attribute.
    // shaderAttributeLocations.put(ShaderAttribute.INDEX,
    // gl4.glGetAttribLocation(programId, ShaderAttribute.INDEX.getAttributeName()));
  }

  public void dispose() {
    gl4.glDeleteProgram(programId);
  }

  public int getProgramId() {
    return programId;
  }

  public int getShaderAttributeLocation(ShaderAttribute shaderAttribute) {
    return shaderAttributeLocations.get(shaderAttribute);
  }
}
