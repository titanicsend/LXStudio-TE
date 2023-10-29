package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.*;

import java.nio.ByteBuffer;

public class OffscreenShaderRenderer {

    private final static int xResolution = 640;
    private final static int yResolution = 480;

    private NativeShader nativeShader;
    private GLAutoDrawable offscreenDrawable = null;

    public OffscreenShaderRenderer(FragmentShader fragmentShader) {
        nativeShader = new NativeShader(fragmentShader, xResolution, yResolution);
    }

    public void initializeNativeShader() {
        // save the currently active GL context in case we're on a thread
        // that's trying to draw something on the UI
        GLContext prevContext = GLContext.getCurrent();


        // how many contexts can one man have, before he has too many contexts?
        for (int i = 0; i < 1000; i++) {
            if (0 == i % 100) {
                System.out.println("Contexts created " + i);
            }
            offscreenDrawable = ShaderUtils.createGLSurface(xResolution, yResolution);
            offscreenDrawable.display();
        }

        nativeShader.init(offscreenDrawable);

        // restore previous context
        if (prevContext != null) prevContext.makeCurrent();
    };

    public ByteBuffer getFrame(PatternControlData ctlInfo) {
        if (!nativeShader.isInitialized()) {
            initializeNativeShader();
        }

        nativeShader.updateControlInfo(ctlInfo);
        nativeShader.display(offscreenDrawable);
        return nativeShader.getSnapshot();
    }

    public void reset() {
        nativeShader.reset();
    }

    public NativeShader getNativeShader() { return nativeShader; }

    public static int getXResolution() { return xResolution; }
    public static int getYResolution() { return yResolution; }

}
