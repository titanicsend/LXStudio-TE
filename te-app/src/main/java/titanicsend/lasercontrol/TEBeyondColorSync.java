package titanicsend.lasercontrol;

import heronarts.lx.LX;
import heronarts.lx.LXLoopTask;
import heronarts.lx.color.ColorParameter;
import studio.jkb.beyond.parameter.BeyondCompoundParameter;
import titanicsend.color.TEColorParameter;

/**
 * Wraps a color parameter and sends Hue Shift + Saturation to Beyond in a TE-specific manner
 * detailed by Jeff in a handwritten document on a late August night early in the 21st century.
 */
public class TEBeyondColorSync implements LXLoopTask {

  private static final String HUE_OSC_BEYOND_PATH = "/b/Channels/15/Value";
  private static final float HUE_VALUE_DEFAULT = 0;
  private static final float HUE_VALUE_0 = 0;
  private static final float HUE_VALUE_1 = 1;

  private static final String SAT_OSC_BEYOND_PATH = "/b/Channels/16/Value";
  private static final float SAT_VALUE_DEFAULT = 1;
  private static final float SAT_VALUE_0 = 1;
  private static final float SAT_VALUE_1 = 0;

  // Scale values to convert TE color parameter values to normalized
  private static final float LX_HUE_RANGE = 360f;
  private static final float LX_SATURATION_RANGE = 100f;

  private final LX lx;

  private final BeyondCompoundParameter hue;
  private final BeyondCompoundParameter saturation;

  private TEColorParameter colorParameter = null;

  public TEBeyondColorSync(LX lx) {
    this(lx, null);
  }

  public TEBeyondColorSync(LX lx, TEColorParameter colorParameter) {
    this.lx = lx;
    this.colorParameter = colorParameter;

    this.hue =
        new BeyondCompoundParameter(
            lx, "Hue", HUE_OSC_BEYOND_PATH, HUE_VALUE_DEFAULT, HUE_VALUE_0, HUE_VALUE_1);
    this.saturation =
        new BeyondCompoundParameter(
            lx, "Saturation", SAT_OSC_BEYOND_PATH, SAT_VALUE_DEFAULT, SAT_VALUE_0, SAT_VALUE_1);

    this.lx.engine.addLoopTask(this);
  }

  public TEBeyondColorSync setColorParameter(TEColorParameter colorParameter) {
    this.colorParameter = colorParameter;
    return this;
  }

  public ColorParameter getColorParameter() {
    return this.colorParameter;
  }

  public TEBeyondColorSync setOutputEnabled(boolean outputEnabled) {
    this.hue.setOutputEnabled(outputEnabled);
    this.saturation.setOutputEnabled(outputEnabled);
    return this;
  }

  @Override
  public void loop(double deltaMs) {
    if (this.colorParameter == null) {
      return;
    }

    float h = this.colorParameter.calcHuef();
    float s = this.colorParameter.calcSaturationf();

    this.hue.setNormalized(h / LX_HUE_RANGE);
    this.saturation.setNormalized(s / LX_SATURATION_RANGE);
  }

  public void dispose() {
    this.lx.engine.removeLoopTask(this);
  }
}
