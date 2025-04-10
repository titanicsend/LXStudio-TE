package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Combo FG")
public class Fireflies extends GLShaderPattern {

  public Fireflies(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // quantity controls number of fireflies
    controls
        .setRange(TEControlTag.QUANTITY, 20, 1, 32)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

    // size controls size of flies
    controls.setRange(TEControlTag.SIZE, 0.1, 0.05, 0.275);

    // Wow1 controls tail length
    controls.setRange(TEControlTag.WOW1, 0.5, 1.25, 0.15);

    // Wow2 controls color mix
    controls.setRange(TEControlTag.WOW2, 1.0, 1.0, 0.0);

    // disable unused controls
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    // register common controls with LX
    addCommonControls();
    addShader("fireflies.fs");
  }
}
