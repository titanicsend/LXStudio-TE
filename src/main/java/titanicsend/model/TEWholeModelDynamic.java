package titanicsend.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;
import titanicsend.dmx.model.DmxModel;

public class TEWholeModelDynamic implements TEWholeModel, LX.Listener {

  private final LX lx;

  // Points
  public List<LXPoint> edgePoints; // Points belonging to edges
  public List<LXPoint> panelPoints; // Points belonging to panels
  //public List<TEBox> boxes;

  // Edges
  private final List<TEEdgeModel> mutableEdges = new ArrayList<TEEdgeModel>();
  private final List<TEEdgeModel> edges = Collections.unmodifiableList(this.mutableEdges);

  public HashMap<String, TEEdgeModel> edgesById;
  public HashMap<LXVector, List<TEEdgeModel>> edgesBySymmetryGroup;

  // Panels
  private final List<TEPanelModel> mutablePanels = new ArrayList<TEPanelModel>();
  private final List<TEPanelModel> panels = Collections.unmodifiableList(this.mutablePanels);

  public HashMap<String, TEPanelModel> panelsById;
  private final HashMap<TEPanelSection, Set<TEPanelModel>> panelsBySection;
  public HashMap<String, List<TEPanelModel>> panelsByFlavor;
  
  // Lasers
  public HashMap<String, TELaserModel> lasersById;

  // Beacons
  private final List<DmxModel> mutableBeacons = new ArrayList<DmxModel>();
  private final List<DmxModel> beacons = Collections.unmodifiableList(this.mutableBeacons);
  private final HashMap<String, DmxModel> beaconsById = new HashMap<String, DmxModel>();

  // DJ Lights
  private final List<DmxModel> mutableDjLights = new ArrayList<DmxModel>();
  private final List<DmxModel> djLights = Collections.unmodifiableList(this.mutableDjLights);
  private final HashMap<String, DmxModel> djLightsById = new HashMap<String, DmxModel>();

  // All DMX models
  private int sizeDmx = 0;
  private int nextDmxIndex = 0;
  private final List<DmxModel> mutableDmxModels = new ArrayList<DmxModel>();
  public final List<DmxModel> dmxModels = Collections.unmodifiableList(this.mutableDmxModels);

  public TEWholeModelDynamic(LX lx) {
    this.lx = lx;

    lx.addListener(this);
  }

  @Override
  public void modelGenerationChanged(LX lx, LXModel model) {
    
  }
  
  @Override
  public int sizeDmx() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public List<DmxModel> getDmxModels() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean isGapPoint(LXPoint p) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public int[] getGapPointIndices() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<LXPoint> getPoints() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<LXPoint> getPointsBySection(TEPanelSection section) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<LXPoint> getPanelPoints() {
    return this.panelPoints;
  }

  @Override
  public List<LXPoint> getEdgePoints() {
    return this.edgePoints;
  }

  @Override
  public List<LXPoint> getEdgePointsBySection(TEEdgeSection section) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public float minX() {
    return this.lx.getModel().xMin;
  }

  @Override
  public float maxX() {
    return this.lx.getModel().xMax;
  }

  @Override
  public float minY() {
    return this.lx.getModel().yMin;
  }

  @Override
  public float maxY() {
    return this.lx.getModel().yMax;
  }

  @Override
  public float minZ() {
    return this.lx.getModel().zMin;
  }

  @Override
  public float maxZ() {
    return this.lx.getModel().zMax;
  }

  @Override
  public Collection<TEVertex> getVertexes() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TEVertex getVertex(int vertexId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<TEPanelModel> getPanels() {
    return this.panels;
  }

  @Override
  public TEPanelModel getPanel(String panelId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean hasPanel(String panelId) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<TEEdgeModel> getEdges() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public TEEdgeModel getEdge(String edgeId) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public boolean hasEdge(String edgeId) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public List<TELaserModel> getLasers() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<DmxModel> getBeacons() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<DmxModel> getDjLights() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public LXModel[] getChildren() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public String getName() {
    // TODO Auto-generated method stub
    return null;
  }

  public void dispose() {
    this.lx.removeListener(this); 
  }
}
