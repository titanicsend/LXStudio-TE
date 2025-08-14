package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.nio.ByteBuffer;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.glengine.TEShader;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("TE Examples")
public class MultipassDemo extends GLShaderPattern {
  ByteBuffer buffer;

  // simple demo of multipass rendering
  public MultipassDemo(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);
    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));

    // register common controls with LX
    addCommonControls();

    // allocate a backbuffer for all the shaders to share
    buffer = TEShader.allocateBackBuffer();

    // add the first shader, passing in the shared backbuffer
    addShader(GLShader.config(lx).withFilename("fire.fs").withLegacyBackBuffer(buffer));

    // add the second shader, which applies a simple edge detection filter to the
    // output of the first shader
    addShader(
        GLShader.config(lx).withFilename("sobel_filter_effect.fs").withLegacyBackBuffer(buffer));
  }

  // additional shaders can be added in the same way. They will be run in the order
  // they are added, and each will be passed the output of the previous shader.
}
