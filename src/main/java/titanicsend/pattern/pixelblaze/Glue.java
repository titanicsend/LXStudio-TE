package titanicsend.pattern.pixelblaze;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import titanicsend.util.TEColor;

import java.util.Random;

public class Glue {
  public static int color;

  public static void reset() {
    color = TEColor.TRANSPARENT;
  }
  public static void hsv(float h, float s, float v) {
    h = h % 1f;
    if (h < 1)
      h += 1f;
    s = Math.max(0, Math.min(1, s));
    v = Math.max(0, Math.min(1, v));
    color = LXColor.hsb(360.0f * (h % 1), 100.0f * s,
            100.0f * v);
  }

  public static void rgb(float r, float g, float b) {
    r = Math.max(0, Math.min(1, r));
    g = Math.max(0, Math.min(1, g));
    b = Math.max(0, Math.min(1, b));
    color = LXColor.rgb((int) (r*255), (int) (g*255), (int) (b*255));
  }
}