package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Combo FG")
public class FollowThatStar extends GLShaderPattern {

  public FollowThatStar(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls
        .setRange(TEControlTag.QUANTITY, 5, 1, 30)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

    controls.setRange(TEControlTag.SIZE, 2., .5, 5);
    controls.setRange(TEControlTag.SPIN, .333, -3.0, 3.0);
    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    // register common controls with LX
    addCommonControls();

    addShader("followthatstar.fs");
  }
}
