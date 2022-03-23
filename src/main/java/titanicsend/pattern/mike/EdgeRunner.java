package titanicsend.pattern.mike;

import java.util.*;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;

import titanicsend.app.TEVirtualColor;
import titanicsend.model.*;
import titanicsend.pattern.PeriodicPattern;
import titanicsend.util.TEColor;

import static titanicsend.util.TEColor.TRANSPARENT;

@LXCategory("Combo FG")
public class EdgeRunner extends PeriodicPattern {
  private static final int NUM_RUNNERS = 10;  // TODO: Make this a config variable in the UI, and gracefully handle changes
  private static final double MOVE_PERIOD_MSEC = 5.0;

  // Useful data related to LIT panels
  private static class PanelData {
    int numEdgePixels; // Total number of pixels within the Edges of this panel
    int litEdgePixels; // Number that are lit up

    PanelData(int numEdgePixels) {
      assert numEdgePixels > 0;
      this.numEdgePixels = numEdgePixels;
      this.litEdgePixels = 0;
    }
  }

  private static class Runner {
    private TEEdgeModel currentEdge;
    private int currentPoint;
    private boolean fwd;

    Runner(TEEdgeModel currentEdge) {
      this.currentEdge = currentEdge;
      this.currentPoint = 0;
      this.fwd = true;
    }
  }

  private final HashMap<TEEdgeModel, Integer> edgeLastVisit;
  private final HashMap<LXPoint, Integer> pointLastVisit;
  private final HashMap<TEPanelModel, PanelData> panelData;
  private final List<Runner> runners;
  private int moveNumber;

  public final LinkedColorParameter runnerColor =
          registerColor("Runner", "runnerColor", ColorType.EDGE,
                  "Color of the runner dots");

  public final LinkedColorParameter trailColor =
          registerColor("Trail", "trailColor", ColorType.SECONDARY,
                  "Color of the trail they leave behind");

  public final LinkedColorParameter fillColor =
          registerColor("Fill", "fillColor", ColorType.PANEL,
                  "Color to fill the panels with");

  public EdgeRunner(LX lx) {
    super(lx);
    super.register(this::update, MOVE_PERIOD_MSEC);
    this.edgeLastVisit = new HashMap<TEEdgeModel, Integer>();
    this.pointLastVisit = new HashMap<LXPoint, Integer>();
    this.runners = new ArrayList<>();

    Iterator<TEEdgeModel> edges = model.edgesById.values().iterator();
    for (int i = 0; i < NUM_RUNNERS; i++) {
      try {
        this.runners.add(new Runner(edges.next()));
      } catch (NoSuchElementException e) {
        // TODO: Handle this case (where there are more runners than edges)
      }
    }
    this.moveNumber = 0;
    for (TEVertex v : model.vertexesById.values()) {
      // Initialize all vertexes to gray
      v.virtualColor = new TEVirtualColor(50, 50, 50, 255);
    }
    this.panelData = new HashMap<>();
    for (TEPanelModel panel : model.panelsById.values()) {
      if (!panel.panelType.equals(TEPanelModel.LIT)) continue;
      int numEdgePixels = panel.e0.points.length + panel.e1.points.length + panel.e2.points.length;

      PanelData pd = new PanelData(numEdgePixels);
      this.panelData.put(panel, pd);
    }
  }

  // Select the edge least-recently visited (unless a subclass overrides this)
  public TEEdgeModel selectEdge(Set<TEEdgeModel> choices) {
    int oldestMove = this.moveNumber;
    TEEdgeModel winner = null;

    assert choices.size() > 0;
    for (TEEdgeModel e : choices) {
      int lastVisit = this.edgeLastVisit.getOrDefault(e, -1);
      if (lastVisit < oldestMove) {
        oldestMove = lastVisit;
        winner = e;
      }
    }

    assert winner != null;
    return winner;
  }

  // This is a hook for subclasses to do something with the current point
  // in between moves. By default, we just note when it was last visited.
  public void mark(Runner runner) {
    LXPoint currentPoint = runner.currentEdge.points[runner.currentPoint];
    assert currentPoint != null;
    if (this.pointLastVisit.getOrDefault(currentPoint, -1) < 0) {
      // First visit to this point. Increment neighbor Panels' lit-edge-pixel count.
      for (TEPanelModel panel : runner.currentEdge.connectedPanels) {
        if (panel.panelType.equals(TEPanelModel.LIT)) {
          this.panelData.get(panel).litEdgePixels++;
        }
      }
    }
    this.pointLastVisit.put(currentPoint, this.moveNumber);
  }

  // Move along the current edge until we reach the end. Use selectEdge()
  // to pick a new one at that point; unless overridden, it picks the
  // path least recently visited.
  public void move(Runner runner) {
    this.edgeLastVisit.put(runner.currentEdge, ++this.moveNumber);
    TEVertex reachedVertex = null;

    if (runner.fwd) {
      if (++runner.currentPoint >= runner.currentEdge.points.length) {
        reachedVertex = runner.currentEdge.v1;
      }
    } else {
      if (--runner.currentPoint < 0) {
        reachedVertex = runner.currentEdge.v0;
      }
    }

    // We're still in the middle of an Edge
    if (reachedVertex == null) return;

    reachedVertex.virtualColor = new TEVirtualColor(0, 100, 255, 255);

    // We've reached a Vertex
    Set<TEEdgeModel> connectedEdges = reachedVertex.edges;

    TEEdgeModel newEdge = selectEdge(connectedEdges);
    runner.currentEdge = newEdge;
    if (newEdge.v0 == reachedVertex) {
      runner.fwd = true;
      runner.currentPoint = 0;
    } else {
      runner.fwd = false;
      runner.currentPoint = newEdge.points.length - 1;
    }
  }

  @Override
  public void runHook(double deltaMsec) {
    updateVirtualColors(deltaMsec);
  }

  public void update() {
    for (Runner runner : this.runners){
      this.mark(runner);
      this.move(runner);
    }

    int runnerColor = this.runnerColor.calcColor();
    int trailColor = this.trailColor.calcColor();
    int fillColor = this.fillColor.calcColor();

    for (LXPoint point : model.edgePoints) {
      int lastVisit = this.pointLastVisit.getOrDefault(point, -1);
      int color;
      if (lastVisit == -1) {
        color = TRANSPARENT;
      } else {
        int age = this.moveNumber - lastVisit;
        if (age <= 15) color = LXColor.WHITE;
        else if (age <= 50) color = runnerColor;
        else if (age <= 150) {
          // Age is 51-150, so alphaPct is 99-0
          int alphaPct = 150 - age;
          int alpha = 0xFF * alphaPct / 100;
          color = LXColor.lerp(trailColor, runnerColor, alpha);
        } else {
          color = trailColor;
        }
      }
      colors[point.index] = color;
    }
    for (Map.Entry<String, TEPanelModel> entry : model.panelsById.entrySet()) {
      TEPanelModel panel = entry.getValue();
      if (panel.panelType.equals(TEPanelModel.SOLID)) {
        assert panel.points.length == 1;
        LXPoint point = panel.points[0];
        int numVisitedEdges = 0;
        if (edgeLastVisit.getOrDefault(panel.e0, -1) >= 0) numVisitedEdges++;
        if (edgeLastVisit.getOrDefault(panel.e1, -1) >= 0) numVisitedEdges++;
        if (edgeLastVisit.getOrDefault(panel.e2, -1) >= 0) numVisitedEdges++;
        int alpha = 0xFF * numVisitedEdges / 3;
        colors[point.index] = TEColor.reAlpha(fillColor, alpha);
      } else if (panel.panelType.equals(TEPanelModel.LIT)) {
        PanelData panelData = this.panelData.get(panel);
        double litFraction = (double) panelData.litEdgePixels / panelData.numEdgePixels;
        for (TEPanelModel.LitPointData lpd : panel.litPointData) {
          int color;
          if (lpd.radiusFraction <= litFraction) {
            color = fillColor;
          } else {
            color = TRANSPARENT;
          }
          colors[lpd.point.index] = color;
        }
      }
    }
  }
}
