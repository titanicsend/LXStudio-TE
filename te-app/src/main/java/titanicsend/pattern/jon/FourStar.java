package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Native Shaders Panels")
public class FourStar extends GLShaderPattern {
  // must match the value in fourstar.fs
  private static final float MAX_RAYS = 8.0f;

  public FourStar(LX lx) {
    super(lx, TEShaderView.ALL_PANELS);

    controls.setRange(TEControlTag.LEVELREACTIVITY, 0.15, 0.0, 1.0); // beat reactivity
    controls.setRange(TEControlTag.SIZE, 0.5, 1.0, 0.0); // brightness & size of the star
    controls.setRange(TEControlTag.QUANTITY, 5.0, 2.0, MAX_RAYS);
    controls.setRange(TEControlTag.WOW1, 0.5, 0.0, 0.6); // diffraction effect
    controls.setRange(TEControlTag.SPEED, .25, -4.0, 4.0);
    controls.setRange(TEControlTag.SPIN,2.0 ,-4.0, 4.0);

    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));



    addCommonControls();
    addShader("fourstar.fs");
  }
}
