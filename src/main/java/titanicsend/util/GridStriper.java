package titanicsend.util;

import heronarts.lx.model.LXPoint;

import java.util.ArrayList;
import java.util.List;

public class GridStriper {
  public static final int HEIGHT = 1524000;
  public static final int WIDTH = 3048000;
  public static final int X_SPAN = 73152;
  public static final int Y_SPAN = 73152;
  public static final int PIXELS_PER_STRAND = 50;
  public static final int NUM_STRANDS = 50;

  public static List<LXPoint> stripe() {
    int y = -HEIGHT / 2;
    List<LXPoint> rv = new ArrayList<>();
    for (int i = 0; i < NUM_STRANDS; i++) {
      int x = -WIDTH / 2;
      for (int j = 0; j < PIXELS_PER_STRAND; j++) {
        rv.add(new LXPoint(x, y, 0));
        x += X_SPAN;
      }
      y += Y_SPAN;
    }
    return rv;
  }
}
