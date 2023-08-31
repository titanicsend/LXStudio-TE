package titanicsend.model;

import java.util.*;

import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;
import titanicsend.app.TEVirtualColor;
import titanicsend.effect.PanelAdjustEffect;
import titanicsend.util.OffsetTriangles;

public class TEPanelModel extends TEModel {
  public static final float PANEL_BACKING_DISTANCE = 50000;  // About two inches

  // Useful data for points inside LIT panels
  public static class LitPointData {
    public LXPoint point;
    public double radius;         // Distance from the centroid
    public double radiusFraction; // ...expressed as a fraction <= 1.0

    LitPointData(LXPoint point, double radius, double radiusFraction) {
      this.point = point;
      this.radius = radius;
      this.radiusFraction = radiusFraction;
    }
  }

  public final static String UNKNOWN = "unknown";
  public final static String SOLID = "solid";
  public final static String LIT = "lit";

  public String id;
  public final TEVertex v0, v1, v2;
  public final TEEdgeModel e0, e1, e2;
  public String panelType;
  public String flavor;
  public final LXVector centroid;

  int[] channelLengths;
  public List<LitPointData> litPointData;
  public OffsetTriangles offsetTriangles;

  private Adjustment currentAdjustment = new Adjustment();

  // Set to non-null and the virtual display will shade the panel's triangle
  public TEVirtualColor virtualColor;

  public String getId() {
    return this.id;
  }

  public TEPanelModel(String id, ArrayList<LXPoint> points, TEVertex v0, TEVertex v1, TEVertex v2,
                      TEEdgeModel e0, TEEdgeModel e1, TEEdgeModel e2, String panelType,
                      String flavor, LXVector centroid, int[] channelLengths, String ... tags) {
    super("Panel", points, tags);

    this.id = id;

    this.panelType = panelType;
    this.flavor = flavor;
    this.centroid = centroid;
    this.channelLengths = channelLengths;

    switch (panelType) {
      case UNKNOWN:
        // Display unknown panels as wispy pink
        this.virtualColor = new TEVirtualColor(255, 0, 255, 100);
        this.litPointData = null;
        break;
      case LIT:
        // Display lit panels as semi-transparent gold
        //this.virtualColor = new TEVirtualColor(255, 255, 0, 200);

        // Don't display lit panels
        this.virtualColor = null;

        double radius0 = v0.distanceTo(this.centroid);
        double radius1 = v1.distanceTo(this.centroid);
        double radius2 = v2.distanceTo(this.centroid);
        double maxRadius = Math.max(radius0, Math.max(radius1, radius2));

        // Calculate useful data for each of this panel's points relative to the panel
        this.litPointData = new ArrayList<LitPointData>();
        for (LXPoint point : points) {
          double radius = TEVertex.distance(this.centroid, point);
          double radiusFraction = radius / maxRadius;
          litPointData.add(new LitPointData(point, radius, radiusFraction));
        }
        break;
      case SOLID:
        // Display solid panels as semi-transparent black, recolorable by patterns
        this.virtualColor = new TEVirtualColor(0, 0, 0, 200);
        this.litPointData = null;
        break;
      default:
        throw new Error("Unknown panel type: " + this.panelType);
    }

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

    this.offsetTriangles = new OffsetTriangles(v0, v1, v2, PANEL_BACKING_DISTANCE);
  }

  // Checks if two panels touch along an edge (not just at a vertex)
  public boolean touches(TEPanelModel other) {
    for (TEEdgeModel eThis : new TEEdgeModel[]{this.e0, this.e1, this.e2}) {
      for (TEEdgeModel eOther : new TEEdgeModel[]{other.e0, other.e1, other.e2}) {
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
    for (TEEdgeModel e : new TEEdgeModel[]{this.e0, this.e1, this.e2}) {
      rv.addAll(e.connectedPanels);
    }
    return rv;
  }

  public TEPanelSection getSection() {
    return getSection(this.centroid);
  }

  // See enum class for section description
  // TODO Replace this with something smarter
  // This is a really lazy/sloppy way of doing this, though if we're not changing the model it should be fine
  static int k = 0;
  static public TEPanelSection getSection(LXVector centroid) {
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
      point.set(
              point.x + xAdjust,
              point.y + yAdjust,
              point.z + zAdjust
              );
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