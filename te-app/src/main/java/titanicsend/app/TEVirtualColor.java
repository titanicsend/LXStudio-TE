package titanicsend.app;

import heronarts.lx.color.LXColor;

public class TEVirtualColor {
  public int rgb;
  public float alpha;

  public TEVirtualColor(int rgb, float alpha) {
    this.rgb = rgb;
    this.alpha = alpha;
  }

  public TEVirtualColor(int r, int g, int b, float alpha) {
    this(LXColor.rgb(r, g, b), alpha);
  }
}
