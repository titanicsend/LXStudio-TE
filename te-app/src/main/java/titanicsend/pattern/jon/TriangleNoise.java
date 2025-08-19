package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Noise")
public class TriangleNoise extends DriftEnabledPattern {

  public TriangleNoise(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // common controls setup
    controls.setRange(TEControlTag.SPEED, 0, -4, 4); // overall scale
    controls.setValue(TEControlTag.SPEED, 0.5);

    controls.setRange(TEControlTag.SIZE, 1.15, 2, 0.3);
    controls.setRange(TEControlTag.QUANTITY, 3, 1, 5);
    controls.setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);
    controls.setRange(TEControlTag.SPIN, 0, -0.5, 0.5);
    controls.setRange(TEControlTag.WOW1, 4.4, 3, 6.5);
    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    // register common controls with LX
    addCommonControls();
    addShader(GLShader.config(lx).withFilename("triangle_noise.fs"));
  }
}
