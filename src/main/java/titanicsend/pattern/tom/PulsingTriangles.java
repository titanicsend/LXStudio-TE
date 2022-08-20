package titanicsend.pattern.tom;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.Tempo;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.*;
import heronarts.lx.transform.LXVector;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEVertex;
import titanicsend.pattern.TEPattern;
import titanicsend.util.PanelStriper;

import java.util.*;
import java.util.stream.Collectors;

import static titanicsend.util.TEColor.TRANSPARENT;

@LXCategory("Panel FG")
public class PulsingTriangles extends TEPattern {
    private HashMap<String, LXPoint[][]> pointMap;

    public final BooleanParameter tempoSync =
            new BooleanParameter("Sync", false)
                    .setDescription("Whether this modulator syncs to a tempo");

    public final EnumParameter<Tempo.Division> tempoDivision =
            new EnumParameter<Tempo.Division>("Division", Tempo.Division.QUARTER)
                    .setDescription("Tempo division when in sync mode");

    public final BooleanParameter tempoLock =
            new BooleanParameter("Lock", true)
                    .setDescription("Whether this modulator is locked to the beat grid or free-running");

    protected final CompoundParameter rate = (CompoundParameter)
            new CompoundParameter("Rate", .50, .01, 2)
                    .setExponent(2)
                    .setUnits(LXParameter.Units.HERTZ)
                    .setDescription("Pulse rate");

    protected final SawLFO phase = new SawLFO(0, 1, new FunctionalParameter() {
        public double getValue() {
            return 1000 / rate.getValue();
        }
    });

    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PRIMARY,
                    "Color of the triangles");

    public PulsingTriangles(LX lx) {
        super(lx);
        startModulator(this.phase);
        addParameter("tempoDivision", this.tempoDivision);
        addParameter("tempoSync", this.tempoSync);
        addParameter("tempoLock", this.tempoLock);
        addParameter("rate", this.rate);
        pointMap = buildPointMap(model.panelsById);
    }

    public void run(double deltaMs) {
        float phase = this.phase.getValuef();

        int triangleColor = this.color.calcColor();

        for (Map.Entry<String, TEPanelModel> entry : model.panelsById.entrySet()) {
            LXPoint[][] panelPoints = pointMap.get(entry.getKey());
            int litIndex = (int) (phase * (panelPoints.length - 1));
            LXPoint[] litSection = panelPoints[litIndex];

            for (int i = 0; i < entry.getValue().points.length; i++) {
                colors[entry.getValue().points[i].index] = LXColor.BLACK;
            }

            for (LXPoint point : litSection) {
                colors[point.index] = triangleColor;
            }
        }
    }

    private HashMap<String, LXPoint[][]> buildPointMap(HashMap<String, TEPanelModel> panels) {
        return panels.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> buildPanelMap(e.getValue()),
                        (a, b) -> a,
                        HashMap::new
                ));
    }


    private LXPoint[][] buildPanelMap(TEPanelModel panel) {
        ArrayList<ArrayList<LXPoint>> points = new ArrayList<ArrayList<LXPoint>>();

        TEVertex[] currentVertices = {
                new TEVertex(new LXVector(panel.v0), -1),
                new TEVertex(new LXVector(panel.v1), -1),
                new TEVertex(new LXVector(panel.v2), -1)
        };

        int i = 1;
        while (currentVertices[0].distanceTo(panel.centroid) > (PanelStriper.DISTANCE_BETWEEN_PIXELS)) {
            points.add(0, new ArrayList<LXPoint>());
            LXVector[][] edges = {
                    {currentVertices[0], currentVertices[1]},
                    {currentVertices[1], currentVertices[2]},
                    {currentVertices[0], currentVertices[2]},
            };

            for (LXPoint point : panel.points) {
                for (LXVector[] edge : edges) {
                    if (distanceBetweenPointAndLineSegment(edge[0], edge[1], point) < 2 * PanelStriper.DISTANCE_BETWEEN_PIXELS) {
                        points.get(0).add(point);
                    }
                }
            }

            for (TEVertex vertex : currentVertices) {
                vertex.nudgeToward(panel.centroid, (float) 0.01 * i);
            }
            i++;
        }

        return points.stream()
                .map(l -> l.stream().toArray(LXPoint[]::new))
                .toArray(LXPoint[][]::new);
    }

    private static double distanceBetweenPointAndLineSegment(LXVector line_p1, LXVector line_p2, LXPoint p) {
        LXVector v0 = new LXVector(p);
        LXVector v1 = new LXVector(line_p1);
        LXVector v2 = new LXVector(line_p2);

        double segmentLengthSquared = new LXVector(v2).sub(v1).magSq();
        double t = Math.max(
                0,
                Math.min(1, new LXVector(v0).sub(v1).dot(new LXVector(v2).sub(v1)) / segmentLengthSquared)
        );
        LXVector projection = new LXVector(v1).add(new LXVector(v2).sub(v1).mult((float)t));

        return v0.dist(projection);
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
        super.onParameterChanged(parameter);
        if (parameter.getPath().equals("tempoSync")) {
            BooleanParameter p = (BooleanParameter) parameter;
            this.phase.tempoSync.setValue(p.getValueb());
        } else if (parameter.getPath().equals("tempoDivision")) {
            @SuppressWarnings("unchecked")
            EnumParameter<Tempo.Division> p = (EnumParameter<Tempo.Division>) parameter;
            this.phase.tempoDivision.setValue(p.getEnum());
        } else if (parameter.getPath().equals("tempoLock")) {
            BooleanParameter p = (BooleanParameter) parameter;
            this.phase.tempoLock.setValue(p.getValueb());
        }
    }
}
