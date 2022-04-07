package titanicsend.pattern.pixelblaze;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEPattern;

import javax.script.Invocable;

public class PixelBlazePattern extends TEPattern {
  private Invocable invocable = null;
//  private static String PB_CLASS = "firework dust";
  private static String PB_CLASS = "fireflies";

  public PixelBlazePattern(LX lx) {
    super(lx);
    try {
      this.invocable = Wrapper.makeInvocable(PB_CLASS);
    } catch (Exception e) {
      LX.error(e);
    }
  }

  public void run(double deltaMs) {
    if (this.invocable == null) return;
    try {
      this.invocable.invokeFunction("teInit", this.model.points.length);
      this.invocable.invokeFunction("beforeRender", deltaMs);
    } catch (Exception e) {
      LX.error(e);
      return;
    }

    for (LXPoint point : this.model.points) {
      try {
        this.invocable.invokeFunction("render", point.index);
      } catch (Exception e) {
        LX.error(e);
        return;
      }
      int rgb = LXColor.hsb(360.0 * Glue.hue, 100.0 * Glue.saturation,
              100.0 * Glue.brightness);
      colors[point.index] = rgb;
    }
  }
}