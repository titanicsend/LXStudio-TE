package titanicsend.pattern.jon;

import com.jogamp.opengl.util.GLBuffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import java.nio.ByteBuffer;

import titanicsend.pattern.glengine.GLEngine;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("TE Examples")
public class MultipassDemo extends GLShaderPattern {
  ByteBuffer buffer;
  GLShader shader;

  // simple demo of multipass rendering
  public MultipassDemo(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // register common controls with LX
    addCommonControls();

    // allocate a backbuffer for all the shaders to share
    buffer = GLShader.allocateBackBuffer();

    // add the second shader, which applies a simple edge detection filter to the
    // output of the first shader
    shader = new GLShader(lx, "multipass1.fs", this, buffer);
    addShader(shader );
  }

  // additional shaders can be added in the same way. They will be run in the order
  // they are added, and each will be passed the output of the previous shader.


}
