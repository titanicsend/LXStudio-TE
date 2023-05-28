package titanicsend.pattern.jon;

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

    private final static String SHADER_PATH = "resources/shaders";

    private static final String SHADER_BODY_PLACEHOLDER = "{{%shader_body%}}";

     /**
     * Create and save binary versions of any shaders that need it.
     *
     */
    public static void rebuildCache() {
        long timer = System.currentTimeMillis();
        int totalFiles = 0;
        int compiledFiles = 0;

        TE.log("Refreshing shader cache...");

        String FRAGMENT_SHADER_TEMPLATE =
                ShaderUtils.loadResource("resources/shaders/framework/template.fs");

        String VERTEX_SHADER_TEMPLATE = ShaderUtils.loadResource("resources/shaders/framework/default.vs");

        // set up just enough OpenGL machinery to let us compile and link
        GLAutoDrawable surface = ShaderUtils.createGLSurface(xResolution, yResolution);
        surface.display();
        GL4 gl4 = surface.getGL().getGL4();
        int programId = gl4.glCreateProgram();

        // enumerate all files in the shader directory and
        // attempt to compile them to cached .bin files.
        File dir = new File(SHADER_PATH);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File file: directoryListing) {
                if (file.getName().endsWith(".fs")) {
                    totalFiles++;

                    if (ShaderUtils.needsRecompile(file.getName())) {
                        String shaderName = ShaderUtils.getCacheFilename(file.getName());
                        String shaderText = ShaderUtils.loadResource(file.getPath());
                        String shaderBody = ShaderUtils.preprocessShader(shaderText, null);
                        String shaderCode = FRAGMENT_SHADER_TEMPLATE.replace(SHADER_BODY_PLACEHOLDER, shaderBody);

                        try {
                            //TE.err("Building shader %s",file.getPath());

                            int vertexShaderId = ShaderUtils.createShader(gl4, programId,
                                    VERTEX_SHADER_TEMPLATE, GL4.GL_VERTEX_SHADER);
                            int fragmentShaderId = ShaderUtils.createShader(gl4, programId,
                                    shaderCode, GL4.GL_FRAGMENT_SHADER);
                            ShaderUtils.link(gl4, programId);

                            ShaderUtils.saveShaderToCache(gl4, shaderName, programId);

                            gl4.glDetachShader(programId, fragmentShaderId);
                            gl4.glDetachShader(programId, vertexShaderId);
                            gl4.glDeleteShader(fragmentShaderId);
                            gl4.glDeleteShader(vertexShaderId);
                        } catch (Exception e) {
                            TE.log("Error building shader %s",file.getName());
                            throw new RuntimeException(e);
                        }
                        compiledFiles++;
                    }
                }
            }
        } else {
           // ignore directories and other file system objects
        }
        gl4.glDeleteProgram(programId);

        TE.log("%d shaders processed in %d ms.",totalFiles,System.currentTimeMillis() - timer);
        //TE.log("%d cache file%s updated.",compiledFiles,(compiledFiles == 1) ? "" : "s");
    }
}
