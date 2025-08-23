package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Noise")
public class TurbulenceLines extends DriftEnabledPattern {

  public TurbulenceLines(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // common controls setup
    controls.setRange(TEControlTag.SPEED, 2, -8, 8);

    controls.setRange(TEControlTag.SIZE, 1, 1.25, 0.4);
    controls.setRange(TEControlTag.QUANTITY, 60, 20, 200);
    controls.setRange(TEControlTag.WOW1, 0.5, 0.2, 1);

    controls.setValue(TEControlTag.XPOS, 0.25);
    controls.setValue(TEControlTag.YPOS, -0.25);

    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    // register common controls with LX
    addCommonControls();
    addShader(GLShader.config(lx).withFilename("turbulent_noise_lines.fs"));
  }
}
