package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.opengl.*;
import java.nio.ByteBuffer;

public class OffscreenShaderRenderer {
  private static final int xResolution = 640;
  private static final int yResolution = 480;

  private NativeShader nativeShader;
  private GLAutoDrawable offscreenDrawable = null;

  public OffscreenShaderRenderer(FragmentShader fragmentShader) {
    nativeShader = new NativeShader(fragmentShader, xResolution, yResolution);
  }

  public void initializeNativeShader() {
    // save the currently active GL context in case we're on a thread
    // that's trying to draw something on the UI
    GLContext prevContext = GLContext.getCurrent();

    offscreenDrawable = ShaderUtils.createGLSurface(xResolution, yResolution);
    offscreenDrawable.display();
    nativeShader.init(offscreenDrawable);

    // restore previous context
    if (prevContext != null) prevContext.makeCurrent();
  }

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

  public NativeShader getNativeShader() {
    return nativeShader;
  }

  public static int getXResolution() {
    return xResolution;
  }

  public static int getYResolution() {
    return yResolution;
  }
}
