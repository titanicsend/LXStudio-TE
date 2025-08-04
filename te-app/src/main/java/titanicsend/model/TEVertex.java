package titanicsend.model;

import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import titanicsend.app.TEVirtualColor;
import titanicsend.util.TEMath;

public class TEVertex extends LXVector {

  public final int id;
  private final Set<TEEdgeModel> mutableEdges = new HashSet<TEEdgeModel>();
  public final Set<TEEdgeModel> edges = Collections.unmodifiableSet(this.mutableEdges);
  private final Set<TEPanelModel> mutablePanels = new HashSet<TEPanelModel>();
  public final Set<TEPanelModel> panels = Collections.unmodifiableSet(this.mutablePanels);

  // Set to non-null and the virtual display will shade vertex's sphere
  public TEVirtualColor virtualColor;

  /** Static model constructor (2022-23) */
  public TEVertex(LXVector vector, int id) {
    super(vector);
    this.id = id;
    this.virtualColor = new TEVirtualColor(255, 255, 255, 255);
  }

  /**
   * Dynamic model constructor (2024+)
   *
   * <p>In this case TEVertex is just a label referenced by multiple edges & panels. It does not
   * have exact coordinates and therefore does not need to extend LXVector.
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
    this.mutableEdges.add(edge);
  }

  public void addPanel(TEPanelModel panel) {
    this.mutablePanels.add(panel);
  }

  public boolean remove(TEEdgeModel edge) {
    return this.mutableEdges.remove(edge);
  }

  public boolean remove(TEPanelModel panel) {
    return this.mutablePanels.remove(panel);
  }

  public void nudgeToward(LXVector other, float distance) {
    lerp(other, distance);
  }
}
