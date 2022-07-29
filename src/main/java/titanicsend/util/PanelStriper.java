package titanicsend.util;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEStripingInstructions;
import titanicsend.model.TEVertex;

import java.util.*;

public class PanelStriper {
  public static final int MARGIN = 86000;
  public static final int DISTANCE_BETWEEN_PIXELS = 50000; // 50k microns ~= 2 inches
  public static final FloorPoint gapFloorPoint = new FloorPoint(-1, -1);

  private static TEVertex[] getStartMidEnd(
          TEVertex v0, TEVertex v1, TEVertex v2,
          TEStripingInstructions stripingInstructions) {
    double distance01 = v0.distanceTo(v1);
    double distance02 = v0.distanceTo(v2);
    double distance12 = v1.distanceTo(v2);

    assert distance01 != distance02;
    assert distance01 != distance12;
    // distance02 and distance12 are equal at fore and aft

    TEVertex vStart = null;
    TEVertex vMid = null;
    TEVertex vEnd = null;

    // Set vEnd to the vertex opposite the longest edge
    // Set vStart to the vertex closest to vEnd
    if (distance01 > distance02 && distance01 > distance12) {
      vEnd = v2;
      if (distance02 < distance12) {
        vStart = v0;
        vMid = v1;
      } else if (distance12 < distance02) {
        vStart = v1;
        vMid = v0;
      } else if (v0.id < v1.id) {
        vStart = v0;
        vMid = v1;
      } else {
        assert v0.id != v1.id;
        vStart = v1;
        vMid = v0;
      }
    } else if (distance02 > distance01 && distance02 > distance12) {
      vEnd = v1;
      if (distance01 < distance12) {
        vStart = v0;
        vMid = v2;
      } else {
        vStart = v2;
        vMid = v0;
      }
    } else if (distance12 > distance01 && distance12 > distance02) {
      vEnd = v0;
      if (distance01 < distance02) {
        vStart = v1;
        vMid = v2;
      } else {
        vStart = v2;
        vMid = v1;
      }
    } else {
      throw new Error("Math fail");
    }

    if (stripingInstructions != null) {
      String[] tokens = stripingInstructions.startingEdgeId.split("-");
      assert tokens.length == 2;
      int startId = Integer.parseInt(tokens[0]);
      int midId = Integer.parseInt(tokens[1]);

      if (v0.id == startId) {
        vStart = v0;
        v0 = null;
      } else if (v1.id == startId) {
        vStart = v1;
        v1 = null;
      } else if (v2.id == startId) {
        vStart = v2;
        v2 = null;
      } else throw new IllegalArgumentException("Nothing matches " + startId);

      if (v0 != null && v0.id == midId) {
        vMid = v0;
        v0 = null;
      } else if (v1 != null && v1.id == midId) {
        vMid = v1;
        v1 = null;
      } else if (v2 != null && v2.id == midId) {
        vMid = v2;
        v2 = null;
      } else throw new IllegalArgumentException("Nothing matches " + startId);

      if (v0 == null && v1 == null) vEnd = v2;
      if (v0 == null && v2 == null) vEnd = v1;
      if (v1 == null && v2 == null) vEnd = v0;
    }

    return new TEVertex[] {vStart, vMid, vEnd};
  }

  public static String stripe(String id, TEVertex v0, TEVertex v1, TEVertex v2,
                              List<LXPoint> pointList,
                              TEStripingInstructions stripingInstructions,
                              LXPoint gapPoint) {
    TEVertex[] startMidEnd = getStartMidEnd(v0, v1, v2, stripingInstructions);

    TEVertex vStart = startMidEnd[0];
    TEVertex vMid = startMidEnd[1];
    TEVertex vEnd = startMidEnd[2];

    FloorTransform floorTransform = new FloorTransform(vStart, vMid, vEnd);

    List<FloorPoint> floorPoints;

    if (stripingInstructions == null)
      floorPoints = oldStripeFloor(floorTransform.f0, floorTransform.f1, floorTransform.f2);
    else
      floorPoints = newStripeFloor(
              floorTransform.f0, floorTransform.f1, floorTransform.f2,
              stripingInstructions, gapFloorPoint);

    for (FloorPoint f : floorPoints) {
      if (f == gapFloorPoint) {
        pointList.add(gapPoint);
      } else {
        if (!triangleContains(floorTransform.f0, floorTransform.f1, floorTransform.f2, f)) {
          LX.error(id + " contains points outside its boundaries");
          break;
/*        } else if (distanceToEdge(floorTransform.f0, floorTransform.f1,
                floorTransform.f2, f) < MARGIN) {
          LX.error(id + " contains points inside its margin");
          break;
*/        } else {
          pointList.add(floorTransform.fly(f));
        }
      }
    }

    int distanceSM = (int)vStart.distanceTo(vMid);
    int distanceME = (int)vMid.distanceTo(vEnd);
    int distanceES = (int)vEnd.distanceTo(vStart);

    return distanceSM + "-" + distanceME + "-" + distanceES;
  }

  // Lays out all the pixels in a LIT panel, once it's been sent through FloorTransform
  // to lay it on the X-Z plane. Starts at fStart and finds the nearest point inside the
  // border margin, and that's where the first pixel goes, then it stripes back and forth,
  // one row at a time, until it runs out of triangle.
  private static List<FloorPoint> oldStripeFloor(
          FloorPoint fStart, FloorPoint fMid, FloorPoint fEnd) {
    FloorPoint currentPoint = findStartingPoint(fEnd);
    ArrayList<FloorPoint> rv = new ArrayList<>();
    double deltaX = DISTANCE_BETWEEN_PIXELS;
    while (currentPoint.z < fEnd.z) {
      if (triangleContains(fStart, fMid, fEnd, currentPoint) &&
              distanceToEdge(fStart, fMid, fEnd, currentPoint) >= MARGIN
      ) rv.add(currentPoint);

      double nextX = currentPoint.x + deltaX;
      double nextZ = currentPoint.z;

      boolean nextRow = false;

      while (nextX > fMid.x) {
        nextX -= DISTANCE_BETWEEN_PIXELS / 2.0;
        nextRow = true;
      }

      while (nextX < 0.0) {
        nextX += DISTANCE_BETWEEN_PIXELS / 2.0;
        nextRow = true;
      }

      if (nextRow) {
        nextZ += DISTANCE_BETWEEN_PIXELS * 0.5 * Math.sqrt(3.0);
        deltaX = -deltaX;
      }

      currentPoint = new FloorPoint(nextX, nextZ);
    }

    return rv;
  }

  private static List<FloorPoint> newStripeFloor(
          FloorPoint fStart, FloorPoint fMid, FloorPoint fEnd,
          TEStripingInstructions stripingInstructions, FloorPoint gapFloorPoint) {
    FloorPoint currentPoint = findStartingPoint(fEnd);
    ArrayList<FloorPoint> rv = new ArrayList<>();

    final double EPSILON = DISTANCE_BETWEEN_PIXELS / 10.0;
    while(distanceToEdge(fStart, fMid, fEnd, currentPoint) < MARGIN) {
      currentPoint = new FloorPoint(currentPoint.x + EPSILON, currentPoint.z);
    }

    double deltaX = DISTANCE_BETWEEN_PIXELS;

    int numRows = stripingInstructions.rowLengths.length;
    for (int i = 0; i < numRows; i++) {
      int rowLength = stripingInstructions.rowLengths[i];
      int beforeNudges = stripingInstructions.beforeNudges[i];
      int gaps = stripingInstructions.gaps[i];

      // For each positive beforeNudge, move back; for each negative, move forward.
      currentPoint = new FloorPoint(currentPoint.x - deltaX * beforeNudges, currentPoint.z);

      // Plot out the row
      for (int j = 0; j < rowLength; j++) {
        rv.add(currentPoint);
        currentPoint = new FloorPoint(currentPoint.x + deltaX, currentPoint.z);
      }

      // We're one step too far at this point. Go back, plus an extra half step
      // because of the changing row. Also update z.
      currentPoint = new FloorPoint(
              currentPoint.x - 1.5 * deltaX,
              currentPoint.z + DISTANCE_BETWEEN_PIXELS * 0.5 * Math.sqrt(3.0));

      // Add gap pixels, which don't live in our 3-D space
      for (int j = 0; j < gaps; j++) {
        rv.add(gapFloorPoint);
      }

      // Reverse direction
      deltaX = -deltaX;
    }

    return rv;
  }

  private static double calcHeading(FloorPoint start, FloorPoint destination) {
    double dx = destination.x - start.x;
    double dz = destination.z - start.z;
    return Math.atan2(dz, dx);
  }

  // https://en.wikipedia.org/wiki/Distance_from_a_point_to_a_line
  private static double distanceBetPointAndLine(FloorPoint f0, FloorPoint f1, FloorPoint f) {
    double top = Math.abs((f1.x - f0.x) * (f0.z - f.z) - (f0.x - f.x) * (f1.z - f0.z));
    double bot = Math.sqrt(Math.pow(f1.x - f0.x, 2) + Math.pow(f1.z - f0.z, 2));
    return top / bot;
  }

  private static double sign(FloorPoint f0, FloorPoint f1, FloorPoint f2) {
    return (f0.x - f2.x) * (f1.z - f2.z) - (f1.x - f2.x) * (f0.z - f2.z);
  }

  private static boolean triangleContains(FloorPoint f0, FloorPoint f1, FloorPoint f2, FloorPoint f) {
    double d1, d2, d3;
    boolean has_neg, has_pos;

    d1 = sign(f, f0, f1);
    d2 = sign(f, f1, f2);
    d3 = sign(f, f2, f0);

    has_neg = (d1 < 0) || (d2 < 0) || (d3 < 0);
    has_pos = (d1 > 0) || (d2 > 0) || (d3 > 0);

    return !(has_neg && has_pos);
  }

  // Returns the distance from f to the nearest edge of the f0-f1-f2 triangle.
  private static double distanceToEdge(FloorPoint f0, FloorPoint f1, FloorPoint f2, FloorPoint f) {
    var d1 = distanceBetPointAndLine(f0, f1, f);
    var d2 = distanceBetPointAndLine(f0, f2, f);
    var d3 = distanceBetPointAndLine(f1, f2, f);
    return Math.min(Math.min(d1, d2), d3);
  }

  private static FloorPoint findStartingPoint(FloorPoint fEnd) {
    double z = MARGIN;
    assert (fEnd.x > 0);
    assert (fEnd.z > 0);

    double slope = fEnd.z / fEnd.x;
    double x = z / slope;
    return new FloorPoint(x, z);
  }
}