package titanicsend.pattern.jeff;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.p4lx.ui.UI2dContainer;
import heronarts.p4lx.ui.component.UITextBox;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEPattern;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;


/**
 *  Visualize signal paths
 *
 */

//TODO currently only displaying edge info

//TODO if there are multiple controllers at a single vertex, we are not differentiating as this info is not yet
//  available in the tsv. Need to import from the spreadsheet.
@LXCategory("Test")
public class SignalDebugger extends TEPattern implements UIDeviceControls<SignalDebugger> {

    public final DiscreteParameter cycleAllParameter =
            new DiscreteParameter("Cycle All", 0, 100)
                    .setDescription("Cycles through all signal chains sorted by controller vertex");

    public final DiscreteParameter vertexSelectParameter =
            new DiscreteParameter("Controller Vertex", 0, 150)
                    .setDescription("Show all signal routes stemming from the controller at the designated vertex id");

    public final BooleanParameter showLowPriParameter =
            new BooleanParameter("LowPri", false)
                    .setDescription("Display low priority (year 2) edges and panels");

    public final StringParameter edgeIdParameter = new StringParameter("EdgeId")
            .setDescription("Show signal route that edge of designated id belongs to");

    private static final List<Integer> CHANNEL_COLORS = List.of(
            LXColor.hsba(175, 100, 50, 100),
            LXColor.hsba(300, 100, 50, 100),
            LXColor.hsba(160, 100, 50, 100),
            LXColor.hsba(30, 100, 50, 100),
            LXColor.BLUE, LXColor.RED, LXColor.GREEN, LXColor.WHITE
    );

    //pixels between marching ants
    private static final int ANT_SPACING = 10;
    private static final int LOWEST_PANEL_PRIORITY = 12;

    private final Map<Integer, List<String>> controllerVertexToPanelIds = new HashMap<>();
    private final Map<Integer, List<List<ChainedEdge>>> controllerVertexToEdgeRoutes = new HashMap<>();
    private List<List<ChainedEdge>> allEdgeRoutes;
    private List<List<ChainedEdge>> activeEdgeRoutes = List.of();
    private List<String> activePanelIds = List.of();

    public SignalDebugger(LX lx) {
        super(lx);
        addParameter("vertexSelect", vertexSelectParameter);
        addParameter("edgeId", edgeIdParameter);
        addParameter("cycleAll", cycleAllParameter);
        addParameter("showLowPri", showLowPriParameter);

        loadChains();
        loadPanelData();
    }

    public void run(double deltaMs) {
        clearEdges();
        Iterator<Integer> colorIter = CHANNEL_COLORS.iterator();

        for (int i = 0; i < activeEdgeRoutes.size(); i++) {
            int routeColor = colorIter.next();
            ChainedEdge prev = null;
            for (ChainedEdge chainedEdge : activeEdgeRoutes.get(i)) {
                boolean backwards;
                if (prev == null) {
                    backwards = chainedEdge.controllerVertex == chainedEdge.edge.v1.id;
                } else {
                    backwards = chainedEdge.edge.v1.edges.contains(prev.edge);
                }

                for (int j = 0; j < chainedEdge.edge.points.length; j++) {
                    //i'm lazy...ants will march to imaginary beat
                    double antProgress = getTempo().basis();
                    if (backwards) {
                        antProgress = 1 - antProgress;
                    }
                    if (j % ANT_SPACING != Math.floor(ANT_SPACING * antProgress)) {
                        TEEdgeModel.Point point = chainedEdge.edge.points[j];
                        colors[point.index] = routeColor;
                    }
                }
                prev = chainedEdge;
            }
        }

        for (String activePanelId : activePanelIds) {
            int routeColor = colorIter.next();
            List<LXPoint> activePoints = model.panelsById.get(activePanelId).getPoints();
            for (LXPoint point : activePoints) {
                colors[point.index] = routeColor;
            }
        }
    }

    protected void loadChains() {
        controllerVertexToEdgeRoutes.clear();

        Scanner s;
        try {
            File f = new File("resources/vehicle/edge_signal_paths.tsv");
            s = new Scanner(f);
        } catch (FileNotFoundException e) {
            throw new Error("edge_signal_paths.tsv not found");
        }

        Map<String, ChainedEdge> edgesById = new HashMap<>();
        String headerLine = s.nextLine();
        assert headerLine.endsWith("Pixels");

        Map<String, String> edgeSignalTo = new HashMap<>();
        while (s.hasNextLine()) {
            String line = s.nextLine();
            String[] tokens = line.split("\\t");
            assert tokens.length == 5;
            if (!showLowPriParameter.getValueb() && tokens[3].equals("Low")) {
                continue;
            }
            ChainedEdge chainedEdge = new ChainedEdge(tokens[0], tokens[1], tokens[2], tokens[3] );
            edgesById.put(chainedEdge.edge.getId(), chainedEdge);
            edgeSignalTo.put(chainedEdge.signalFrom, chainedEdge.edge.getId());
        }

        for (ChainedEdge chainedEdge : edgesById.values()) {
            if (chainedEdge.controllerVertex != 0) {
                controllerVertexToEdgeRoutes.putIfAbsent(chainedEdge.controllerVertex, new ArrayList<>());
                List<ChainedEdge> route = new LinkedList<>();
                controllerVertexToEdgeRoutes.get(chainedEdge.controllerVertex).add(route);
                route.add(chainedEdge);
                while (edgeSignalTo.get(chainedEdge.edge.getId()) != null) {
                    chainedEdge = edgesById.get(edgeSignalTo.get(chainedEdge.edge.getId()));
                    route.add(chainedEdge);
                }
            }
        }
        allEdgeRoutes = controllerVertexToEdgeRoutes.values().stream().flatMap(List::stream).collect(Collectors.toList());
        allEdgeRoutes.sort(Comparator.comparingInt(o -> o.get(0).controllerVertex));
    }

    private void loadPanelData() {
        controllerVertexToPanelIds.clear();

        Scanner s;
        try {
            File f = new File("resources/vehicle/panel_signal_paths.tsv");
            s = new Scanner(f);
        } catch (FileNotFoundException e) {
            throw new Error("edge_signal_paths.tsv not found");
        }

        String headerLine = s.nextLine();
        assert headerLine.endsWith("IDs");

        while (s.hasNextLine()) {
            String line = s.nextLine();
            String[] tokens = line.split("\\t");
            assert tokens.length == 11;

            String panelId = tokens[0];
            String type = tokens[4];
            int controllerVertex = Integer.parseInt(tokens[6]);
            int priority = Integer.parseInt(tokens[9]);

            if (type.equals("solid") || (!showLowPriParameter.getValueb() && priority > LOWEST_PANEL_PRIORITY)) {
                continue;
            }
            controllerVertexToPanelIds.putIfAbsent(controllerVertex, new LinkedList<>());
            controllerVertexToPanelIds.get(controllerVertex).add(panelId);
        }
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
        super.onParameterChanged(parameter);
        if (parameter.equals(vertexSelectParameter)) {
            activeEdgeRoutes = controllerVertexToEdgeRoutes.get((int) vertexSelectParameter.getValue());
            activePanelIds = controllerVertexToPanelIds.get((int) vertexSelectParameter.getValue());
            if (activePanelIds == null) {
                activePanelIds = List.of();
            }
        } else if (parameter.equals(edgeIdParameter)) {
            activeEdgeRoutes = null;
            for (List<List<ChainedEdge>> controllerRoutes : controllerVertexToEdgeRoutes.values()) {
                for (List<ChainedEdge> route : controllerRoutes) {
                    for (ChainedEdge chainedEdge : route) {
                        if (chainedEdge.edge.getId().equals(edgeIdParameter.getString())) {
                            vertexSelectParameter.setValue(route.get(0).controllerVertex);
                        }
                    }
                }
            }
            cycleAllParameter.setValue(0);
        } else if (parameter.equals(cycleAllParameter)) {
            activeEdgeRoutes = List.of(allEdgeRoutes.get((int) cycleAllParameter.getValue()));
            vertexSelectParameter.setValue(activeEdgeRoutes.get(0).get(0).controllerVertex);
            edgeIdParameter.setValue("");
        } else if (parameter.equals(showLowPriParameter)) {
            loadChains();
            loadPanelData();
            activeEdgeRoutes = controllerVertexToEdgeRoutes.get((int) vertexSelectParameter.getValue());
        }
    }

    @Override
    public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, SignalDebugger pattern) {
        uiDevice.setLayout(UI2dContainer.Layout.VERTICAL);
        uiDevice.setChildSpacing(6);
        uiDevice.setContentWidth(COL_WIDTH);

        UITextBox tb;
        uiDevice.addChildren(
                newIntegerBox(vertexSelectParameter),
                controlLabel(ui, "ContrVert"),
                tb = new UITextBox(0, 0, COL_WIDTH, 16),
                controlLabel(ui, "EdgeId"),
                newKnob(cycleAllParameter),
                newButton(showLowPriParameter)
        );
        tb.setParameter(edgeIdParameter);
        tb.setEmptyValueAllowed(true);
    }

    private class ChainedEdge {
        public TEEdgeModel edge;
        public enum Priority { HIGH, MEDIUM, LOW }
        public Priority priority;
        public int controllerVertex;
        public String signalFrom;

        ChainedEdge(String edgeId, String signalFrom, String controllerVertex, String pri) {
            this.edge = model.edgesById.get(edgeId);
            this.signalFrom = signalFrom;
            if (!controllerVertex.equals("")) {
                this.controllerVertex = Integer.parseInt(controllerVertex);
            }
            this.priority = Priority.LOW;
            if (pri.toLowerCase().contains("medium")) this.priority = Priority.MEDIUM;
            if (pri.toLowerCase().contains("high")) this.priority = Priority.HIGH;
        }
    }
}
