package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.GL4;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ShaderProgram {

    private final Map<ShaderAttribute, Integer> shaderAttributeLocations = new HashMap<>();
    private boolean initialized = false;
    private int programId;
    private int vertexShaderId;
    private int fragmentShaderId;

    public void init(GL4 gl4, File vertexShader, String fragmentShaderCode, String shaderName, long ts) {
        if (initialized) {
            throw new IllegalStateException(
                    "Unable to initialize the shader program! (it was already initialized)");
        }

        try {
            programId = gl4.glCreateProgram();
            boolean inCache = ShaderUtils.loadShaderFromCache(gl4,programId,shaderName,ts);

            if (!inCache) {
                String vertexShaderCode = ShaderUtils.loadResource(vertexShader
                        .getPath());
                vertexShaderId = ShaderUtils.createShader(gl4, programId,
                        vertexShaderCode, GL4.GL_VERTEX_SHADER);
                fragmentShaderId = ShaderUtils.createShader(gl4, programId,
                        fragmentShaderCode, GL4.GL_FRAGMENT_SHADER);

                ShaderUtils.link(gl4, programId);

                shaderAttributeLocations.put(ShaderAttribute.POSITION,
                        gl4.glGetAttribLocation(programId, ShaderAttribute.POSITION.getAttributeName()));
                shaderAttributeLocations.put(ShaderAttribute.INDEX,
                        gl4.glGetAttribLocation(programId, ShaderAttribute.INDEX.getAttributeName()));

                ShaderUtils.saveShaderToCache(gl4, shaderName, programId);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        initialized = true;
    }

    public void dispose(GL4 gl4) {
        initialized = false;
        gl4.glDetachShader(programId, vertexShaderId);
        gl4.glDetachShader(programId, fragmentShaderId);
        gl4.glDeleteProgram(programId);
    }

    public int getProgramId() {
        if (!initialized) {
            throw new IllegalStateException(
                    "Unable to get the program id! The shader program was not initialized!");
        }
        return programId;
    }

    public int getShaderAttributeLocation(ShaderAttribute shaderAttribute) {
        if (!initialized) {
            throw new IllegalStateException(
                    "Unable to get the attribute location! The shader program was not initialized!");
        }
        return shaderAttributeLocations.get(shaderAttribute);
    }

    public boolean isInitialized() {
        return initialized;
    }

}
