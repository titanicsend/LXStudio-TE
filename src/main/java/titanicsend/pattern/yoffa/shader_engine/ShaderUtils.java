package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.GL4;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Scanner;

public class ShaderUtils {

    public static String loadResource(String fileName) {
        try {
            return new Scanner(new File(fileName), "UTF-8").useDelimiter("\\A").next();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static int createShader(GL4 gl4, int programId, String shaderCode, int shaderType) throws Exception {
        int shaderId = gl4.glCreateShader(shaderType);
        if (shaderId == 0) {
            throw new Exception("Error creating shader. Shader id is zero.");
        }
        gl4.glShaderSource(shaderId, 1, new String[] { shaderCode }, null);
        gl4.glCompileShader(shaderId);
        validateStatus(gl4, shaderId, GL4.GL_COMPILE_STATUS);
        gl4.glAttachShader(programId, shaderId);
        return shaderId;
    }

    public static void link(GL4 gl4, int programId) throws Exception {
        gl4.glLinkProgram(programId);
        validateStatus(gl4, programId, GL4.GL_LINK_STATUS);

        gl4.glValidateProgram(programId);
        validateStatus(gl4, programId, GL4.GL_VALIDATE_STATUS);
    }

    private static void validateShaderCompileStatus(GL4 gl4, int shaderId) {
        validateStatus(gl4, shaderId, GL4.GL_COMPILE_STATUS);
    }

    private static void validateProgramStatus(GL4 gl4, int programId, int statusConstant) {
        validateStatus(gl4, programId, statusConstant);
    }

    private static void validateStatus(GL4 gl4, int id, int statusConstant) {
        boolean isShaderStatus = statusConstant == GL4.GL_COMPILE_STATUS;
        IntBuffer intBuffer = IntBuffer.allocate(1);
        if (isShaderStatus) {
            gl4.glGetShaderiv(id, statusConstant, intBuffer);
        } else {
            gl4.glGetProgramiv(id, statusConstant, intBuffer);
        }

        if (intBuffer.get(0) != 1) {
            if (isShaderStatus) {
                gl4.glGetShaderiv(id, GL4.GL_INFO_LOG_LENGTH, intBuffer);
            } else {
                gl4.glGetProgramiv(id, GL4.GL_INFO_LOG_LENGTH, intBuffer);
            }
            int size = intBuffer.get(0);
            String errorMessage = "";
            if (size > 0) {
                ByteBuffer byteBuffer = ByteBuffer.allocate(size);
                if (isShaderStatus) {
                    gl4.glGetShaderInfoLog(id, size, intBuffer, byteBuffer);
                } else {
                    gl4.glGetProgramInfoLog(id, size, intBuffer, byteBuffer);
                }
                errorMessage = new String(byteBuffer.array());
            }
            throw new RuntimeException("Error producing shader!\n" + errorMessage);
        }
    }
}
