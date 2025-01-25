/**
 * Copyright 2016- Mark C. Slee, Heron Arts LLC
 *
 * <p>This file is part of the LX Studio software library. By using LX, you agree to the terms of
 * the LX Studio Software License and Distribution Agreement, available at: http://lx.studio/license
 *
 * <p>Please note that the LX license is not open-source. The license allows for free,
 * non-commercial use.
 *
 * <p>HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, AND SPECIFICALLY
 * DISCLAIMS ANY WARRANTY OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE,
 * WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 *     <p>JKB: This file is part of the LX Studio library. Including this code in the TE repo is a
 *     temporary measure to provide early access to upcoming features/improvements.
 */
package titanicsend.lx;

import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.color.LXPalette;
import heronarts.lx.color.LXSwatch;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.utils.LXUtils;

public class LXGradientUtils {

  public static class GrayTable {

    private static final int SIZE = 256;

    private final LXNormalizedParameter invert;
    private double previousValue = -1;
    private boolean dirty = true;

    /** Lookup table of gray values */
    public final int[] lut;

    public GrayTable(LXNormalizedParameter invert) {
      this(invert, 0);
    }

    public GrayTable(LXNormalizedParameter invert, int padding) {
      this.invert = invert;
      this.lut = new int[SIZE + padding];
    }

    public void update() {
      double invert = this.invert.getValue();
      if (invert != this.previousValue) {
        this.dirty = true;
        this.previousValue = invert;
      }
      if (this.dirty) {
        for (int i = 0; i < SIZE; ++i) {
          int b = (int) LXUtils.lerp(i, 255.9 - i, invert);
          this.lut[i] = 0xff000000 | (b << 16) | (b << 8) | b;
        }
        for (int i = SIZE; i < this.lut.length; ++i) {
          this.lut[i] = this.lut[SIZE - 1];
        }
        this.dirty = false;
      }
    }

    /**
     * Gets the LUT grayscale color for this brightness value
     *
     * @param b Brightness from 0-100
     * @return Invert table color
     */
    public int get(float b) {
      return this.lut[(int) (2.559f * b)];
    }
  }

  public static class ColorStop {
    private float hue;
    private float saturation;
    private float brightness;
    private int r;
    private int g;
    private int b;

    // values for oklab (actually the intermediate LMS perceptual space mapping,
    // which saves us a matrix multiply on conversions going both ways)
    private float lStar;
    private float aStar;
    private float bStar;

    public void set(ColorParameter color) {
      set(color, 0);
    }

    public void set(ColorParameter color, float hueOffset) {
      set(color, hueOffset, 0, 0);
    }

    public void set(
        ColorParameter color, float hueOffset, float saturationOffset, float brightnessOffset) {
      this.hue = color.hue.getValuef() + hueOffset;
      this.saturation = LXUtils.clampf(color.saturation.getValuef() + saturationOffset, 0, 100);
      this.brightness = LXUtils.clampf(color.brightness.getValuef() + brightnessOffset, 0, 100);
      int col = LXColor.hsb(this.hue, this.saturation, this.brightness);
      setRGB(col);
      setOklab(col);
    }

    public void set(LXDynamicColor color) {
      set(color, 0);
    }

    public void set(LXDynamicColor color, float hueOffset) {
      final int c = color.getColor();
      this.hue = color.getHuef() + hueOffset;
      this.saturation = color.getSaturation();
      this.brightness = LXColor.b(c);
      int col = LXColor.hsb(this.hue, this.saturation, this.brightness);
      setRGB(col);
      setOklab(col);
    }

    public void set(
        LXDynamicColor color, float hueOffset, float saturationOffset, float brightnessOffset) {
      int c = color.getColor();
      this.hue = color.getHuef() + hueOffset;
      this.saturation = LXUtils.clampf(LXColor.s(c) + saturationOffset, 0, 100);
      this.brightness = LXUtils.clampf(LXColor.b(c) + brightnessOffset, 0, 100);
      int col = LXColor.hsb(this.hue, this.saturation, this.brightness);
      setRGB(col);
      setOklab(col);
    }

    public void setRGB(int c) {
      this.r = (c & LXColor.R_MASK) >>> LXColor.R_SHIFT;
      this.g = (c & LXColor.G_MASK) >>> LXColor.G_SHIFT;
      this.b = (c & LXColor.B_MASK);
    }

    /**
     * cube-root approximation function for float values. Faster than Math.cbrt(), but does not work
     * for negative inputs. That's fine for our one use case - RGB->Oklab conversion.
     * <br>
     * Original code:
     * https://github.com/Marc-B-Reynolds/Stand-alone-junk/blob/master/src/Posts/ballcube.c#L182-L197
     * . <br>
     *
     * @param x non-negative floating point number
     * @return the (approximate) cube root of x
     */
    public static float cbrt(float x) {
      // comments here are me figuring out wtf this is doing...
      //
      // use bit manipulation to get an initial guess for cbrt, which we
      // do by using bit shifts to divide the exponent by 3, then
      // "adjusting" the mantissa by adding a magic constant to produce
      // a sane floating point number.
      int ix = Float.floatToRawIntBits(x);
      final float x0 = x;
      ix = (ix >>> 2) + (ix >>> 4);
      ix += (ix >>> 4);
      ix += (ix >>> 8) + 0x2A5137A0;
      x = Float.intBitsToFloat(ix);

      // now refine the estimate using the
      // Newton-Raphson algorithm, which converges
      // very quickly. Two trips should get us close enough.
      x = 0.33333334f * (2f * x + x0 / (x * x));
      x = 0.33333334f * (2f * x + x0 / (x * x));
      return x;
    }

    public void setOklab(int col) {
      final float r = (float) (0xFF & LXColor.red(col)) * (1f / 255f);
      final float g = (float) (0xFF & LXColor.green(col)) * (1f / 255f);
      final float b = (float) (0xFF & LXColor.blue(col)) * (1f / 255f);

      // RGB->Oklab transfer function: multiply by oklab matrix, then take cube root
      this.lStar = cbrt(0.4121656120f * r + 0.5362752080f * g + 0.0514575653f * b);
      this.aStar = cbrt(0.2118591070f * r + 0.6807189584f * g + 0.1074065790f * b);
      this.bStar = cbrt(0.0883097947f * r + 0.2818474174f * g + 0.6302613616f * b);
    }

    public void set(ColorStop that) {
      this.hue = that.hue;
      this.saturation = that.saturation;
      this.brightness = that.brightness;
      this.r = that.r;
      this.g = that.g;
      this.b = that.b;
      this.lStar = that.lStar;
      this.aStar = that.aStar;
      this.bStar = that.bStar;
    }

    public boolean isBlack() {
      return this.brightness == 0;
    }

    @Override
    public String toString() {
      return String.format("rgb(%d,%d,%d) hsb(%f,%f,%f)", r, g, b, hue, saturation, brightness);
    }
  }

  public static class ColorStops {
    public final ColorStop[] stops = new ColorStop[LXSwatch.MAX_COLORS + 1];
    public int numStops = 1;

    public ColorStops() {
      for (int i = 0; i < this.stops.length; ++i) {
        this.stops[i] = new ColorStop();
      }
    }

    public int getColor(float lerp, BlendFunction blendFunction) {
      lerp = (lerp % 1f) * this.numStops;
      int stop = (int) Math.floor(lerp);
      return blendFunction.blend(this.stops[stop], this.stops[stop + 1], lerp - stop);
    }
  }

  /**
   * Hue interpolation modes. Since the hues form a color wheel, there are various strategies for
   * moving from hue1 to hue2.
   */
  public interface HueInterpolation {

    /**
     * Interpolate between two values
     *
     * @param hue1 Source hue
     * @param hue2 Destination hue
     * @param lerp Interpolation amount
     * @return A hue on a path between these two values
     */
    public float lerp(float hue1, float hue2, float lerp);

    /**
     * HSV path always stays within the color wheel of raw values, never crossing the 360-degree
     * boundary
     */
    public static final HueInterpolation HSV =
        (hue1, hue2, lerp) -> {
          return LXUtils.lerpf(hue1, hue2, lerp);
        };

    /**
     * HSVM takes the minimum path from hue1 to hue2, wrapping around the 360-degree boundary if it
     * makes for a shorter path
     */
    public static final HueInterpolation HSVM =
        (hue1, hue2, lerp) -> {
          if (hue2 - hue1 > 180) {
            hue1 += 360f;
          } else if (hue1 - hue2 > 180) {
            hue2 += 360f;
          }
          return LXUtils.lerpf(hue1, hue2, lerp);
        };

    /**
     * HSVCW takes a clockwise path always, even if it means a longer interpolation from hue1 to
     * hue2, e.g. [350->340] will go [350->360],[0->340]
     */
    public static final HueInterpolation HSVCW =
        (hue1, hue2, lerp) -> {
          if (hue2 < hue1) {
            hue2 += 360f;
          }
          return LXUtils.lerpf(hue1, hue2, lerp);
        };

    /**
     * HSVCCW takes a counter-clockwise path always, even if it means a longer interpolation from
     * hue1 to hue2, e.g. [340->350] will go [340->0],[360->350]
     */
    public static final HueInterpolation HSVCCW =
        (hue1, hue2, lerp) -> {
          if (hue1 < hue2) {
            hue1 += 360f;
          }
          return LXUtils.lerpf(hue1, hue2, lerp);
        };
  }

  /** A blend function interpolates between two colors */
  public interface BlendFunction {

    public int blend(ColorStop c1, ColorStop c2, float lerp);

    public static final BlendFunction RGB =
        (c1, c2, lerp) -> {
          int r = LXUtils.lerpi(c1.r, c2.r, lerp);
          int g = LXUtils.lerpi(c1.g, c2.g, lerp);
          int b = LXUtils.lerpi(c1.b, c2.b, lerp);
          return LXColor.rgba(r, g, b, 255);
        };

    public static final BlendFunction OKLAB =
      (c1, c2, lerp) -> {
        // linear interpolation in oklab space
        float l = LXUtils.lerpf(c1.lStar, c2.lStar, lerp);
        float m = LXUtils.lerpf(c1.aStar, c2.aStar, lerp);
        float s = LXUtils.lerpf(c1.bStar, c2.bStar, lerp);

        // Optional:
        // This will give a slight boost to mid-gradient colors, as
        // suggested by iq. Not part of oklab spec, but looks
        // a little nicer on 2 and 3 color swatches imo. Ymmv though.
        float bump = 1.0f+0.2f*lerp*(1.0f-lerp);
        l *= bump; m *= bump; s *= bump;

        // convert back to rgb (clamped to 0..1)
        // TODO: Can we SIMD this w/Vector library or JBLAS at some point?
        float r = Math.min(Math.max(+4.0767245293f * l - 3.3072168827f * m + 0.2307590544f * s, 0f), 1f);
        float g = Math.min(Math.max(-1.2681437731f * l + 2.6093323231f * m - 0.3411344290f * s, 0f), 1f);
        float b = Math.min(Math.max(-0.0041119885f * l - 0.7034763098f * m + 1.7068625689f * s, 0f), 1f);

        return LXColor.rgbf(r*r*r, g*g*g, b*b*b);
      };

    static BlendFunction _HSV(HueInterpolation hueLerp) {
      return (c1, c2, lerp) -> {
        float hue1 = c1.hue;
        float hue2 = c2.hue;
        float sat1 = c1.saturation;
        float sat2 = c2.saturation;
        if (c1.isBlack()) {
          hue1 = hue2;
          sat1 = sat2;
        } else if (c2.isBlack()) {
          hue2 = hue1;
          sat2 = sat1;
        }
        return LXColor.hsb(
            hueLerp.lerp(hue1, hue2, lerp),
            LXUtils.lerpf(sat1, sat2, lerp),
            LXUtils.lerpf(c1.brightness, c2.brightness, lerp));
      };
    }

    public static final BlendFunction HSV = _HSV(HueInterpolation.HSV);
    public static final BlendFunction HSVM = _HSV(HueInterpolation.HSVM);
    public static final BlendFunction HSVCW = _HSV(HueInterpolation.HSVCW);
    public static final BlendFunction HSVCCW = _HSV(HueInterpolation.HSVCCW);
  }

  public enum BlendMode {
    RGB("RGB", null, BlendFunction.RGB),
    HSV("HSV", HueInterpolation.HSV, BlendFunction.HSV),
    HSVM("HSV-Min", HueInterpolation.HSVM, BlendFunction.HSVM),
    OKLAB("Oklab", null, BlendFunction.OKLAB),
    HSVCW("HSV-CW", HueInterpolation.HSVCW, BlendFunction.HSVCW),
    HSVCCW("HSV-CCW", HueInterpolation.HSVCCW, BlendFunction.HSVCCW);


    public final String label;
    public final HueInterpolation hueInterpolation;
    public final BlendFunction function;

    private BlendMode(String label, HueInterpolation hueInterpolation, BlendFunction function) {
      this.label = label;
      this.hueInterpolation = hueInterpolation;
      this.function = function;
    }

    @Override
    public String toString() {
      return this.label;
    }
  };
}
