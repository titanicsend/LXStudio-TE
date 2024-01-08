package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Combo FG")
public class FollowThatStar extends GLShaderPattern {
  GLShader shader;

  public FollowThatStar(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls
        .setRange(TEControlTag.QUANTITY, 5, 1, 10)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

    controls.setRange(TEControlTag.SIZE, 1.75, 1.0, 5);
    controls.setRange(TEControlTag.WOW2, 1, 1, 4);

    // register common controls with LX
    addCommonControls();

    shader = new GLShader(lx, "followthatstar.fs", this);
    addShader(shader, setup);
  }

  ShaderSetup setup =
    new ShaderSetup() {
    @Override
    public void setUniforms(GLShader s) {
      s.setUniform("iQuantity", (float)  getQuantity());
      s.setUniform("iSize", (float) getSize());
      s.setUniform("iWow2", (float) getWow2());
    }
  };
  
  @Override
  public void runTEAudioPattern(double deltaMs) {

    shader.setUniform("iRotationAngle", (float) -getRotationAngleFromSpin());
    shader.setUniform("iTime", (float) -getTime());

    // run the shader
    shader.run(deltaMs);
  }

  @Override
  // THIS IS REQUIRED if you're not using ConstructedPattern!
  // Initialize the NativeShaderPatternEffect and retrieve the native shader object
  // from it when the pattern becomes active
  public void onActive() {
    super.onActive();
    shader.onActive();
  }

  @Override
  public void onInactive() {
    super.onInactive();
    shader.onInactive();
  }
}
