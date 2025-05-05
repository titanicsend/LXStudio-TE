package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.util.List;
import titanicsend.pattern.jon.DriftEnabledPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Native Shaders")
public class TEAutoDriftPattern extends DriftEnabledPattern {

  protected TEAutoDriftPattern(LX lx) {
    this(lx, null);
  }

  protected TEAutoDriftPattern(LX lx, TEShaderView defaultView) {
    super(lx, defaultView);

    // Create shader instance
    TEShader shader =
        addShader(
            this.getShaderFile(),
            (s) -> {
              // calculate incremental transform based on elapsed time
              s.setUniform("iTranslate", (float) getXPosition(), (float) getYPosition());
            });

    // use common control configuration data from shader to set control defaults,
    // then register controls with Chromatik.
    List<ShaderConfiguration> shaderConfig = shader.getShaderConfig();
    configureCommonControls(shaderConfig);
    addCommonControls();
  }

  // Returns the shader file path. This is intended to be overridden when we create our
  // dynamic runtime class
  protected String getShaderFile() {
    return "";
  }
}
