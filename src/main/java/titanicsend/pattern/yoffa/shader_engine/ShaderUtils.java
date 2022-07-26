package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.GL4;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import titanicsend.util.TE;

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

    /**
     * Takes shader filename with extension, returns string containing path to
     * corresponding cache file.
     */
    public static String getCacheFilename(String shaderName) {
        String[] parts = shaderName.split("\\.");
        return String.format("resources/shaders/cache/%s.bin",parts[0]);
    }

    /**
     * Attempts to read the named shader from resources/shaders/cache.  Returns
     * a ByteBuffer full of shader binary if successful, null otherwise.
     */
    public static boolean loadShaderFromCache(GL4 gl4,int programID, String shaderName, long timeStamp) {

        // compare timestamp to see if the shader has been updated.
        File shaderBin = new File(shaderName);
        if (!shaderBin.exists()) {
            TE.log("Shader '%s' not found in cache",shaderName);
            return false;
        }

        if (timeStamp > shaderBin.lastModified()) {
            TE.log("Shader '%s` has been modified.",shaderName);
            return false;
        }

        // attempt to read shader binary from cache file
        try {
            byte [] outBuf = Files.readAllBytes(Path.of( shaderName));
            ByteBuffer shader = ByteBuffer.wrap(outBuf);

            // attach binary to our shader program
            gl4.glProgramBinary(programID,0,shader,outBuf.length);
        }
        catch (IOException e) {
            TE.log("I/O Exception reading shader '%s'.",shaderName);
            return false;
        }
        
        // see if it worked
        int[] status = new int[1];
        gl4.glGetIntegerv(GL4.GL_LINK_STATUS,status,0);
        TE.log("Shader '%s' loaded from cache",shaderName);
        return (status[0] != GL4.GL_FALSE);
    }

    /**
     * Save compiled and linked shader binary in a cache file, in
     * resources/shaders/cache
     */
    public static void saveShaderToCache(GL4 gl4, String shaderName, int programId) {

        // get the size of the shader binary in bytes
        int[] len = new int[1];
        gl4.glGetProgramiv(programId,GL4.GL_PROGRAM_BINARY_LENGTH,len,0);

        // get available binary formats for shader storage
        int[] fmtCount = new int[1];
        gl4.glGetIntegerv(GL4.GL_NUM_PROGRAM_BINARY_FORMATS,fmtCount,0);

        if (fmtCount[0] < 1) {
            TE.log("Shader cache: No compatible binary shader format available.");
            return;
        }

        // take the first available format
        int[] formatList = new int[fmtCount[0]];
        gl4.glGetIntegerv(GL4.GL_PROGRAM_BINARY_FORMATS,formatList,0);

        // now we can get the shader binary
        ByteBuffer bin = ByteBuffer.allocate(len[0]);
        gl4.glGetProgramBinary(programId,len[0],len,0,formatList, 0,bin);

        // and at long last, save it to a file!
        try {
            Files.write(Path.of(shaderName), bin.array());
        }
        catch (IOException e) {
            TE.log("I/O exception writing shader '%s",shaderName);
        }
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
