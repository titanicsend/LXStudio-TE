package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderEffect;

@LXCategory("Titanics End")
public class ExplodeEffect extends GLShaderEffect {
  public float amount;

  public ExplodeEffect(LX lx) {
    super(lx);

    // add the first shader, passing in the effect's backbuffer
    GLShader shader = new GLShader(lx, "red.fs", controlData, getDefaultImageBuffer());
    addShader(
        shader,
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader shader) {
            // set the shader's uniform "iAmount" to the effect's amount
            shader.setUniform("iAmount", amount);
          }
        });
  }
}
