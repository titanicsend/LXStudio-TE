package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.util.List;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Native Shaders")
public class TEAutoShaderPattern extends GLShaderPattern {

  protected TEAutoShaderPattern(LX lx) {
    this(lx, null);
  }

  protected TEAutoShaderPattern(LX lx, TEShaderView defaultView) {
    super(lx, defaultView);

    // Create shader instance
    GLShader shader = new GLShader(lx, this.getShaderFile(), this.getControlData());

    // use common control configuration data from shader to set control defaults,
    // then register controls with Chromatik.
    List<ShaderConfiguration> shaderConfig = shader.getShaderConfig();
    configureCommonControls(shaderConfig);
    addCommonControls();

    addShader(shader);
  }

  // Returns the shader file path. This is intended to be overridden when we create our
  // dynamic runtime class
  protected String getShaderFile() {
    return "";
  }
}
