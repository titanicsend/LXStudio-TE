package titanicsend.util;

import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.GradientUtils;
import heronarts.lx.color.LXColor;
import java.util.ArrayList;
import java.util.List;

public class TEColor {
  public static final int TRANSPARENT = 0x00000000;

  // From our art direction palette:
  // https://www.figma.com/file/sQnxcoNd9ZPAJnIgjfDbA9/Titanic's-End?node-id=404%3A8792
  public static final int YELLOW = LXColor.hsb(61, 67, 99);
  public static final int ORANGE = LXColor.hsb(40, 74, 96);
  public static final int PINK = LXColor.hsb(323, 75, 89);
  public static final int CYAN = LXColor.hsb(194, 61, 91);
  public static final int BLUE = LXColor.hsb(229, 64, 90);
  public static final int GREEN = LXColor.hsb(80, 67, 98);
  public static final int PURPLE = LXColor.hsb(256, 77, 75);

  // Hues are given in Figma as hue from 0..1, not 0..360
  private static final double[][] GRADIENT_HUES = {
    {.97, .01}, {.97, .8}, {.08, .4}, {.8, .4}, {.2, .01}
  };
  public static final List<GradientUtils.ColorStops> GRADIENTS =
      new ArrayList<>(GRADIENT_HUES.length);

  public TEColor() {
    int i = 0;
    for (double[] hueTuple : GRADIENT_HUES) {
      GradientUtils.ColorStops colorStops = new GradientUtils.ColorStops();
      colorStops.setNumStops(2);
      colorStops.stops[0].set(
          new ColorParameter("G" + i + ".0", LXColor.hsb(hueTuple[0] * 360, 100, 100)));
      colorStops.stops[1].set(
          new ColorParameter("G" + i + ".1", LXColor.hsb(hueTuple[1] * 360, 100, 100)));
      GRADIENTS.add(colorStops);
      i++;
    }
  }

  /**
   * Set the brightness of a color
   *
   * @param color LX color integer
   * @param brightness 0.0 to 1.0
   * @return color at new brightness
   */
  public static int setBrightness(int color, float brightness) {
    float r = (float) (0xff & LXColor.red(color)) * brightness;
    float g = (float) (0xff & LXColor.green(color)) * brightness;
    float b = (float) (0xff & LXColor.blue(color)) * brightness;
    return LXColor.rgb((int) r, (int) g, (int) b);
  }

  public static int reAlpha(int color, int alpha) {
    return ((alpha & 0xff) << LXColor.ALPHA_SHIFT) | (color & LXColor.RGB_MASK);
  }
}
