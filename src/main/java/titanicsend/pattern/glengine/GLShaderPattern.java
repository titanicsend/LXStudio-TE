package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import java.util.ArrayList;

public class GLShaderPattern extends TEPerformancePattern {

  public interface ShaderSetup {
   void setUniforms(GLShader shader);
  }
  protected class ShaderInfo {
    protected GLShader shader;
    protected ShaderSetup setUniforms;

    public ShaderInfo(GLShader shader, ShaderSetup setUniforms) {
      this.shader = shader;
      this.setUniforms = setUniforms;
    }
  }

  // list of shaders to run, with associated setup functions
  private final ArrayList<ShaderInfo> shaderInfo = new ArrayList<>();

  public GLShaderPattern(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);
  }

  public GLShaderPattern(LX lx, TEShaderView view) {
    super(lx, view);
  }

  public void addShader(GLShader shader, ShaderSetup setup) {
    shaderInfo.add(new ShaderInfo(shader, setup));
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {

    for (ShaderInfo s : shaderInfo) {
      s.setUniforms.setUniforms(s.shader);
      s.shader.run(deltaMs);
    }
  }

  @Override
  public void onActive() {
    super.onActive();
    for (ShaderInfo s : shaderInfo) {
      s.shader.onActive();
    }

  }

  @Override
  public void onInactive() {
    super.onInactive();
    for (ShaderInfo s : shaderInfo) {
      s.shader.onInactive();
    }
  }
}
