package titanicsend.pattern.glengine;

import com.jogamp.opengl.GLAutoDrawable;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import titanicsend.pattern.yoffa.shader_engine.ShaderUtils;
import titanicsend.util.TE;

public class GLEngine extends LXComponent implements LXLoopTask {
    public final static String PATH = "GLEngine";
    private final static int xSize = 640;
    private final static int ySize = 480;
    private GLAutoDrawable canvas = null;

    public GLAutoDrawable getCanvas() {
        return canvas;
    }
    public static int getWidth() { return xSize; }
    public static int getHeight() { return ySize; }

    protected final LX lx;
    public GLEngine(LX lx) {
        this.lx = lx;

        // register glengine so we can access it from patterns.
        // and add it as an engine task for audio analysis and buffer management
        lx.engine.registerComponent(PATH, this);
        lx.engine.addLoopTask(this);
    }
    public void loop(double deltaMs) {
        // TEST-TEST-TEST
        // Create an offscreen drawable and context on the engine thread.
        // TODO - get rid of the default FBO and let the pattern init code specify render buffers
        //
        // We can get away with a single context assuming that all loop tasks
        // and patterns run on the engine thread.
        //
        // If this is the case, we won't have to put special case code in the
        // pattern initialization handler for the first frame, which will slightly
        // improve performance.
        if (canvas == null) {
            canvas = ShaderUtils.createGLSurface(xSize,ySize);
            canvas.display();
            TE.log("GLEngine: Created main offscreen drawable & context");
        }

        // TODO - handle per-frame audio analysis and texture creation
    }
}
