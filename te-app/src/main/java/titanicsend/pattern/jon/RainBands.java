package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BoundedParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Noise")
public class RainBands extends DriftEnabledPattern {

  public RainBands(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.setRange(TEControlTag.SPEED, 0, -4, 4);
    controls.setValue(TEControlTag.SPEED, 0.5);

    controls.setRange(TEControlTag.QUANTITY, 0.15, 0.01, 1.6); // field density

    controls.setRange(TEControlTag.SIZE, 1, 2, 0.01);
    controls.setNormalizationCurve(TEControlTag.SIZE, BoundedParameter.NormalizationCurve.REVERSE);
    controls.setExponent(TEControlTag.SIZE, 3);

    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    controls.setRange(TEControlTag.WOW1, 1.5, 8, 0.01); // relative y scale
    // register common controls with LX
    addCommonControls();

    addShader(GLShader.config(lx).withFilename("rain_noise.fs"));
  }
}
