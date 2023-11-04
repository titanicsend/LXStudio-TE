package titanicsend.pattern.glengine;

import com.jogamp.opengl.*;

import java.io.File;

import titanicsend.pattern.yoffa.shader_engine.*;
import titanicsend.util.TE;

/**
 * Checks shader cache and builds binaries for any changed or previously
 * uncached shaders.
 */
public class ShaderPrecompiler {
    private final static int xResolution = OffscreenShaderRenderer.getXResolution();
    private final static int yResolution = OffscreenShaderRenderer.getYResolution();

    /**
     * Create and save binary versions of any shaders that need it.
     */
    public static void rebuildCache() {
        long timer = System.currentTimeMillis();
        int totalFiles = 0;
        int compiledFiles = 0;

        TE.log("Refreshing shader cache...");

        // save the currently active GL context
        GLContext prevContext = GLContext.getCurrent();

        // set up just enough OpenGL machinery to let us compile and link
        GLAutoDrawable surface = ShaderUtils.createGLSurface(xResolution, yResolution);
        surface.display();
        surface.getContext().makeCurrent();

        GL4 gl4 = surface.getGL().getGL4();
        int programId = gl4.glCreateProgram();

        // enumerate all files in the shader directory and
        // attempt to compile them to cached .bin files.
        File dir = new File(ShaderUtils.SHADER_PATH);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File file : directoryListing) {
                if (file.getName().endsWith(".fs")) {
                    totalFiles++;

                    if (ShaderUtils.needsRecompile(file.getName())) {
                        ShaderUtils.buildShader(gl4, programId, file.getName());
                        compiledFiles++;
                    }
                }
            }
        }

        // free native resources and restore the previous GL context
        gl4.glDeleteProgram(programId);
        surface.getContext().release();

        if (prevContext != null) prevContext.makeCurrent();

        TE.log("%d shaders processed in %d ms.", totalFiles, System.currentTimeMillis() - timer);
        //TE.log("%d cache file%s updated.", compiledFiles, (compiledFiles == 1) ? "" : "s");
    }
}
