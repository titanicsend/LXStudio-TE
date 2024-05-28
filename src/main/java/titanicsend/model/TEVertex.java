package titanicsend.model;

import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;
import java.util.*;
import titanicsend.app.TEVirtualColor;
import titanicsend.util.TEMath;

public class TEVertex extends LXVector {

  public final int id;
  public final Set<TEEdgeModel> edges = new HashSet<TEEdgeModel>();
  public final Set<TEPanelModel> panels = new HashSet<TEPanelModel>();

  // Set to non-null and the virtual display will shade vertex's sphere
  public TEVirtualColor virtualColor;

  /**
   * Static model constructor (2022-23)
   */
  public TEVertex(LXVector vector, int id) {
    super(vector);
    this.id = id;
    this.virtualColor = new TEVirtualColor(255, 255, 255, 255);
  }

  /**
   * Dynamic model constructor (2024+)
   * 
   * In this case TEVertex is just a label referenced by multiple edges & panels.
   * It does not have exact coordinates and therefore does not need to extend LXVector.
   */
  public TEVertex(int id) {
    super(0, 0, 0);
    this.id = id;
    this.virtualColor = new TEVirtualColor(255, 255, 255, 255);    
  }

  /*
   * Static Model
   */

  public static double distance(LXVector v, float x, float y, float z) {
    return TEMath.distance(v.x, v.y, v.z, x, y, z);
  }

  public static double distance(LXVector v, LXPoint p) {
    return distance(v, p.x, p.y, p.z);
  }

  public double distanceTo(LXVector v) {
    return distance(this, v.x, v.y, v.z);
  }

  /*
   * Static & Dynamic Model
   */

  public void addEdge(TEEdgeModel edge) {
    this.edges.add(edge);
  }

  public void addPanel(TEPanelModel panel) {
    this.panels.add(panel);
  }

  public void nudgeToward(LXVector other, float distance) {
    lerp(other, distance);
  }

}
