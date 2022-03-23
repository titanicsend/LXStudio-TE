package titanicsend.util;

import heronarts.lx.model.LXPoint;

// I ended up not using this, but I might need it at some point, and I'll be
// damned if I'm going to figure it all out again from scratch.

public class Circumcenter {
  public static LXPoint find(LXPoint p0, LXPoint p1, LXPoint p2) {
    double dx10 = p1.x - p0.x;
    double dy10 = p1.y - p0.y;
    double dz10 = p1.z - p0.z;

    double dx20 = p2.x - p0.x;
    double dy20 = p2.y - p0.y;
    double dz20 = p2.z - p0.z;

    double scalar1010 = dx10 * dx10 + dy10 * dy10 + dz10 * dz10;
    double scalar1020 = dx10 * dx20 + dy10 * dy20 + dz10 * dz20;
    double scalar2020 = dx20 * dx20 + dy20 * dy20 + dz20 * dz20;

    double det = scalar1010 * scalar2020 - scalar1020 * scalar1020;
    double alpha = 0.5 * (scalar2020 * scalar1010 - scalar1020 * scalar2020) / det;
    double beta = 0.5 * (-scalar1020 * scalar1010 + scalar1010 * scalar2020) / det;

    return new LXPoint(
            p0.x + alpha * dx10 + beta * dx20,
            p0.y + alpha * dy10 + beta * dy20,
            p0.z + alpha * dz10 + beta * dz20
    );
  }
}
