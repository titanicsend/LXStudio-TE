package titanicsend.model;

import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;

import java.util.*;
import titanicsend.app.TEVirtualColor;
import titanicsend.util.OffsetTriangles;
import titanicsend.util.TEMath;

public class TEPanelModel extends TEModel {

  public static final String TE_MODEL_TYPE = "Panel";

  // Static model constants
  public static final float PANEL_BACKING_DISTANCE_STATIC = 90000; // About four inches
  public static final String UNKNOWN = "unknown";
  public static final String SOLID = "solid";
  public static final String LIT = "lit";

  // Dynamic model constants
  public static final float PANEL_BACKING_DISTANCE_DYNAMIC = 2;
  public static final String TAG = "panel";
  public static final String META_ID = "panelId";
  public static final String META_EDGE1 = "edge1";
  public static final String META_EDGE2 = "edge2";
  public static final String META_EDGE3 = "edge3";
  public static final String META_LEADING_EDGE = "leadingEdge";
  public static final String META_MODULE = "module";
  
  /**
   * Wrapper around each LXPoint in the panel
   */
  public static class Point {
    public final LXPoint point;
    public final double radius; // Distance from the centroid
    public final double radiusFraction; // ...expressed as a fraction <= 1.0

    Point(LXPoint point, double radius, double radiusFraction) {
      this.point = point;
      this.radius = radius;
      this.radiusFraction = radiusFraction;
    }
  }

  // Two versions of the internal array of points:
  // 1. Pass-through to the underlying model's LXPoints
  public final LXPoint[] points;
  // 2. Wrapper around each LXPoint adding edge-specific data
  public final Point[] panelPoints;

  // Pass-through to model, for transition from static->dynamic.
  public final int size;

  public final String panelType;
  public final String flavor;
  public final LXVector centroid;

  // UI backer coordinates
  public OffsetTriangles offsetTriangles;
  // Set to non-null and the virtual display will shade the panel's triangle
  public TEVirtualColor virtualColor;

  // Connections
  public final TEVertex v0, v1, v2;
  public TEEdgeModel e0, e1, e2;

  // Adjustments for distortion on ends of car
  private Adjustment currentAdjustment = new Adjustment();

  /**
   * Static model constructor (2022-23)
   */
  public TEPanelModel(
      String id,
      ArrayList<LXPoint> points,
      TEVertex v0,
      TEVertex v1,
      TEVertex v2,
      TEEdgeModel e0,
      TEEdgeModel e1,
      TEEdgeModel e2,
      String panelType,
      String flavor,
      LXVector centroid,
      int[] channelLengths,
      String... tags) {
    super(TE_MODEL_TYPE, points, tags);

    setId(id);

    this.panelType = panelType;
    this.flavor = flavor;
    this.centroid = centroid;

    switch (panelType) {
      case UNKNOWN:
        // Display unknown panels as wispy pink
        this.virtualColor = new TEVirtualColor(255, 0, 255, 100);
        this.points = new LXPoint[0];
        this.panelPoints = new Point[0];
        break;
      case LIT:
        // Display lit panels as semi-transparent gold
        // this.virtualColor = new TEVirtualColor(255, 255, 0, 200);

        // Don't display lit panels
        this.virtualColor = null;

        double radius0 = v0.distanceTo(this.centroid);
        double radius1 = v1.distanceTo(this.centroid);
        double radius2 = v2.distanceTo(this.centroid);
        double maxRadius = Math.max(radius0, Math.max(radius1, radius2));

        // Calculate useful data for each of this panel's points relative to the panel
        this.points = this.model.points;
        this.panelPoints = new Point[this.points.length];
        for (int i = 0; i < this.points.length; i++) {
          LXPoint point = this.points[i];
          double radius = TEVertex.distance(this.centroid, point);
          double radiusFraction = radius / maxRadius;
          this.panelPoints[i] = new Point(point, radius, radiusFraction);
        }
        break;
      case SOLID:
        // Display solid panels as semi-transparent black, recolorable by patterns
        this.virtualColor = new TEVirtualColor(0, 0, 0, 200);
        this.points = new LXPoint[0];
        this.panelPoints = new Point[0];

        break;
      default:
        throw new Error("Unknown panel type: " + this.panelType);
    }

    this.size = this.points.length;

    // Make sure we have three different edges
    assert e0 != e1;
    assert e0 != e2;
    assert e1 != e2;

    // ...and three different vertexes
    assert v0 != v1;
    assert v0 != v2;
    assert v1 != v2;

    // Make sure each edge touches the other two
    assert e0.touches(e1);
    assert e0.touches(e2);
    assert e1.touches(e2);

    // Make sure each edge touches exactly two of the three vertexes
    assert countTouches(e0, v0, v1, v2) == 2;
    assert countTouches(e1, v0, v1, v2) == 2;
    assert countTouches(e2, v0, v1, v2) == 2;

    this.e0 = e0;
    this.e1 = e1;
    this.e2 = e2;

    this.v0 = v0;
    this.v1 = v1;
    this.v2 = v2;

    this.offsetTriangles = new OffsetTriangles(v0, v1, v2, PANEL_BACKING_DISTANCE_STATIC);
  }

  /**
   * Dynamic model constructor (2024+)
   */
  public TEPanelModel(LXModel model, TEVertex v0, TEVertex v1, TEVertex v2) {
    super(TE_MODEL_TYPE, model);

    this.v0 = v0;
    this.v1 = v1;
    this.v2 = v2;

    setId(this.model.meta(META_ID));
    
    // These variables are unnecessary in dynamic model:
    this.panelType = LIT;
    this.flavor = "";

    // Pass-through variables
    this.size = this.model.points.length;
    this.points = this.model.points;

    // Calculate centroid as an average of the points,
    // different from static model where it was an average of the 3 vertices.
    this.centroid = calculateCentroid(this.points);
    double maxRadius = 0;
    for (LXPoint p : this.points) {
      maxRadius = Math.max(maxRadius,
        TEMath.distance(this.centroid.x, this.centroid.y, this.centroid.z, p.x, p.y, p.z));
    }

    // Wrap each point to store some additional data.
    this.panelPoints = new Point[this.points.length];
    for (int i = 0; i < this.points.length; i++) {
      LXPoint point = this.points[i];
      double radius = TEVertex.distance(this.centroid, point);
      double radiusFraction = radius / maxRadius;
      this.panelPoints[i] = new Point(point, radius, radiusFraction);
    }

    // Calculate UI backer triangles for dynamic model
    // Use the end points of the first row and the middle point of the last row.
    LXModel firstRow = this.model.children[0];
    LXModel lastRow = this.model.children[this.model.children.length - 1];
    LXVector vA = new LXVector(firstRow.points[0]);
    LXVector vB = new LXVector(firstRow.points[firstRow.size - 1]);
    LXVector vC = new LXVector(lastRow.points[lastRow.size / 2]);
    this.offsetTriangles = new OffsetTriangles(vA, vB, vC, PANEL_BACKING_DISTANCE_DYNAMIC);
  }

  private static LXVector calculateCentroid(LXPoint[] points) {
    LXVector centroid = new LXVector(0, 0, 0);
    if (points.length > 0) {
      for (LXPoint p : points) {
        centroid.add(p.x, p.y, p.z);
      }
      centroid.div(points.length);
    }
    return centroid;
  }

  // Checks if two panels touch along an edge (not just at a vertex)
  public boolean touches(TEPanelModel other) {
    for (TEEdgeModel eThis : new TEEdgeModel[] {this.e0, this.e1, this.e2}) {
      for (TEEdgeModel eOther : new TEEdgeModel[] {other.e0, other.e1, other.e2}) {
        if (eThis == eOther) return true;
      }
    }
    return false;
  }

  // Given an Edge and three Vertexes, return the number of vertexes the edge touches
  private static int countTouches(TEEdgeModel e, TEVertex v0, TEVertex v1, TEVertex v2) {
    int rv = 0;
    if (e.touches(v0)) rv++;
    if (e.touches(v1)) rv++;
    if (e.touches(v2)) rv++;
    return rv;
  }

  // Returns set of panels that touch along an edge (not just at a vertex)
  public Set<TEPanelModel> neighbors() {
    HashSet<TEPanelModel> rv = new HashSet<>();
    for (TEEdgeModel e : new TEEdgeModel[] {this.e0, this.e1, this.e2}) {
      rv.addAll(e.connectedPanels);
    }
    return rv;
  }

  public TEPanelSection getSection() {
    return getSection(this.centroid);
  }

  // See enum class for section description
  // TODO Replace this with something smarter
  // This is a really lazy/sloppy way of doing this, though if we're not changing the model it
  // should be fine
  public static TEPanelSection getSection(LXVector centroid) {
    boolean fore = centroid.z < 0;

    // is it an end panel?
    if (Math.abs(centroid.x) < 1200000) {
      return fore ? TEPanelSection.FORE : TEPanelSection.AFT;
    }

    boolean portSide = centroid.x > 0;

    // is it high or low on the car?
    if (centroid.y < 5000000) {
      // lower section
      if (portSide) {
        return fore ? TEPanelSection.PORT_FORE : TEPanelSection.PORT_AFT;
      } else {
        return fore ? TEPanelSection.STARBOARD_FORE : TEPanelSection.STARBOARD_AFT;
      }
    } else {
      // upper section
      if (portSide) {
        return fore ? TEPanelSection.PORT_FORE_SINGLE : TEPanelSection.PORT_AFT_SINGLE;
      } else {
        return fore ? TEPanelSection.STARBOARD_FORE_SINGLE : TEPanelSection.STARBOARD_AFT_SINGLE;
      }
    }
  }

  public void setAdjustment(Adjustment newAdjustment) {
    if (newAdjustment == null) {
      newAdjustment = new Adjustment();
    }
    float xAdjust = newAdjustment.x - currentAdjustment.x;
    float yAdjust = newAdjustment.y - currentAdjustment.y;
    float zAdjust = newAdjustment.z - currentAdjustment.z;
    for (LXPoint point : points) {
      point.set(point.x + xAdjust, point.y + yAdjust, point.z + zAdjust);
    }
    currentAdjustment = newAdjustment.copy();
  }

  public void clearAdjustment() {
    setAdjustment(null);
  }

  public static class Adjustment {
    public float x = 0;
    public float y = 0;
    public float z = 0;

    public Adjustment copy() {
      Adjustment adjustment = new Adjustment();
      adjustment.x = x;
      adjustment.y = y;
      adjustment.z = z;
      return adjustment;
    }
  }
}
