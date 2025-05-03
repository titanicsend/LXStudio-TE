package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BoundedParameter;
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

    controls.setRange(TEControlTag.WOW1, 1.5, 8, 0.01); // relative y scale
    // register common controls with LX
    addCommonControls();

    addShader("rain_noise.fs", (s) -> {
      // calculate incremental transform based on elapsed time
      s.setUniform("iTranslate", (float) getXPosition(), (float) getYPosition());
    });
  }
}
