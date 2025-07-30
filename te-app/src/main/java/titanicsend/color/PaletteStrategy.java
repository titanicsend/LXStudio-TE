package titanicsend.color;

import heronarts.lx.color.LXColor;

/** Mechanisms for deriving a second and third complementary color given a starting color */
public enum PaletteStrategy {
  ANALOGOUS("Analogous") {
    public int getColor2(float h, float s, float b) {
      return LXColor.hsb(h + 30, s, b);
    }

    public int getColor3(float h, float s, float b) {
      return LXColor.hsb(h - 30, s, b);
    }
  },

  GOLDEN_RATIO_CONJUGATE("Golden Ratio Conjugate") {
    private final float paletteOffset = 360f * 0.618f;

    public int getColor2(float h, float s, float b) {
      // color2 will be offset by 0.61803^2 x 360 == 137.5 degrees
      return LXColor.hsb(h + paletteOffset * 0.618f, s, b);
    }

    public int getColor3(float h, float s, float b) {
      // color3 will be offset by 0.61803 x 360 == 227.5 degrees
      return LXColor.hsb(h + paletteOffset, s, b);
    }
  },

  SPLIT_COMPLEMENTARY("Split Complementary") {
    public int getColor2(float h, float s, float b) {
      return LXColor.hsb(h + 150, s, b);
    }

    public int getColor3(float h, float s, float b) {
      return LXColor.hsb(h + 210, s, b);
    }
  },

  MONO("Mono") {
    public int getColor2(float h, float s, float b) {
      return LXColor.hsb(h, s, b);
    }

    public int getColor3(float h, float s, float b) {
      return LXColor.hsb(h, s, b);
    }
  };

  public final String label;

  PaletteStrategy(String label) {
    this.label = label;
  }

  public abstract int getColor2(float h, float s, float b);

  public abstract int getColor3(float h, float s, float b);

  @Override
  public String toString() {
    return this.label;
  }
}
