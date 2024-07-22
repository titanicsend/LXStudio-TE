package titanicsend.pattern.glengine;

import com.jogamp.opengl.*;
import java.io.File;
import titanicsend.pattern.yoffa.shader_engine.*;
import titanicsend.util.TE;

/** Checks shader cache and builds binaries for any changed or previously uncached shaders. */
public class ShaderPrecompiler {
  private static final int xResolution = OffscreenShaderRenderer.getXResolution();
  private static final int yResolution = OffscreenShaderRenderer.getYResolution();

  /** Create and save binary versions of any shaders that need it. */
  public static void rebuildCache() {
    long timer = System.currentTimeMillis();
    int totalFiles = 0;
    int compiledFiles = 0;

    //TE.log("Refreshing shader cache...");

    // save the currently active GL context
    GLContext prevContext = GLContext.getCurrent();

    // set up just enough OpenGL machinery to let us compile and link
    GLAutoDrawable surface = ShaderUtils.createGLSurface(xResolution, yResolution);
    surface.display();
    surface.getContext().makeCurrent();

    /********************************************
     * 6/2024
     * Disabling the shader precompiler for now because
     * Apple has chosen not to support OpenGL binary shader file
     * formats in MacOS Sonoma.  We can retest periodically, but
     * performance shouldn't be affected -- the shader compiler
     * is very fast and still only needs to be run once per loaded
     * shader.  And app startup time will actually be slightly faster!
     *
     * ShaderUtils.rebuildCache(surface) will still be called from TEApp,
     * but now it will run only the minimum code necessary to initialize
     * our OpenGL setup for the first time.
     ********************************************/
    /*
    GL4 gl4 = surface.getGL().getGL4();
    int programId = gl4.glCreateProgram();

    // Get a list of all shaders in resource path
    // and try to compile them to .bin files in the
    // cache directory. (NB: This directory listing
    // method is significantly faster than the old one.)


    File[] shaderFiles =
        new File(ShaderUtils.SHADER_PATH).listFiles((dir, name) -> name.endsWith(".fs"));
    if (shaderFiles == null) {
      // TE.log("No shaders found in " + dir);
      return;
    }


    for (File file : shaderFiles) {
      if (file.getName().endsWith(".fs")) {
        totalFiles++;

        if (ShaderUtils.needsRecompile(file.getName())) {
          ShaderUtils.buildShader(gl4, programId, file.getName());
          compiledFiles++;
        }
      }
    }

    // free native resources and restore the previous GL context
    gl4.glDeleteProgram(programId);
    */

    surface.getContext().release();

    if (prevContext != null) prevContext.makeCurrent();

    //TE.log("%d shaders processed in %d ms.", totalFiles, System.currentTimeMillis() - timer);
    // TE.log("%d cache file%s updated.", compiledFiles, (compiledFiles == 1) ? "" : "s");
  }
}
