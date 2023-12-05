package titanicsend.pattern.mike;

import static java.lang.Math.floorMod;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UIParameterControl;
import heronarts.glx.ui.component.UITextBox;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.Click;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import java.util.*;
import titanicsend.app.TEVirtualColor;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEVertex;
import titanicsend.pattern.TEPattern;

@LXCategory("Test")
public class ModuleEditor extends TEPattern implements UIDeviceControls<ModuleEditor> {
  private static final double MOVE_PERIOD_MSEC = 50.0;
  protected final Click mover = new Click(MOVE_PERIOD_MSEC);

  private static class Link {
    TEEdgeModel edge;
    boolean fwd;

    Link(TEEdgeModel edge, boolean fwd) {
      this.edge = edge;
      this.fwd = fwd;
    }
  }

  private Map<Integer, String> configsByModNum;
  private Map<TEEdgeModel, Integer> modNumsByEdge;
  private Map<Integer, List<TEEdgeModel>> edgesByModNum;
  private Map<Integer, List<List<Link>>> routesByModule;

  public final DiscreteParameter moduleNumber =
      new DiscreteParameter("Mod #", 1, 99).setDescription("Module ID");

  public final StringParameter moduleParts =
      new StringParameter("Parts").setDescription("Vertex/Edges in this module");

  private UI2dComponent partsErr;
  private UI2dComponent dupeErr;
  private int phase;

  public ModuleEditor(LX lx) {
    super(lx);
    this.configsByModNum = new HashMap<>();
    this.modNumsByEdge = new HashMap<>();
    this.edgesByModNum = new HashMap<>();
    this.routesByModule = new HashMap<>();
    startModulator(this.mover);
    phase = 0;
  }

  @Override
  public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, ModuleEditor pattern) {
    uiDevice.setLayout(UI2dContainer.Layout.VERTICAL);
    uiDevice.setChildSpacing(6);
    uiDevice.setContentWidth(COL_WIDTH * 2);

    UITextBox tbModParts;
    UIParameterControl loadSwitch;

    uiDevice.addChildren(
        controlLabel(ui, "Mod #"),
        newIntegerBox(moduleNumber),
        controlLabel(ui, "Parts"),
        tbModParts = new UITextBox(0, 0, COL_WIDTH * 2, 16).setParameter(moduleParts),
        this.partsErr = controlLabel(ui, "Bad parts"),
        this.dupeErr = controlLabel(ui, "Dupe"),
        new UIButton(0, 0, COL_WIDTH, 20) {
          @Override
          public void onToggle(boolean on) {
            if (on) {
              load();
            }
          }
        }.setLabel("Load").setMomentary(true));

    tbModParts.setEmptyValueAllowed(true);
    this.partsErr.setVisible(false);
    this.dupeErr.setVisible(false);

    moduleNumber.addListener(this::loadParts);
    moduleParts.addListener(this::setParts);
  }

  private void load() {
    this.partsErr.setVisible(true);
    this.configsByModNum = new HashMap<>();
    this.modNumsByEdge = new HashMap<>();
    this.edgesByModNum = new HashMap<>();
    this.routesByModule = new HashMap<>();

    Scanner s = this.modelTE.loadFile("modules.txt");

    int longestGlobalRoute = 0;
    while (s.hasNextLine()) {
      String line = s.nextLine();
      String[] tokens = line.split(":\\s*");
      assert tokens.length == 2;
      int modNum = Integer.parseInt(tokens[0]);
      tokens = tokens[1].split("\\s+");
      assert tokens.length >= 1;
      List<TEEdgeModel> edges = new ArrayList<>();
      List<List<Link>> routes = new ArrayList<>();
      int longestRoute = this.getRoutes(tokens, routes, edges);
      if (longestRoute < 0) return;

      if (longestRoute > longestGlobalRoute) longestGlobalRoute = longestRoute;

      for (TEEdgeModel edge : edges) {
        if (this.modNumsByEdge.containsKey(edge)) return;
        this.modNumsByEdge.put(edge, modNum);
      }

      if (this.routesByModule.containsKey(modNum)) return;

      this.routesByModule.put(modNum, routes);
      this.edgesByModNum.put(modNum, edges);
    }

    LX.log("Longest route is " + longestGlobalRoute);
    this.partsErr.setVisible(false);
  }

  public void loadParts(LXParameter unused) {
    Integer modNum = this.moduleNumber.getValuei();
    moduleParts.setValue(this.configsByModNum.getOrDefault(modNum, ""));
  }

  private TEVertex vertexByString(String idStr) {
    int id;
    try {
      id = Integer.parseInt(idStr);
    } catch (NumberFormatException e) {
      return null;
    }
    return this.modelTE.vertexesById.getOrDefault(id, null);
  }

  private int getRoutes(String[] tokens, List<List<Link>> routes, List<TEEdgeModel> edges) {
    int longestRoute = 0;
    for (String token : tokens) {
      int thisRouteLength = 0;
      List<Link> route = new ArrayList<>();

      token = token.strip();
      List<String> subTokens = new ArrayList<>(Arrays.asList(token.split("-")));
      if (subTokens.size() == 0) continue;
      TEVertex vCurr = vertexByString(subTokens.remove(0));
      if (vCurr == null) return -1;
      while (!subTokens.isEmpty()) {
        TEVertex vNext = vertexByString(subTokens.remove(0));
        if (vNext == null) return -1;

        boolean fwd;
        TEEdgeModel edge;
        if (vCurr.id < vNext.id) {
          fwd = true;
          edge = this.modelTE.edgesById.getOrDefault(vCurr.id + "-" + vNext.id, null);
        } else {
          fwd = false;
          edge = this.modelTE.edgesById.getOrDefault(vNext.id + "-" + vCurr.id, null);
        }
        if (edge == null) return -1;
        edges.add(edge);
        route.add(new Link(edge, fwd));
        vCurr = vNext;
        thisRouteLength += edge.points.length;
      }
      routes.add(route);
      if (thisRouteLength > longestRoute) longestRoute = thisRouteLength;
    }
    return longestRoute;
  }

  public void setParts(LXParameter unused) {
    String partStr = this.moduleParts.getString();

    this.dupeErr.setVisible(false);

    // Set to true so we can just return in the event of a problem
    this.partsErr.setVisible(true);

    List<TEEdgeModel> edges = new ArrayList<>();

    String[] tokens = partStr.split(",");
    List<List<Link>> routes = new ArrayList<>();
    int longestRoute = getRoutes(tokens, routes, edges);
    if (longestRoute < 0) return;

    int modNum = this.moduleNumber.getValuei();

    for (TEEdgeModel edge : edges) {
      int existing = this.modNumsByEdge.getOrDefault(edge, modNum);
      if (modNum != existing) {
        this.dupeErr.setVisible(true);
        return;
      }
    }

    for (TEEdgeModel edge : this.edgesByModNum.getOrDefault(modNum, new ArrayList<>())) {
      this.modNumsByEdge.remove(edge);
    }

    for (TEEdgeModel edge : edges) {
      this.modNumsByEdge.put(edge, modNum);
    }

    this.routesByModule.put(modNum, routes);
    this.edgesByModNum.put(modNum, edges);

    this.configsByModNum.put(modNum, partStr);
    this.partsErr.setVisible(false);
  }

  public void moveDots() {
    this.clearPixels();
    if (phase % 10 < 3) {
      for (TEEdgeModel edge : this.modelTE.edgesById.values()) {
        for (LXPoint point : edge.points) {
          colors[point.index] = LXColor.rgb(150, 150, 150);
        }
      }
    }
    for (Map.Entry<Integer, List<List<Link>>> entry : this.routesByModule.entrySet()) {
      int hue = ((entry.getKey() - 1) * 27) % 360;
      int i = 0;
      int numAnts = 0;

      for (List<Link> listOfLinks : entry.getValue()) {
        int j = 0;
        numAnts++;

        int trailLength = numAnts + 50;
        for (Link link : listOfLinks) {
          LXPoint[] points = link.edge.points;
          if (!link.fwd) { // Reverse the points list
            List<LXPoint> l = new ArrayList<>(Arrays.asList(points));
            Collections.reverse(l);
            points = l.toArray(new LXPoint[0]);
          }
          for (LXPoint point : points) {
            // int sat = ((i++ % 10) == (phase % 10)) ? 0 : 100;
            int sat = 100;
            int MIN_BRI = 20;
            int bri = MIN_BRI + (100 - MIN_BRI) * (400 - j) / (400 - MIN_BRI);
            if (bri > 100) bri = 100;
            if (bri < MIN_BRI) bri = MIN_BRI;
            j++;

            int ci = floorMod(i++ - phase, trailLength);
            if (ci < numAnts * 3) {
              if (ci % 3 == 0) sat = 0;
              if (ci % 3 == 1) bri = 0;
            }
            colors[point.index] = LXColor.hsb(hue, sat, bri);
          }
        }
      }
    }
    phase++;
    for (TEVertex v : this.modelTE.vertexesById.values()) {
      int unassignedEdgeCount = 0;
      for (TEEdgeModel e : v.edges) {
        if (!this.modNumsByEdge.containsKey(e)) unassignedEdgeCount++;
      }
      int color;
      if (unassignedEdgeCount == 0) color = LXColor.rgb(50, 50, 50);
      else if (unassignedEdgeCount % 3 == 0) color = LXColor.rgb(0, 150, 200);
      else if (unassignedEdgeCount % 3 == 1) color = LXColor.rgb(200, 0, 0);
      else color = LXColor.rgb(200, 180, 0);
      v.virtualColor = new TEVirtualColor(color, 0xFF);
    }
  }

  @Override
  public void run(double deltaMsec) {
    if (this.mover.click()) {
      for (int i = 0; i < mover.numLoops(); i++) {
        this.moveDots();
      }
    }
  }
}
