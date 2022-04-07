package titanicsend.pattern.pixelblaze;

import heronarts.lx.LX;

import java.util.Random;

public class Glue {
  public static float hue;
  public static float saturation;
  public static float brightness;

  private static Random rand = new Random();

  public static double random(double max) {
    double rv = rand.nextDouble() * max;
    return rv;
  }

  public static void hsv(float h, float s, float v) {
    LX.log(h + " " + s + " " + v);
    hue = h;
    saturation = s;
    brightness = v;
  }
}