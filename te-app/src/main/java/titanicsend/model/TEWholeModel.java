package titanicsend.model;

import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;
import java.util.List;
import java.util.Map;
import titanicsend.dmx.model.DmxModel;
import titanicsend.dmx.model.DmxWholeModel;

/**
 * TE-specific model information
 *
 * <p>As of Spring 2024 TEWholeModel has been converted to an interface, allowing access to
 * TE-specific collections for both static model and dynamic model runtimes.
 */
public interface TEWholeModel extends DmxWholeModel {

  public static interface TEModelListener {
    /**
     * TEWholeModel collections have been rebuilt following a change to the LXModel hierarchy. All
     * users of these collections and objects should re-initialize as the underlying model objects
     * may no longer be valid.
     */
    public void modelTEChanged(TEWholeModel model);
  }

  public TEWholeModel addListener(TEModelListener listener);

  public TEWholeModel removeListener(TEModelListener listener);

  @Deprecated
  public abstract boolean isGapPoint(LXPoint p);

  @Deprecated
  public abstract int[] getGapPointIndices();

  public abstract List<LXPoint> getPoints();

  public abstract List<LXPoint> getPointsBySection(TEPanelSection section);

  public abstract List<LXPoint> getEdgePoints();

  public abstract List<LXPoint> getEdgePointsBySection(TEEdgeSection section);

  public abstract List<LXPoint> getPanelPoints();

  public abstract float minX();

  public abstract float maxX();

  public abstract float minY();

  public abstract float maxY();

  public abstract float minZ();

  public abstract float maxZ();

  public abstract TEVertex getVertex(int vertexId);

  public abstract List<TEVertex> getVertexes();

  public abstract boolean hasEdge(String edgeId);

  public abstract TEEdgeModel getEdge(String edgeId);

  public abstract List<TEEdgeModel> getEdges();

  public abstract Map<LXVector, List<TEEdgeModel>> getEdgesBySymmetryGroup();

  public abstract boolean hasPanel(String panelId);

  public abstract TEPanelModel getPanel(String panelId);

  public abstract List<TEPanelModel> getPanels();

  public abstract List<DmxModel> getBeacons();

  public abstract List<DmxModel> getDjLights();

  public abstract List<TELaserModel> getLasers();

  public abstract LXModel[] getChildren();

  public abstract String getName();

  /**
   * During the transition period some components need to know which model is in use such as for
   * different axis orientations. Avoid new dependencies on this method.
   */
  public abstract boolean isStatic();
}
