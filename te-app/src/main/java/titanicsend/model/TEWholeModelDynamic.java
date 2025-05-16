package titanicsend.model;

import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.transform.LXVector;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import titanicsend.dmx.model.DmxModel;
import titanicsend.pattern.jon.ModelBender;
import titanicsend.ui.UI3DManager;
import titanicsend.util.TE;

public class TEWholeModelDynamic implements TEWholeModel, LX.Listener {

  public static final String TAG_EDGE = "edge";
  public static final String TAG_PANEL = "panel";

  private final String name = "Titanic's End";

  private final LX lx;

  // Points
  private final List<LXPoint> mutablePoints = new ArrayList<LXPoint>();
  public final List<LXPoint> points = Collections.unmodifiableList(this.mutablePoints);
  private final List<LXPoint> mutableEdgePoints = new ArrayList<LXPoint>();
  public final List<LXPoint> edgePoints = Collections.unmodifiableList(this.mutableEdgePoints);
  private final List<LXPoint> mutablePanelPoints = new ArrayList<LXPoint>();
  public final List<LXPoint> panelPoints = Collections.unmodifiableList(this.mutablePanelPoints);

  // Vertexes placeholder, does not exist as a model with fixture files
  private final List<TEVertex> mutableVertexes = new ArrayList<TEVertex>();
  public final List<TEVertex> vertexes = Collections.unmodifiableList(this.mutableVertexes);
  private final Map<Integer, TEVertex> vertexesById = new TreeMap<Integer, TEVertex>();

  // Edges
  private final List<TEEdgeModel> mutableEdges = new ArrayList<TEEdgeModel>();
  public final List<TEEdgeModel> edges = Collections.unmodifiableList(this.mutableEdges);
  private final Map<LXModel, TEEdgeModel> edgeModels = new HashMap<LXModel, TEEdgeModel>();
  private final Map<String, TEEdgeModel> edgesById = new HashMap<String, TEEdgeModel>();
  public final Map<LXVector, List<TEEdgeModel>> edgesBySymmetryGroup =
      new HashMap<LXVector, List<TEEdgeModel>>();

  // Panels
  private final List<TEPanelModel> mutablePanels = new ArrayList<TEPanelModel>();
  public final List<TEPanelModel> panels = Collections.unmodifiableList(this.mutablePanels);
  private final Map<LXModel, TEPanelModel> panelModels = new HashMap<LXModel, TEPanelModel>();
  private final Map<String, TEPanelModel> panelsById = new HashMap<String, TEPanelModel>();
  private final Map<TEPanelSection, Set<TEPanelModel>> panelsBySection =
      new HashMap<TEPanelSection, Set<TEPanelModel>>();

  // Lasers
  public final Map<String, TELaserModel> lasersById = new HashMap<String, TELaserModel>();
  private final List<TELaserModel> mutableLasers = new ArrayList<TELaserModel>();
  public final List<TELaserModel> lasers = Collections.unmodifiableList(this.mutableLasers);

  // Beacons
  private final List<DmxModel> mutableBeacons = new ArrayList<DmxModel>();
  public final List<DmxModel> beacons = Collections.unmodifiableList(this.mutableBeacons);
  private final HashMap<String, DmxModel> beaconsById = new HashMap<String, DmxModel>();

  // DJ Lights
  private final List<DmxModel> mutableDjLights = new ArrayList<DmxModel>();
  public final List<DmxModel> djLights = Collections.unmodifiableList(this.mutableDjLights);
  private final HashMap<String, DmxModel> djLightsById = new HashMap<String, DmxModel>();

  // All DMX models
  private final List<DmxModel> mutableDmxModels = new ArrayList<DmxModel>();
  public final List<DmxModel> dmxModels = Collections.unmodifiableList(this.mutableDmxModels);

  private final List<LXModel> mutableChildren = new ArrayList<LXModel>();
  public LXModel[] children = new LXModel[0];

  // Placeholder until TEPanelSections are updated
  private final List<LXPoint> emptyPointsList = new ArrayList<LXPoint>();

  public TEWholeModelDynamic(LX lx) {
    this.lx = lx;

    lx.addListener(this);

    for (TEPanelSection section : TEPanelSection.values()) {
      this.panelsBySection.put(section, new HashSet<TEPanelModel>());
    }
  }

  /** Use tags and metadata to identify TE-specific LXModels within the overall model. */
  @Override
  public void modelGenerationChanged(LX lx, LXModel model) {
    Set<LXModel> expired;
    List<LXModel> tagged;

    // Find Edges
    expired = new HashSet<LXModel>(this.edgeModels.keySet());
    tagged = model.sub(TAG_EDGE);
    List<TEEdgeModel> newEdges = new ArrayList<TEEdgeModel>();
    for (LXModel m : tagged) {
      if (this.edgeModels.containsKey(m)) {
        expired.remove(m);
      } else {
        // Find vertexes using fixture metadata
        int iv0, iv1;
        try {
          iv0 = Integer.parseInt(m.meta(TEEdgeModel.META_V0));
        } catch (NumberFormatException x) {
          LX.warning("TEEdgeModel does not contain valid metadata for vertex 0: " + m);
          continue;
        }
        try {
          iv1 = Integer.parseInt(m.meta(TEEdgeModel.META_V1));
        } catch (NumberFormatException x) {
          LX.warning("TEEdgeModel does not contain valid metadata for vertex 1: " + m);
          continue;
        }
        TEVertex v0 = findOrCreateVertex(iv0);
        TEVertex v1 = findOrCreateVertex(iv1);
        // Create edge
        TEEdgeModel edge = new TEEdgeModel(m, v0, v1);
        newEdges.add(edge);
        this.mutableEdges.add(edge);
        this.edgeModels.put(m, edge);
        // Add edge to vertexes
        v0.addEdge(edge);
        v1.addEdge(edge);
      }
    }

    // Remove any previous edges that no longer exist
    for (LXModel m : expired) {
      TEEdgeModel edge = this.edgeModels.remove(m);
      this.mutableEdges.remove(edge);
      edge.v0.remove(edge);
      edge.v1.remove(edge);
      // TODO: remove from edgesBySymmetryGroup
    }
    expired.clear();

    // Rebuild edgesById, because an id may have stayed the same or may have changed objects
    this.edgesById.clear();
    for (TEEdgeModel edge : this.edges) {
      this.edgesById.put(edge.getId(), edge);
    }

    // Find Panels
    expired = new HashSet<LXModel>(this.panelModels.keySet());
    tagged = model.sub(TAG_PANEL);
    List<TEPanelModel> newPanels = new ArrayList<TEPanelModel>();
    for (LXModel m : tagged) {
      if (this.panelModels.containsKey(m)) {
        expired.remove(m);
      } else {
        // Find vertexes using fixture metadata
        int iv0, iv1, iv2;
        try {
          iv0 = Integer.parseInt(m.meta(TEPanelModel.META_V0));
        } catch (NumberFormatException x) {
          LX.warning("TEPanelModel does not contain valid metadata for vertex 0.");
          continue;
        }
        try {
          iv1 = Integer.parseInt(m.meta(TEPanelModel.META_V1));
        } catch (NumberFormatException x) {
          LX.warning("TEPanelModel does not contain valid metadata for vertex 1.");
          continue;
        }
        try {
          iv2 = Integer.parseInt(m.meta(TEPanelModel.META_V2));
        } catch (NumberFormatException x) {
          LX.warning("TEPanelModel does not contain valid metadata for vertex 2.");
          continue;
        }
        TEVertex v0 = findOrCreateVertex(iv0);
        TEVertex v1 = findOrCreateVertex(iv1);
        TEVertex v2 = findOrCreateVertex(iv2);
        // Create panel
        TEPanelModel panel = new TEPanelModel(m, v0, v1, v2);
        newPanels.add(panel);
        this.mutablePanels.add(panel);
        this.panelModels.put(m, panel);
        // Add panel to vertexes
        v0.addPanel(panel);
        v1.addPanel(panel);
        v2.addPanel(panel);
      }
    }

    // Remove any previous panels that no longer exist
    for (LXModel m : expired) {
      TEPanelModel panel = this.panelModels.remove(m);
      this.mutablePanels.remove(panel);
      panel.v0.remove(panel);
      panel.v1.remove(panel);
      panel.v2.remove(panel);
      // TODO: remove from panelsBySection
    }
    expired.clear();

    // Rebuild lookup by id
    this.panelsById.clear();
    for (TEPanelModel panel : this.panels) {
      this.panelsById.put(panel.getId(), panel);
    }

    // Rebuild lists of points
    this.mutableEdgePoints.clear();
    for (TEEdgeModel edge : this.edges) {
      this.mutableEdgePoints.addAll(edge.model.getPoints());
    }

    this.mutablePanelPoints.clear();
    for (TEPanelModel panel : this.panels) {
      this.mutablePanelPoints.addAll(panel.model.getPoints());
    }

    this.mutablePoints.clear();
    this.mutablePoints.addAll(this.edgePoints);
    this.mutablePoints.addAll(this.panelPoints);

    // Remove vertexes with no connections
    for (int i = this.mutableVertexes.size() - 1; i >= 0; i--) {
      TEVertex vertex = this.mutableVertexes.get(i);
      if (vertex.edges.size() == 0 && vertex.panels.size() == 0) {
        this.mutableVertexes.remove(i);
        this.vertexesById.remove(vertex.id);
      }
    }

    // Rebuild Edge Connections
    for (TEEdgeModel edge : this.edges) {
      edge.rebuildConnections(this.edges, this.panels);
    }

    // Rebuild Panel Connections
    // Set edges on all panels prior to panel->panel connections.
    for (TEPanelModel panel : this.panels) {
      panel.reconnectEdges(this.edges);
    }
    // Build panel->panel connections now that edges are known
    for (TEPanelModel panel : this.panels) {
      panel.reconnectPanels(this.panels);
    }

    // Find all DMX models (Beacons, DJ Lights)
    // TODO

    // There are no longer LXModels for lasers

    // Rebuild list of TE models
    this.mutableChildren.clear();
    this.mutableChildren.addAll(this.edgeModels.keySet());
    this.mutableChildren.addAll(this.panelModels.keySet());
    this.children = this.mutableChildren.toArray(new LXModel[0]);

    // adjust model geometry to improve texture mapping of ends in all views.
    ModelBender mb = new ModelBender();
    boolean rebuildViews = mb.adjustEndGeometry(this, lx.getModel());

    // if the TE main car is part of this model, iterate over the views
    // and rebuild them (with the "adjusted" end geometry)
    // To do this, we call a method in heronarts.lx.structure.view.LXViewEngine
    //
    // NOTE that this method, though public, is mainly used internally by Chromatik,
    // and its behavior might change without warning in a future version.
    if (rebuildViews) {
      lx.structure.views.modelGenerationChanged(lx, lx.getModel());
    }

    // restore the model to its original state
    mb.restoreModel(this, lx.getModel());

    /* TE.log("Model changed. Found " +
    this.edges.size() + " edges, " +
    this.panels.size() + " panels, " +
    this.edgePoints.size() + " edge points, " +
    this.panelPoints.size() + " panel points, " +
    this.points.size() + " total points"); */

    for (TEModelListener listener : this.listeners) {
      listener.modelTEChanged(this);
    }

    // Update 3D ui elements (now with extra thread safety!)
    UI3DManager.current.rebuild();

    // Run the garbage collector to prevent buildup of old-generation objects?
  }

  private TEVertex findOrCreateVertex(int v) {
    TEVertex vertex = this.vertexesById.get(v);
    if (vertex == null) {
      vertex = new TEVertex(v);
      this.mutableVertexes.add(vertex);
      this.vertexesById.put(v, vertex);
    }
    return vertex;
  }

  @Override
  public int sizeDmx() {
    return this.dmxModels.size();
  }

  @Override
  public List<DmxModel> getDmxModels() {
    return this.dmxModels;
  }

  @Override
  public void clearBeacons() {
    // Fast hack
    this.mutableDmxModels.clear();
    this.mutableBeacons.clear();
  }

  @Override
  public void addBeacon(DmxModel dmxModel) {
    TE.log("Adding beacon! %s", dmxModel);
    // Fast hack
    this.mutableDmxModels.add(dmxModel);
    this.mutableBeacons.add(dmxModel);
    dmxModel.index = this.mutableBeacons.indexOf(dmxModel);
  }

  private final List<DmxWholeModelListener> dmxWholeModelListeners =
      new ArrayList<DmxWholeModelListener>();

  @Override
  public void addDmxListener(DmxWholeModelListener listener) {
    this.dmxWholeModelListeners.add(listener);
  }

  @Override
  public void removeDmxListener(DmxWholeModelListener listener) {
    this.dmxWholeModelListeners.remove(listener);
  }

  // haaack
  @Override
  public void notifyDmxWholeModelListeners() {
    for (DmxWholeModelListener listener : this.dmxWholeModelListeners) {
      listener.dmxModelsChanged(this.dmxModels);
    }
  }

  // Note(JKB): Gap points are no longer loaded into the model. This can be removed.
  @Deprecated
  @Override
  public boolean isGapPoint(LXPoint p) {
    // There are no LXPoints for gap pixels in this wonderful dynamic future.
    return false;
  }

  @Deprecated
  private final int[] gapPointIndices = new int[0]; // Not used with dynamic model

  @Deprecated
  @Override
  public int[] getGapPointIndices() {
    return this.gapPointIndices;
  }

  @Override
  public List<LXPoint> getPoints() {
    return this.points;
  }

  @Override
  public List<LXPoint> getPointsBySection(TEPanelSection section) {
    // TODO
    return this.emptyPointsList;
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
    // TODO
    return emptyPointsList;
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
  public TEVertex getVertex(int vertexId) {
    return this.vertexesById.get(vertexId);
  }

  @Override
  public List<TEVertex> getVertexes() {
    return this.vertexes;
  }

  @Override
  public boolean hasPanel(String panelId) {
    return this.panelsById.containsKey(panelId);
  }

  @Override
  public TEPanelModel getPanel(String panelId) {
    return this.panelsById.get(panelId);
  }

  @Override
  public List<TEPanelModel> getPanels() {
    return this.panels;
  }

  @Override
  public boolean hasEdge(String edgeId) {
    return this.edgesById.containsKey(edgeId);
  }

  @Override
  public TEEdgeModel getEdge(String edgeId) {
    return this.edgesById.get(edgeId);
  }

  @Override
  public List<TEEdgeModel> getEdges() {
    return this.edges;
  }

  @Override
  public Map<LXVector, List<TEEdgeModel>> getEdgesBySymmetryGroup() {
    return this.edgesBySymmetryGroup;
  }

  @Override
  public List<TELaserModel> getLasers() {
    return this.lasers;
  }

  @Override
  public List<DmxModel> getBeacons() {
    return this.beacons;
  }

  @Override
  public List<DmxModel> getDjLights() {
    return this.djLights;
  }

  @Override
  public LXModel[] getChildren() {
    return this.children;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public boolean isStatic() {
    return false;
  }

  private final List<TEModelListener> listeners = new ArrayList<TEModelListener>();

  public TEWholeModel addListener(TEModelListener listener) {
    Objects.requireNonNull(listener);
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException("Cannot add duplicate TEListener: " + listener);
    }
    this.listeners.add(listener);
    return this;
  }

  public TEWholeModel removeListener(TEModelListener listener) {
    if (!this.listeners.contains(listener)) {
      throw new IllegalStateException("Cannot remove non-registered TEListener: " + listener);
    }
    this.listeners.remove(listener);
    return this;
  }

  public void dispose() {
    this.lx.removeListener(this);
    this.listeners.clear();
  }
}
