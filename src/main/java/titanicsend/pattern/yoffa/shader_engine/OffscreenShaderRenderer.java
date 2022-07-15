package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.*;

public class OffscreenShaderRenderer {

    private final static int xResolution = 640;
    private final static int yResolution = 480;

    private final NativeShader nativeShader;
    private final GLAutoDrawable offscreenDrawable;

    public OffscreenShaderRenderer(FragmentShader fragmentShader,ShaderOptions shaderOptions) {
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
        offscreenDrawable = factory.createOffscreenAutoDrawable(factory.getDefaultDevice(), glCapabilities,
                new DefaultGLCapabilitiesChooser(), xResolution, yResolution);
        nativeShader = new NativeShader(fragmentShader, xResolution, yResolution,shaderOptions);
        offscreenDrawable.display();
    }

    // use default shader options
    public OffscreenShaderRenderer(FragmentShader fragmentShader) {
        this(fragmentShader,new ShaderOptions());
    }

    public void initializeNativeShader() {
        if (!nativeShader.isInitialized()) {
            nativeShader.init(offscreenDrawable);
        }
    }

    public int[][] getFrame(AudioInfo audioInfo) {
        nativeShader.updateAudioInfo(audioInfo);
        nativeShader.display(offscreenDrawable);
        return nativeShader.getSnapshot();
    }

    public void reset() {
        nativeShader.reset();
    }

}
