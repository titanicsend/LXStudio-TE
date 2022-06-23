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
        GLDrawableFactory factory = GLDrawableFactory.getFactory(glProfile);

        //need to specifically create an offscreen drawable
        //there is no way to have a normal drawable render on a panel/canvas which is not visible
        offscreenDrawable = factory.createOffscreenAutoDrawable(factory.getDefaultDevice(), glCapabilities,
                new DefaultGLCapabilitiesChooser(), xResolution, yResolution);
        offscreenDrawable.display();
        nativeShader = new NativeShader(fragmentShader, xResolution, yResolution);

    }

    public int[][] getFrame() {
        //lazy initialize
        //if this get called too early on it will disrupt lx's gl initialization
        if (!nativeShader.isInitialized()) {
            offscreenDrawable.getContext().makeCurrent();
            nativeShader.init(offscreenDrawable);
        }

        nativeShader.display(offscreenDrawable);
        return nativeShader.getSnapshot();
    }

    public void reset() {
        nativeShader.reset();
    }

}
