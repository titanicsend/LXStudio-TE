package titanicsend.pattern.ben;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.ConstructedShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Native Shaders Panels")
public class XorceryDiamonds extends ConstructedShaderPattern {
  public XorceryDiamonds(LX lx) {
    super(lx, TEShaderView.ALL_PANELS);
  }

  @Override
  protected void createShader() {

    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));
    controls.setRange(TEControlTag.SIZE, 2.5, 7.0, 0.1);

    addShader("xorcery_diamonds.fs");
  }
}
