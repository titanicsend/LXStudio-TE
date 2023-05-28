package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.util.TE;

public class ShaderUtils {

    // paths to various shader resources
    private static final String SHADER_PATH = "resources/shaders/";
    private static final String FRAMEWORK_PATH = SHADER_PATH+"framework/";
    private static final String CACHE_PATH = SHADER_PATH+"cache/";

    private static final String FRAGMENT_SHADER_TEMPLATE =
            ShaderUtils.loadResource(FRAMEWORK_PATH+"template.fs");

    private static final String SHADER_BODY_PLACEHOLDER = "{{%shader_body%}}";

    private static final Pattern PLACEHOLDER_FINDER = Pattern.compile("\\{%(.*?)(\\[(.*?)\\])??\\}");

    public static String loadResource(String fileName) {
        try {
            Scanner s = new Scanner(new File(fileName), "UTF-8");
            s.useDelimiter("\\A");
            String result = s.next();
            s.close();
            return result;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Creates offscreen drawable OpenGL surface at the specified resolution
     *
     */
    public static GLAutoDrawable createGLSurface(int xResolution, int yResolution) {
        GLProfile glProfile = GLProfile.getGL4ES3();
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glCapabilities.setHardwareAccelerated(true);
        glCapabilities.setOnscreen(false);
        glCapabilities.setDoubleBuffered(false);
        // set bit count for all channels to get alpha to work correctly
        glCapabilities.setAlphaBits(8);
        glCapabilities.setRedBits(8);
        glCapabilities.setBlueBits(8);
        glCapabilities.setGreenBits(8);
        GLDrawableFactory factory = GLDrawableFactory.getFactory(glProfile);

        //need to specifically create an offscreen drawable
        //there is no way to have a normal drawable render on a panel/canvas which is not visible
        GLAutoDrawable offscreenDrawable = factory.createOffscreenAutoDrawable(factory.getDefaultDevice(), glCapabilities,
                new DefaultGLCapabilitiesChooser(), xResolution, yResolution);
        //offscreenDrawable.display();
        return offscreenDrawable;
    }

    /**
     * Returns string containing the preprocessed code of the specified shader
     */
    public static String getFragmentShaderCode(String shaderFile) {
        //String shaderCode = FRAGMENT_SHADER_TEMPLATE.replace(SHADER_BODY_PLACEHOLDER, fragmentShader.getShaderBody());
        return null;
    }

    /**
     * Preprocess the shader, converting embedded control specifiers to proper uniforms,
     * and optionally creating corresponding controls if the "pattern" parameter is non-null.
     *
     */
    public static String preprocessShader(String shaderBody, List<LXParameter> parameters) {
        Matcher matcher = PLACEHOLDER_FINDER.matcher(shaderBody);
        // preallocate reasonable sized buffers to keep us out of Java's memory manager while looping
        StringBuilder shaderCode = new StringBuilder(shaderBody.length());
        StringBuilder finalShader = new StringBuilder(shaderBody.length()+256);
        while (matcher.find()) {
            try {
                String placeholderName = matcher.group(1);
                if (matcher.groupCount() >= 3) {
                    String metadata = matcher.group(3);
                    if ("bool".equals(metadata)) {
                        finalShader.append("uniform bool " + placeholderName+Uniforms.CUSTOM_SUFFIX+";\n");
                        if (parameters != null) {
                            parameters.add(new BooleanParameter(placeholderName));
                        }
                    } else {
                        finalShader.append("uniform float " + placeholderName+Uniforms.CUSTOM_SUFFIX+";\n");
                        if (parameters != null) {
                            Double[] rangeValues = Arrays.stream(metadata.split(","))
                                    .map(Double::parseDouble)
                                    .toArray(Double[]::new);
                            parameters.add(new CompoundParameter(placeholderName, rangeValues[0], rangeValues[1], rangeValues[2]));
                        }
                    }
                }
                matcher.appendReplacement(shaderCode, placeholderName + Uniforms.CUSTOM_SUFFIX);
            } catch (Exception e) {
                throw new RuntimeException("Problem parsing placeholder: " + matcher.group(0), e);
            }
        }
        matcher.appendTail(shaderCode);
        finalShader.append(shaderCode);

        return finalShader.toString();
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
        // strip the incoming path down to just the filename, and build
        // the cache file path from there.
        String shaderFile = shaderName.substring(shaderName.lastIndexOf('/')+1);
        String[] parts = shaderFile.split("\\.");

        return String.format(CACHE_PATH+"%s.bin",parts[0]);
    }

    static boolean isNewerThan(File f1, File f2) {
        // if either of the files doesn't exist, clearly *something* has been modified
        // in this case returning true will result in either a recompile or an exception,
        // depending on which file is missing.
        if (!f1.exists()) {
            return true;
        }
        if (!f2.exists()) {
            return true;
        }
        // return true if file1 is newer than file2
        if (f1.lastModified() > f2.lastModified()) {
            return true;
        }
        return false;
    }

    static boolean isNewerThan(String file1, String file2) {
        File f1 = new File(file1);
        File f2 = new File(file2);

        return isNewerThan(f1, f2);
    }

    /**
     * True if we need to recompile this shader because either the framework
     * or the shader code have been modified since last compile, false otherwise
     * @param shaderFile path to the shader's glsl file
     * @return true if recompile needed, false otherwise
     */
    public static boolean needsRecompile(String shaderFile) {
        String cacheFile = ShaderUtils.getCacheFilename(shaderFile);

        if (isNewerThan(FRAMEWORK_PATH+"default.vs",cacheFile)) {
            TE.log("Vertex shader framework has been modified.");
            return true;
        }

        if (isNewerThan(FRAMEWORK_PATH+"template.fs",cacheFile)) {
            TE.err("Fragment shader framework been modified.");
            return true;
        }

        if (isNewerThan(SHADER_PATH+shaderFile,cacheFile)) {
            TE.log("Shader '%s` has been modified.",shaderFile);
            return true;
        }

        // if here, no recompile needed.
        return false;
    }

    /**
     * Attempts to read the named shader from resources/shaders/cache.  Returns
     * a ByteBuffer full of shader binary if successful, null otherwise.
     */
    public static boolean loadShaderFromCache(GL4 gl4,int programID, String shaderName) {

        // account for shadertoy shaders pulled in via URL
        if (shaderName == null) return false;

        // see if the shader or the framework have changed since last recompile
        if (needsRecompile(shaderName)) {
            return false;
        }

        // attempt to read shader binary from cache file
        String cacheFile = getCacheFilename(shaderName);

        try {
            byte [] outBuf = Files.readAllBytes(Path.of( cacheFile));
            ByteBuffer shader = ByteBuffer.wrap(outBuf);

            // attach binary to our shader program
            gl4.glProgramBinary(programID,0,shader,outBuf.length);
        }
        catch (IOException e) {
            TE.log("I/O Exception reading shader '%s'.",shaderName);
            return false;
        }
        
        // make sure we were able to create a valid shader program
        int[] status = new int[1];
        gl4.glGetIntegerv(GL4.GL_LINK_STATUS,status,0);
       // TE.log("Shader '%s' loaded from cache",shaderName);
        return (status[0] != GL4.GL_FALSE);
    }

    /**
     * Save compiled and linked shader binary in a cache file, in
     * resources/shaders/cache
     */
    public static void saveShaderToCache(GL4 gl4, String shaderName, int programId) {

        // account for shadertoy shaders pulled in via URL
        if (shaderName == null) return;

        // get the size of the shader binary in bytes
        int[] len = new int[1];
        gl4.glGetProgramiv(programId,GL4.GL_PROGRAM_BINARY_LENGTH,len,0);

        // get available binary formats for shader storage
        int[] fmtCount = new int[1];
        gl4.glGetIntegerv(GL4.GL_NUM_PROGRAM_BINARY_FORMATS,fmtCount,0);

        if (fmtCount[0] < 1) {
            //TE.log("Shader cache: No compatible binary shader format available.");
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
