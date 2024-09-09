package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.ndi.NDIPattern;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.yoffa.framework.TEShaderView;
/**
 * Test class for NDIReceiver: displays video frames, and adds a second shader for
 * a simple edge detection effect (controlled by WOW2)
 */
@LXCategory("NDI")
public class NDIReceiverTest extends NDIPattern {

  public NDIReceiverTest(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // register common controls with LX
    addCommonControls();

    // TODO - DSP effect temporarily disabled 'till we get mappedBuffer working in shader patterns
    // add test edge detection effect shader
    //addShader(new GLShader(lx, "sobel_filter_effect.fs", getControlData(), getImageBuffer()));
  }
}
