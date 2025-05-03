package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Panel FG")
public class SpiralDiamonds extends GLShaderPattern {

  public SpiralDiamonds(LX lx) {
    super(lx, TEShaderView.DOUBLE_LARGE);

    controls.setRange(TEControlTag.FREQREACTIVITY, 0.25, 0, 0.4);
    controls.setValue(TEControlTag.SPEED, 0.25);
    controls.setRange(TEControlTag.SIZE, 1.0, 0.2, 10.0);

    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));
    // Quantity controls density of diamonds
    controls
        .setRange(TEControlTag.QUANTITY, 4, 1, 7)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

    addCommonControls();

    addShader(
        "spiral_diamonds.fs",
        (s) -> {
          s.setUniform("speedAngle", (float) getRotationAngleFromSpeed());
        });
  }
}
