package titanicsend.pattern.ben;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.ConstructedShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Native Shaders Panels")
public class XorceryDiamonds extends ConstructedShaderPattern {
  public XorceryDiamonds(LX lx) {
    super(lx, TEShaderView.ALL_PANELS);
  }

  @Override
  protected void createShader() {
    addShader("xorcery_diamonds.fs");
  }
}