package titanicsend.model;

import heronarts.lx.model.LXPoint;
import heronarts.lx.model.LXModel;
import java.util.*;

public class TEEdgeModel extends TEModel {
  String teModelType = "Edge";
  public TEVertex v0, v1;
  public HashSet<TEPanelModel> connectedPanels;

  // In microns, the same unit x,y,z coordinates use
  public static final int DISTANCE_BETWEEN_PIXELS = 16666; // 0.76 inches/pixel; 1.31 pix/inch

  public TEEdgeModel(TEVertex v0, TEVertex v1, boolean dark) {
    super("Edge", makePoints(v0, v1, dark));
    this.v0 = v0;
    this.v1 = v1;
    this.connectedPanels = new HashSet<TEPanelModel>();
  }

  public String getId() {
    return this.v0.id + "-" + this.v1.id;
  }

  public TEEdgeModel(TEVertex v0, TEVertex v1) {
    this(v0, v1, false);
  }

  private static List<LXPoint> makePoints(TEVertex v0, TEVertex v1, boolean dark) {
    List<LXPoint> points = new ArrayList<LXPoint>();
    if (dark) return points;

    int numPixels = (int)(v0.distanceTo(v1) / DISTANCE_BETWEEN_PIXELS);
    assert numPixels > 0 : "Edge " + v0.repr() + "-" + v1.repr() + " so short it has no pixels";

    float dx = v1.x - v0.x;
    float dy = v1.y - v0.y;
    float dz = v1.z - v0.z;

    for (int i = 0; i < numPixels; i++) {
      float fraction = (float)(i) / numPixels;
      LXPoint point = new LXPoint(
              v0.x + dx * fraction,
              v0.y + dy * fraction,
              v0.z + dz * fraction
      );
      points.add(point);
    }
    return points;
  }

  public boolean touches(TEEdgeModel other) {
    return this.v0.edges.contains(other) || this.v1.edges.contains(other);
  }

  public boolean touches(TEVertex v) {
    return this.v0 == v || this.v1 == v;
  }

  public TEVertex otherSide(TEVertex v) {
    if (v == v0) {
      return v1;
    } else if (v == v1) {
      return v0;
    } else {
      throw new Error("otherSide() called with invalid vertex");
    }
  }
}