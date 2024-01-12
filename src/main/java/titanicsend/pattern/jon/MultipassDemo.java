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

@LXCategory("Combo FG")
public class MultipassDemo extends GLShaderPattern {
  ByteBuffer buffer;
  GLShader shader;

  public MultipassDemo(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls
        .setRange(TEControlTag.QUANTITY, 5, 1, 10)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

    controls.setRange(TEControlTag.SIZE, 1.75, 1.0, 5);
    controls.setRange(TEControlTag.WOW2, 1, 1, 4);

    // register common controls with LX
    addCommonControls();

    buffer = GLShader.allocateBackBuffer();

    // add the shader and its frame-time setup function
    shader = new GLShader(lx, "followthatstar.fs", this,buffer);

    addShader(
        shader,
        new GLShaderFrameSetup() {
          @Override
          public void setUniforms(GLShader s) {
            s.setUniform("iQuantity", (float) getQuantity());
            s.setUniform("iSize", (float) getSize());
            s.setUniform("iWow2", (float) getWow2());
            //System.out.println(String.format("#%06X", (0xFFFFFF & buffer.getInt(0))));
          }
        });

    shader = new GLShader(lx, "multipass1.fs", this, buffer,"gray_noise.png");

    addShader(
      shader,
      new GLShaderFrameSetup() {
        @Override
        public void setUniforms(GLShader s) {
          //System.out.println(String.format("#%06X", (0xFFFFFF & buffer.getInt(1))));
        }
      });

  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    super.runTEAudioPattern(deltaMs);
  }
}
