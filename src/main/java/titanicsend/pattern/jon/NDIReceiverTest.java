package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.ndi.NDIPattern;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("AAAardvark")
/**
 * Test class for NDIReceiver: displays video frames, and adds a simple edge detection effect,
 * controlled by WOW2
 */
public class NDIReceiverTest extends NDIPattern {

  public NDIReceiverTest(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // add test edge detection effect shader
    addShader(new GLShader(lx, "sobel.fs", this, buffer));
  }
}
