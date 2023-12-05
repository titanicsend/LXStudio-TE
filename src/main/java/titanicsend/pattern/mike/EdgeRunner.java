package titanicsend.pattern.mike;

import static titanicsend.util.TEColor.TRANSPARENT;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.Click;
import heronarts.lx.parameter.DiscreteParameter;
import java.util.*;
import titanicsend.app.TEVirtualColor;
import titanicsend.color.TEColorType;
import titanicsend.model.*;
import titanicsend.pattern.TEPattern;
import titanicsend.util.TEColor;

@LXCategory("Combo FG")
public class EdgeRunner extends TEPattern {
    public final DiscreteParameter numRunners =
            new DiscreteParameter("Runners", 10, 0, 50).setDescription("Number of concurrent runners");

    private static final double MOVE_PERIOD_MSEC = 1.0;
    private static final double MOVES_PER_RESET = 10000;
    private static final double RESET_PERIOD_MSEC = MOVE_PERIOD_MSEC * MOVES_PER_RESET;

    protected final Click mover = new Click(MOVE_PERIOD_MSEC);
    protected final Click spawner = new Click(2500);
    protected final Click resetter = new Click(RESET_PERIOD_MSEC);

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
            registerColor("Runner", "runnerColor", TEColorType.PRIMARY, "Color of the runner dots");

    public final LinkedColorParameter trailColor =
            registerColor("Trail", "trailColor", TEColorType.SECONDARY, "Color of the trail they leave behind");

    public final LinkedColorParameter fillColor =
            registerColor("Fill", "fillColor", TEColorType.SECONDARY_BACKGROUND, "Color to fill the panels with");

    public EdgeRunner(LX lx) {
        super(lx);
        this.edgeLastVisit = new HashMap<TEEdgeModel, Integer>();
        this.pointLastVisit = new HashMap<LXPoint, Integer>();
        this.runners = new ArrayList<>();
        startModulator(this.mover);
        startModulator(this.spawner);
        startModulator(this.resetter);
        this.spawner.fire();
        this.moveNumber = 0;
        this.panelData = new HashMap<>();
        for (TEPanelModel panel : modelTE.panelsById.values()) {
            if (!panel.panelType.equals(TEPanelModel.LIT)) continue;
            int numEdgePixels = panel.e0.points.length + panel.e1.points.length + panel.e2.points.length;
            PanelData pd = new PanelData(numEdgePixels);
            this.panelData.put(panel, pd);
        }
        this.reset();
    }

    private void reset() {
        this.edgeLastVisit.clear();
        this.pointLastVisit.clear();
        for (PanelData pd : this.panelData.values()) {
            pd.litEdgePixels = 0;
        }

        for (TEVertex v : modelTE.vertexesById.values()) {
            // Initialize all vertexes to gray
            v.virtualColor = new TEVirtualColor(50, 50, 50, 255);
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
    public void run(double deltaMsec) {
        if (this.resetter.click()) this.reset();

        updateVirtualColors(deltaMsec);
        if (this.mover.click()) {
            for (int i = 0; i < mover.numLoops(); i++)
                for (Runner runner : this.runners) {
                    this.mark(runner);
                    this.move(runner);
                }
        }
        if (this.spawner.click()) {
            while (this.runners.size() > this.numRunners.getValuei()) {
                this.runners.remove(0);
            }
            if (this.runners.size() < this.numRunners.getValuei()) {
                // Spawn from Edge 37-44, if it exists, or else just whatever comes first
                TEEdgeModel edge = modelTE.edgesById.get("37-44");
                if (edge == null) edge = modelTE.edgesById.values().iterator().next();
                this.runners.add(new Runner(edge));
            }
        }

        int runnerColor = this.runnerColor.calcColor();
        int trailColor = this.trailColor.calcColor();
        int fillColor = this.fillColor.calcColor();

        for (LXPoint point : modelTE.edgePoints) {
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
        for (Map.Entry<String, TEPanelModel> entry : modelTE.panelsById.entrySet()) {
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
