package titanicsend.util;

import heronarts.lx.model.LXPoint;

import java.util.ArrayList;
import java.util.List;

public class GridStriper {
  public static final int MICRONS_PER_INCH = 25400;
  public static final int MICRONS_PER_FOOT = MICRONS_PER_INCH * 12;
  public static final int HEIGHT = 12 * MICRONS_PER_FOOT;
  public static final int WIDTH =  12 * MICRONS_PER_FOOT;
  public static final int PIXELS_PER_STRAND = 50;
  public static final int NUM_STRANDS = 50;
  public static final int X_SPAN = WIDTH / (PIXELS_PER_STRAND - 1);
  public static final int Y_SPAN = HEIGHT / (NUM_STRANDS - 1);

  public static List<LXPoint> stripe() {
    int y = -HEIGHT / 2;
    List<LXPoint> rv = new ArrayList<>();

    for (int i = 0; i < NUM_STRANDS; i++) {
      boolean fwd = i % 2 == 0;
      int x = fwd ? -WIDTH / 2 : WIDTH / 2;
      for (int j = 0; j < PIXELS_PER_STRAND; j++) {
        rv.add(new LXPoint(x, y, 0));
        if (fwd)
          x += X_SPAN;
        else
          x -= X_SPAN;
      }
      y += Y_SPAN;
    }
    return rv;
  }
}
