package titanicsend.pattern.pixelblaze;

import heronarts.lx.color.LXColor;

public class Glue {

  public static int hsv(float h, float s, float v) {
    h = h % 1f;
    if (h < 1)
      h += 1f;
    s = Math.max(0, Math.min(1, s));
    v = Math.max(0, Math.min(1, v));
    return LXColor.hsb(360.0f * (h % 1), 100.0f * s,
            100.0f * v);
  }

  public static int rgb(float r, float g, float b) {
    r = Math.max(0, Math.min(1, r));
    g = Math.max(0, Math.min(1, g));
    b = Math.max(0, Math.min(1, b));
    return LXColor.rgb((int) (r*255), (int) (g*255), (int) (b*255));
  }

  public static int rgba(float r, float g, float b, float a) {
    r = Math.max(0, Math.min(1, r));
    g = Math.max(0, Math.min(1, g));
    b = Math.max(0, Math.min(1, b));
    a = Math.max(0, Math.min(1, a));
    return LXColor.rgba((int) (r*255), (int) (g*255), (int) (b*255), (int) (a*255));
  }

  public static int setAlpha(int color, float a) {
    return Math.min(255, (int)(a * 255.0F)) << 24 | (color & LXColor.RGB_MASK);
  }
}