  package titanicsend.app.director;

import java.util.ArrayList;
import java.util.List;

import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;

public enum Directable {
  TE("te", "TE"),
  PANELS("panels", "Panels");
  
  private final String path;
  private final String label;

  private static final List<LXPoint> emptyList = new ArrayList<LXPoint>();

  private Directable(String path, String label) {
    this.path = path;
    this.label = label;
  }
  
  /**
   * Subclasses should override and return a list of points
   * that are part of this Directable.
   */
  public List<LXPoint> getPoints(LXModel model) {
    return emptyList;
  }
}