package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.*;

public class OffscreenShaderRenderer {

    private final static int xResolution = 640;
    private final static int yResolution = 480;

    private final NativeShader nativeShader;
    private final GLAutoDrawable offscreenDrawable;

    public OffscreenShaderRenderer(FragmentShader fragmentShader) {
        GLProfile glProfile = GLProfile.getGL4ES3();
        GLCapabilities glCapabilities = new GLCapabilities(glProfile);
        glCapabilities.setHardwareAccelerated(true);
        glCapabilities.setOnscreen(false);
        glCapabilities.setDoubleBuffered(false);
        glCapabilities.setAlphaBits(8);
        glCapabilities.setRedBits(8);
        glCapabilities.setBlueBits(8);
        glCapabilities.setGreenBits(8);
        GLDrawableFactory factory = GLDrawableFactory.getFactory(glProfile);

        //need to specifically create an offscreen drawable
        //there is no way to have a normal drawable render on a panel/canvas which is not visible
        offscreenDrawable = factory.createOffscreenAutoDrawable(factory.getDefaultDevice(), glCapabilities,
                new DefaultGLCapabilitiesChooser(), xResolution, yResolution);
        nativeShader = new NativeShader(fragmentShader, xResolution, yResolution);
        offscreenDrawable.display();

    }

    public void useAlphaChannel(boolean b) {
        nativeShader.useAlphaChannel(b);
    }

    public int[][] getFrame(AudioInfo audioInfo) {
        //lazy initialize
        //if this get called too early on it will disrupt lx's gl initialization
        if (!nativeShader.isInitialized()) {
            offscreenDrawable.getContext().makeCurrent();
            nativeShader.init(offscreenDrawable);
        }

        nativeShader.updateAudioInfo(audioInfo);
        nativeShader.display(offscreenDrawable);
        return nativeShader.getSnapshot();
    }

    public void reset() {
        nativeShader.reset();
    }

}
