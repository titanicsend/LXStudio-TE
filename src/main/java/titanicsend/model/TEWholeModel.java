package titanicsend.model;

import java.util.Collection;
import java.util.List;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import titanicsend.dmx.model.DmxModel;
import titanicsend.dmx.model.DmxWholeModel;

/**
 * As of Spring 2024 TEWholeModel has been converted to an interface,
 * allowing access to TE-specific collections for both static model
 * and dynamic model runtimes.
 */
public interface TEWholeModel extends DmxWholeModel {

  public abstract boolean isGapPoint(LXPoint p);
  public abstract int[] getGapPointIndices();
  
  public abstract List<LXPoint> getPoints();
  public abstract List<LXPoint> getPointsBySection(TEPanelSection section);
  public abstract List<LXPoint> getPanelPoints();
  public abstract List<LXPoint> getEdgePoints();
  public abstract List<LXPoint> getEdgePointsBySection(TEEdgeSection section);

  public abstract float minX();
  public abstract float maxX();
  public abstract float minY();
  public abstract float maxY();
  public abstract float minZ();
  public abstract float maxZ();
  
  public abstract Collection<TEVertex> getVertexes();
  public abstract TEVertex getVertex(int vertexId);

  public abstract List<TEPanelModel> getPanels();
  public abstract TEPanelModel getPanel(String panelId);
  public abstract boolean hasPanel(String panelId);

  public abstract List<TEEdgeModel> getEdges();
  public abstract TEEdgeModel getEdge(String edgeId);
  public abstract boolean hasEdge(String edgeId);

  public abstract List<TELaserModel> getLasers();

  public abstract List<DmxModel> getBeacons();
  public abstract List<DmxModel> getDjLights();
  
  public abstract LXModel[] getChildren();

  public abstract String getName();
}
