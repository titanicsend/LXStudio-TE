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
import titanicsend.util.TEMath;

import java.util.*;

@LXCategory("Titanics End")
public class TERandomStrobe extends TEEffect {

    private class PanelsWithEdges {
        public TEPanelModel panel;
        public Set<TEEdgeModel> edges;
    }

    private final ArrayList<PanelsWithEdges> panelsWithEdges = new ArrayList<>();
    private final VariableSpeedTimer time;
    private double eventStartTime;

    private static final int PRIMARY_COLOR_INDEX = 2;

    public final CompoundParameter speed =
        new CompoundParameter("Speed", 0, 0, 24)
            .setExponent(2)
            .setDescription("Frequency of strobe effect in Hz");

    public final CompoundParameter density =
        new CompoundParameter("Density", 8, 1, 26)
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

    private PanelsWithEdges findPanelEntry(TEPanelModel p) {
        for (PanelsWithEdges pwe : panelsWithEdges) {
            if (pwe.panel == p) return pwe;
        }
        return null;
    }

    // Ick! exhaustive search for edges connected to each panel
    // TODO - Add connectedEdges to panel model. It'll be handy.
    //
    // TODO - This is all the panels/edges on the car, which means
    // TODO - it's technically possible to have all the random strobes on
    // TODO - one side, leaving the other dark. Can we make use of symmetry
    // TODO - to avoid this, and speed up frame calculation?
    public void buildPanelsAndEdgeList() {
        for (TEEdgeModel edge : modelTE.getAllEdges()) {
            for (TEPanelModel panel : edge.connectedPanels) {
                PanelsWithEdges pwe = findPanelEntry(panel);
                if (pwe == null) {
                    pwe = new PanelsWithEdges();
                    pwe.panel = panel;
                    pwe.edges = new HashSet<>();
                    panelsWithEdges.add(pwe);
                }
                pwe.edges.add(edge);
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
                Collections.shuffle(panelsWithEdges);
                eventStartTime = time.getTime();
            }

            if (et <= dutyCycle.getValue()) {
                // TODO - get a real color
                int col = (white.isOn()) ? LXColor.WHITE : lx.engine.palette.getSwatchColor(PRIMARY_COLOR_INDEX).getColor();;

                for (int i = 0; i < density.getValue(); i++) {
                    PanelsWithEdges pwe = panelsWithEdges.get(i);
                    if (panelsLit.isOn()) {
                        for (TEPanelModel.LitPointData p : pwe.panel.litPointData) {
                            this.colors[p.point.index] = col;
                        }
                    }
                    if (edgesLit.isOn()) {
                        for (TEEdgeModel e : pwe.edges) {
                            for (TEEdgeModel.Point p : e.points) {
                                this.colors[p.index] = col;
                            }
                        }
                    }
                }
            }
        }
    }
}
