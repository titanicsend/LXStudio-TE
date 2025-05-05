package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.*;
import com.jogamp.opengl.util.GLBuffers;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;
import titanicsend.pattern.glengine.GLPreprocessor;
import titanicsend.pattern.glengine.ShaderConfiguration;
import titanicsend.util.TE;

public class ShaderUtils {

  // paths to various shader resources
  public static final String SHADER_PATH = "resources/shaders/";
  public static final String FRAMEWORK_PATH = SHADER_PATH + "framework/";
  public static final String CACHE_PATH = SHADER_PATH + "cache/";

  // Strings for internal use by the preprocessor
  public static final String SHADER_BODY_PLACEHOLDER = "{{%shader_body%}}";
  public static final Pattern PLACEHOLDER_FINDER = Pattern.compile("\\{%(.*?)(\\[(.*?)\\])??\\}");

  public static String loadResource(File file) {
    try {
      Scanner s = new Scanner(file, "UTF-8");
      s.useDelimiter("\\A");
      String result = s.next();
      s.close();
      return result;
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static String loadResource(String fileName) {
    return loadResource(new File(fileName));
  }

  /** Creates offscreen drawable OpenGL surface at the specified resolution */
  public static GLAutoDrawable createGLSurface(int width, int height) {
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

    return factory.createOffscreenAutoDrawable(
        factory.getDefaultDevice(),
        glCapabilities,
        new DefaultGLCapabilitiesChooser(),
        width,
        height);
  }

  public static String getVertexShaderTemplate() {
    return loadResource(FRAMEWORK_PATH + "default.vs");
  }

  public static int createShader(GL4 gl4, int programId, String shaderCode, int shaderType)
      throws Exception {
    int shaderId = gl4.glCreateShader(shaderType);
    if (shaderId == 0) {
      throw new Exception("Error creating shader. Shader id is zero.");
    }
    gl4.glShaderSource(shaderId, 1, new String[] {shaderCode}, null);
    gl4.glCompileShader(shaderId);
    validateStatus(gl4, shaderId, GL4.GL_COMPILE_STATUS);
    gl4.glAttachShader(programId, shaderId);
    return shaderId;
  }

  /**
   * Takes shader filename with extension, returns string containing path to corresponding cache
   * file.
   */
  public static String getCacheFilename(String shaderName) {
    // strip the incoming path down to just the filename, and build
    // the cache file path from there.
    String shaderFile = shaderName.substring(shaderName.lastIndexOf('/') + 1);
    String[] parts = shaderFile.split("\\.");

    return String.format(CACHE_PATH + "%s.bin", parts[0]);
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
   * True if we need to recompile this shader because either the framework or the shader code have
   * been modified since last compile, false otherwise
   *
   * @param shaderFile path to the shader's glsl file
   * @return true if recompile needed, false otherwise
   */
  public static boolean needsRecompile(String shaderFile) {
    String cacheFile = ShaderUtils.getCacheFilename(shaderFile);

    if (isNewerThan(FRAMEWORK_PATH + "default.vs", cacheFile)) {
      // TE.log("Vertex shader framework has been modified.");
      return true;
    }

    if (isNewerThan(FRAMEWORK_PATH + "template.fs", cacheFile)) {
      // TE.err("Fragment shader framework been modified.");
      return true;
    }

    if (isNewerThan(SHADER_PATH + shaderFile, cacheFile)) {
      // TE.log("Shader '%s` has been modified.",shaderFile);
      return true;
    }

    // if here, no recompile needed.
    return false;
  }

  /**
   * Attempts to read the named shader from resources/shaders/cache. Returns a ByteBuffer full of
   * shader binary if successful, null otherwise.
   */
  public static boolean loadShaderFromCache(GL4 gl4, int programId, String shaderName) {

    // account for shadertoy shaders pulled in via URL
    if (shaderName == null) return false;

    // see if the shader or the framework have changed since last recompile
    if (needsRecompile(shaderName)) {
      return false;
    }

    // attempt to read shader binary from cache file
    String cacheFile = getCacheFilename(shaderName);

    try {
      // get available binary formats for shader storage
      int[] fmtCount = new int[1];
      gl4.glGetIntegerv(GL4.GL_NUM_PROGRAM_BINARY_FORMATS, fmtCount, 0);

      // fast out if no binary formats are available (e.g. on MacOS)
      if (fmtCount[0] < 1) {
        // TE.log("Shader cache: No compatible binary shader format available.");
        return false;
      }

      // take the first available format
      int[] formatList = new int[fmtCount[0]];
      gl4.glGetIntegerv(GL4.GL_PROGRAM_BINARY_FORMATS, formatList, 0);

      byte[] outBuf = Files.readAllBytes(Path.of(cacheFile));
      ByteBuffer shader = ByteBuffer.wrap(outBuf);

      // attach binary to our shader program
      gl4.glProgramBinary(programId, formatList[0], shader, outBuf.length);
    } catch (IOException e) {
      TE.log("I/O Exception loading shader '%s'.", shaderName);
      return false;
    }

    // TE.log("Loaded from cache: '%s'", shaderName);
    return true;
  }

  /** Save compiled and linked shader binary in a cache file, in resources/shaders/cache */
  public static void saveShaderToCache(GL4 gl4, String shaderName, int programId) {

    // account for shadertoy shaders pulled in via URL
    if (shaderName == null) return;

    // get the size of the shader binary in bytes
    int[] len = new int[1];
    gl4.glGetProgramiv(programId, GL4.GL_PROGRAM_BINARY_LENGTH, len, 0);

    // get available binary formats for shader storage
    int[] fmtCount = new int[1];
    gl4.glGetIntegerv(GL4.GL_NUM_PROGRAM_BINARY_FORMATS, fmtCount, 0);

    if (fmtCount[0] < 1) {
      // TE.log("Shader cache: No compatible binary shader format available.");
      return;
    }

    // take the first available format
    int[] formatList = new int[fmtCount[0]];
    gl4.glGetIntegerv(GL4.GL_PROGRAM_BINARY_FORMATS, formatList, 0);

    // get the binary from OpenGL and store it in our buffer
    ByteBuffer shader = GLBuffers.newDirectByteBuffer(len[0]);
    gl4.glGetProgramBinary(programId, len[0], len, 0, formatList, 0, shader);

    // save the shader binary to the cache file
    try (FileChannel fileChannel =
        new FileOutputStream(ShaderUtils.getCacheFilename(shaderName)).getChannel()) {
      fileChannel.write(shader);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Preprocess, compile and link vertex shader template, fragment shader template and pattern
   * shader code into a binary object, attach it to the specified OpenGL programId, and save it to
   * the shader cache.
   *
   * @param gl4 an active OpenGL context
   * @param programId id to which the shader binary will be attached
   * @param shaderName filename (without path) of fragment shader
   * @param tePreProcess whether to perform te pre-processing and merge with template shader
   */
  public static void buildShader(GL4 gl4, int programId, String shaderName, boolean tePreProcess) {
    ArrayList<ShaderConfiguration> config = new ArrayList<>();
    String cacheName = getCacheFilename(shaderName);
    String shaderText = loadResource(SHADER_PATH + shaderName);

    String shaderBody;
    try {
      if (tePreProcess) {
        GLPreprocessor glp = new GLPreprocessor();
        // try the new way
        try {
          shaderBody = glp.preprocessShader(shaderText, config);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      } else {
        shaderBody = shaderText;
      }

      int vertexShaderId =
          createShader(gl4, programId, getVertexShaderTemplate(), GL4.GL_VERTEX_SHADER);
      int fragmentShaderId = createShader(gl4, programId, shaderBody, GL4.GL_FRAGMENT_SHADER);
      link(gl4, programId);

      // free native resources after link
      gl4.glDetachShader(programId, fragmentShaderId);
      gl4.glDetachShader(programId, vertexShaderId);
      gl4.glDeleteShader(fragmentShaderId);
      gl4.glDeleteShader(vertexShaderId);

      // and save the complete program object to file
      saveShaderToCache(gl4, cacheName, programId);
    } catch (Exception e) {
      TE.error("Error building shader " + shaderName + "\n" + e.getMessage());
    }
  }

  public static void link(GL4 gl4, int programId) {
    gl4.glLinkProgram(programId);
    validateStatus(gl4, programId, GL4.GL_LINK_STATUS);

    gl4.glValidateProgram(programId);
    validateStatus(gl4, programId, GL4.GL_VALIDATE_STATUS);
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
