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

    controls
        .setRange(TEControlTag.QUANTITY, 5, 1, 10)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

    controls.setRange(TEControlTag.SIZE, 1.75, 1.0, 5);
    controls.setRange(TEControlTag.WOW2, 1, 1, 4);

    // register common controls with LX
    addCommonControls();

    // allocate a backbuffer for all the shaders to share
    buffer = GLShader.allocateBackBuffer();

    // add the first shader
    shader = new GLShader(lx, "followthatstar.fs", this,buffer);
    addShader(shader);

    // add the second shader, which will invert the colors from the previous shader
    shader = new GLShader(lx, "multipass1.fs", this, buffer);
    addShader(shader );

    // we can add still more shaders, for example, another instance of multipass1 to re-invert the colors
    // but you get the idea...

  }

}
