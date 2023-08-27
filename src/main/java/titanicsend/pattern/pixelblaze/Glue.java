package titanicsend.pattern.pixelblaze;

import heronarts.lx.color.LXColor;
import java.util.Random;
import java.util.SplittableRandom;

public class Glue {

  // Math
  public static float fract(float x) {
    return (float) (x - Math.floor(x));
  }
  public static float mod(float x, float y) {
    return (float) (x - Math.floor(x / y) * y);
  }
  public static float square(float n,float dutyCycle) {
    return (float) ((Math.abs(fract(n)) <= dutyCycle) ? 1.0 : 0.0);
  }
  public static float smoothstep(float edge0, float edge1, float x) {
    x = Math.max(0, Math.min(1, (x - edge0) / (edge1 - edge0)));
    return x * x * (3 - 2 * x);
  }

  public static float prng(float v) {
    return fract(Math.sin(v * 12.9898) * 43758.5453);
  }

  public static float mix(float x, float y, float a) { return x * (1 - a) + y * a; }

  // color, painting, drawing
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
    return Math.max(0,Math.min(255, (int)(a * 255.0F))) << 24 | (color & LXColor.RGB_MASK);
  }
}