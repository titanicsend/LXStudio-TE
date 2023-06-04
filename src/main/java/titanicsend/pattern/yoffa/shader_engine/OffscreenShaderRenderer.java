package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.*;

import java.nio.ByteBuffer;

public class OffscreenShaderRenderer {

    private final static int xResolution = 640;
    private final static int yResolution = 480;

    private NativeShader nativeShader;
    private GLAutoDrawable offscreenDrawable;

    public OffscreenShaderRenderer(FragmentShader fragmentShader) {
        offscreenDrawable = ShaderUtils.createGLSurface(xResolution,yResolution);
        offscreenDrawable.display();
        nativeShader = new NativeShader(fragmentShader, xResolution, yResolution);

        // load shaders at creation time to make switching instant for
        // the rest of the run.
        nativeShader.init(offscreenDrawable);
    }

    public void initializeNativeShader() {
    };

    public ByteBuffer getFrame(PatternControlData ctlInfo) {
        nativeShader.updateControlInfo(ctlInfo);
        nativeShader.updateAudioInfo(audioInfo);
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
