package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.*;

public class OffscreenShaderRenderer {

    private final static int xResolution = 640;
    private final static int yResolution = 480;

    private final NativeShader nativeShader;
    private final GLAutoDrawable offscreenDrawable;

    public OffscreenShaderRenderer(FragmentShader fragmentShader,ShaderOptions shaderOptions) {
        offscreenDrawable = ShaderUtils.createGLSurface(xResolution,yResolution);
        offscreenDrawable.display();
        nativeShader = new NativeShader(fragmentShader, xResolution, yResolution,shaderOptions);
    }

    // use default shader options
    public OffscreenShaderRenderer(FragmentShader fragmentShader) {
        this(fragmentShader,new ShaderOptions());
    }

    public void initializeNativeShader() {
       // do nothing for now.
    };

    public int[][] getFrame(AudioInfo audioInfo) {
        // initialize as late as possible to avoid stepping on LX's toes
        if (!nativeShader.isInitialized()) {
            nativeShader.init(offscreenDrawable);
        }

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
