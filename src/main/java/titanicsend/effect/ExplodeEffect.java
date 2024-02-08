package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BooleanParameter;
import titanicsend.pattern.TEPattern;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderEffect;

import java.nio.ByteBuffer;

@LXCategory("Titanics End")
public class ExplodeEffect extends GLShaderEffect {
  protected ByteBuffer frameBuffer;

  public ExplodeEffect(LX lx) {
    super(lx);

    // add the first shader, passing in the effect's backbuffer
    GLShader shader = new GLShader(lx, "red.fs", controlData, getImageBuffer());
    addShader(shader);
  }
}
