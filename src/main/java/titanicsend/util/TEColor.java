package titanicsend.util;

import heronarts.lx.color.LXColor;

public class TEColor {
  public static final int TRANSPARENT = 0x00000000;

  public static int reAlpha(int color, int alpha) {
    return ((alpha & 0xff) << LXColor.ALPHA_SHIFT) | (color & LXColor.RGB_MASK);
  }
}
