package titanicsend.effect;

/*
 * Minimalist strobe effect for Titanic's End, designed to be run via DMX from
 * a remote lighting console.  Strobes one or more random panels at the set
 * trigger rate.
 *
 */

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.jon.VariableSpeedTimer;

import java.util.*;

@LXCategory("Titanics End")
public class TERandomStrobe extends TEEffect {

    private class PanelsWithEdges {
        public TEPanelModel panel;
        public Set<TEEdgeModel> edges;
    }

    private final ArrayList<PanelsWithEdges> starboard = new ArrayList<>();
    private final ArrayList<PanelsWithEdges> port = new ArrayList<>();
    private final VariableSpeedTimer time;
    private double eventStartTime;

    private static final int PRIMARY_COLOR_INDEX = 2;

    public final CompoundParameter speed =
        new CompoundParameter("Speed", 0, 0, 24)
            .setExponent(2)
            .setDescription("Frequency of strobe effect in Hz");

    public final CompoundParameter density =
        new CompoundParameter("Density", 8, 1, 22)
            .setDescription("Number of triangles to strobe");

    public final CompoundParameter dutyCycle =
        new CompoundParameter("DutyCycle", 0.4)
            .setDescription("Duty cycle of strobe effect");

    public final BooleanParameter panelsLit =
        new BooleanParameter("Panels", true)
            .setDescription("Strobe effect lights panels");

    public final BooleanParameter edgesLit =
        new BooleanParameter("Edges", true)
            .setDescription("Strobe effect lights panel edges");

    public final BooleanParameter white =
        new BooleanParameter("White", false)
            .setDescription("Strobe is bright white if enabled.");

    public TERandomStrobe(LX lx) {
        super(lx);
        addParameter("speed", this.speed);
        addParameter("density", this.density);
        addParameter("dutyCycle", this.dutyCycle);
        addParameter("panelsLit", this.panelsLit);
        addParameter("edgesLit", this.edgesLit);
        addParameter("white", this.white);

        time = new VariableSpeedTimer();
        eventStartTime = -99f;

        buildPanelsAndEdgeList();
    }

    private PanelsWithEdges findPanelEntry(TEPanelModel p, ArrayList<PanelsWithEdges> list) {
        for (PanelsWithEdges pwe : list) {
            if (pwe.panel == p) return pwe;
        }
        return null;
    }

    // Ick! Exhaustive search for edges connected to each panel
    // TODO - consider adding a connectedEdges list to TEPanelModel
    //
    // We keep lists for each side of the car so we can light both
    // sides in a balanced way, and there's no chance that rng will
    // cause one side to be completely dark.
    public void buildPanelsAndEdgeList() {
        for (TEPanelModel panel : modelTE.getAllPanels()) {
            boolean isStarboard = (panel.centroid.x > 0);

            PanelsWithEdges pwe = findPanelEntry(panel, isStarboard ? starboard : port);
            if (pwe == null) {
                pwe = new PanelsWithEdges();
                pwe.panel = panel;
                pwe.edges = new HashSet<>();
                if (isStarboard) {
                    starboard.add(pwe);
                } else {
                    port.add(pwe);
                }
            }
            for (TEEdgeModel edge : modelTE.getAllEdges()) {
                if (edge.connectedPanels.contains(panel)) {
                    pwe.edges.add(edge);
                }
            }
        }
    }

    private void lightPanelPoints(PanelsWithEdges pwe, int col) {
        for (TEPanelModel.LitPointData p : pwe.panel.litPointData) {
            this.colors[p.point.index] = col;
        }
    }

    private void lightEdgePoints(PanelsWithEdges pwe, int col) {
        for (TEEdgeModel e : pwe.edges) {
            for (TEEdgeModel.Point p : e.points) {
                this.colors[p.index] = col;
            }
        }
    }

    @Override
    public void run(double deltaMs, double enabledAmount) {
        // set time scale for current strobe frequency
        time.setScale(speed.getValue());
        time.tick();

        if (speed.getValue() > 0) {
            double et = time.getTime() - eventStartTime;

            // if we're not currently in a flash event start a new one
            if (et >= 1.0) {
                Collections.shuffle(starboard);
                Collections.shuffle(port);
                eventStartTime = time.getTime();
            }

            if (et <= dutyCycle.getValue()) {
                // TODO - get a real color
                int col = (white.isOn()) ? LXColor.WHITE :
                    lx.engine.palette.getSwatchColor(PRIMARY_COLOR_INDEX).getColor();

                for (int i = 0; i < density.getValue(); i++) {
                    PanelsWithEdges starboardTriangle = starboard.get(i);
                    PanelsWithEdges portTriangle = port.get(i);

                    if (panelsLit.isOn()) {
                        lightPanelPoints(starboardTriangle, col);
                        lightPanelPoints(portTriangle, col);

                    }
                    if (edgesLit.isOn()) {
                        lightEdgePoints(starboardTriangle, col);
                        lightEdgePoints(portTriangle, col);
                    }
                }
            }
        }
    }
}
