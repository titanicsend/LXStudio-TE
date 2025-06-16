package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import java.util.Map;
import titanicsend.pattern.yoffa.framework.TEShaderView;

public abstract class ConstructedShaderPattern extends GLShaderPattern {

  protected ConstructedShaderPattern(LX lx, TEShaderView defaultView) {
    this(lx, defaultView, null);
  }

  protected ConstructedShaderPattern(
      LX lx, TEShaderView defaultView, Map<String, LXListenableNormalizedParameter> extraParams) {
    super(lx, defaultView, extraParams);

    // create the OpenGL shaders and add them to the pattern
    createShader();

    // initialize common controls - required for all patterns
    addCommonControls();

    // add LX controls for any non-standard parameters found in the shader code
    // (right now, this is mostly for legacy code support, but we may use it
    // for other things in the future)
    for (ShaderInfo s : shaderInfo) {
      for (LXParameter parameter : s.shader.getParameters()) {
        addParameter(parameter.getLabel(), parameter);
      }
    }
  }

  /**
   * Derived classes must implement createShader() to add one or more OpenGL shaders to the pattern.
   * For example, to add a single shader, which at frame generation time, does some calculation and
   * sets custom uniforms:
   *
   * <p>protected void createShader() {
   *
   * <p>// initialize common control values and ranges here.
   * controls.setRange(TEControlTag.QUANTITY, 5, 1, 10) controls.setRange(TEControlTag.SIZE, 1.75,
   * 1.0, 5);
   *
   * <p>// add the shader and its frame-time setup function addShader("fireflies.fs", new
   * GLShaderFrameSetup() { @Override public void OnFrame(GLShader s) { float wtf = (float) (PI /
   * sin(getQuantity() * PI * 10));
   *
   * <p>s.setUniform("iCustomFloat", wtf); s.setUniform("iCustomInt", 42);
   * s.setUniform("iCustomVec3", x, y, z); } }); }
   *
   * <p>Note that you can call addShader() multiple times to add multiple shaders to the pattern.
   * Shaders will run in the order they are added, with the output of each shader available to the
   * next shader in line via the iBackbuffer 2D sampler uniform.
   *
   * <p>Each shader can have its own onFrame() setup function, or you can use the default function
   * (which does nothing) by calling addShader(shader) instead of addShader(shader, setupFunction).
   */
  protected abstract void createShader();
}
