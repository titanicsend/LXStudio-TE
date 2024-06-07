package titanicsend.model;

import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import java.util.*;
import java.util.stream.Collectors;

public class TEEdgeModel extends TEModel {

  public static final String TE_MODEL_TYPE = "Edge";

  public static final String META_ID = "edgeId";
  public static final String META_V0 = "v0";
  public static final String META_V1 = "v1";
  public static final String META_MODULE = "module";

  /**
   * Wrapper around each LXPoint in the edge
   */
  public static class Point {
    public final LXPoint point;

    /**
     * `i` is the index into this edge Contrast this with `LXPoint.index` which must be globally
     * unique
     */
    public final int i;

    /**
     * `frac` is the fractional percentage (0..1) into this edge Calling it `in` would seem
     * canonical ("i, normalized"), but is too similar to the abbreviation for "inches"
     */
    public final float frac;

    public Point(LXPoint point, int i, float fraction) {
      this.point = point;
      this.i = i;
      this.frac = fraction;
    }
  }

  // Two versions of the internal array of points:
  // 1. Pass-through to the underlying model's LXPoints
  public final LXPoint[] points;
  // 2. Wrapper around each LXPoint adding edge-specific data
  public final Point[] edgePoints;

  // Pass-through to model, for transition from static->dynamic.
  public final int size;

  // Connections
  public final TEVertex v0, v1;
  public final Set<TEPanelModel> connectedPanels = new HashSet<TEPanelModel>();
  /**
   * This edge and any other that's a reflection about the XY or YZ planes 
   */
  public final List<TEEdgeModel> symmetryGroup = new ArrayList<TEEdgeModel>();

  /**
   * Static model constructor (2022-23)
   */
  public TEEdgeModel(TEVertex v0, TEVertex v1, int numPixels, boolean dark, String... tags) {
    super(TE_MODEL_TYPE, makePoints(v0, v1, numPixels, dark), tags);

    this.v0 = v0;
    this.v1 = v1;

    setId(this.v0.id + "-" + this.v1.id);

    this.size = this.model.size;
    this.points = this.model.points;
    // Allocate an array of the LXPoint wrapper, TEEdgeModel.Point
    this.edgePoints = new Point[this.model.points.length];
    for (int i = 0; i < this.points.length; i++) {
      // Now that Point can't extend LXPoint we ended up calculating the fraction twice.
      this.edgePoints[i] = new Point(this.points[i], i, fraction(i, this.size));
    }
  }

  private static List<LXPoint> makePoints(TEVertex v0, TEVertex v1, int numPixels, boolean dark) {
    List<LXPoint> points = new ArrayList<LXPoint>();
    if (dark) return points;

    float dx = v1.x - v0.x;
    float dy = v1.y - v0.y;
    float dz = v1.z - v0.z;

    for (int i = 0; i < numPixels; i++) {
      float fraction = fraction(i, numPixels);
      points.add(new LXPoint(v0.x + dx * fraction, v0.y + dy * fraction, v0.z + dz * fraction));
    }
    return points;
  }

  private static float fraction(int i, int numPixels) {
    if (numPixels > 1) {
      return (float) (i) / (numPixels - 1);
    } else {
      return .5f;
    }
  }

  /**
   * Dynamic model constructor (2024+)
   */
  public TEEdgeModel(LXModel model, TEVertex v0, TEVertex v1) {
    super(TE_MODEL_TYPE, model);

    this.v0 = v0;
    this.v1 = v1;

    setId(this.model.meta(META_ID));

    this.size = this.model.points.length;
    this.points = this.model.points;
    this.edgePoints = new Point[this.points.length];
    for (int i = 0; i < this.points.length; i++) {
      this.edgePoints[i] = new Point(this.points[i], i, fraction(i, this.size));
    }
  }

  public void rebuildConnections(List<TEEdgeModel> edges, List<TEPanelModel> panels) {
    // TODO: Add more edge connection lists

    this.connectedPanels.clear();
    this.connectedPanels.addAll(
      panels.stream()
      .filter(item ->
           item.edge0id == this.getId()
        || item.edge1id == this.getId()
        || item.edge2id == this.getId())
      .collect(Collectors.toList()));
  }
}
