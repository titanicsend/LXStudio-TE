package titanicsend.pattern.will;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.p4lx.ui.UI2dComponent;
import heronarts.p4lx.ui.UI2dContainer;
import heronarts.p4lx.ui.component.UIButton;
import heronarts.p4lx.ui.component.UIKnob;
import heronarts.p4lx.ui.component.UITextBox;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEPattern;
import titanicsend.util.TE;

import java.util.*;

@LXCategory("Test")
public class PowerDebugger extends TEPattern implements UIDeviceControls<PowerDebugger> {

    public final StringParameter powerboxId =
            new StringParameter("Powerbox ID")
                    .setDescription("ID of the powerbox");

    public final DiscreteParameter powerboxSelect =
            new DiscreteParameter("Powerbox select", 0, 1)
                    .setDescription("Manual powerbox selection");

    private UI2dComponent errLabel;

    private HashSet<String> powerboxIDsSet;
    private ArrayList<String> powerboxIDsList;

    // powerbox ID -> edge IDs
    private HashMap<String, HashSet<String>> pow2edge;
    // powerbox ID -> panel IDs
    private HashMap<String, HashSet<String>> pow2panel;

    // powerbox -> sum of current
    private HashMap<String, Double> pow2totalCurrent; // powerbox -> total current
    private HashMap<String, Double> panelJboxTotalCurrent; // panel + powerbox -> total current from pow to panel
    private HashMap<String, Double> panelTotalCurrent; // panelID -> total current


    // powerbox -> sum of LEDs
    private HashMap<String, Double> pow2totalLEDs;

    public PowerDebugger(LX lx) {
        super(lx);

        // parameters
        addParameter("powerboxId", powerboxId);
        addParameter("powerboxSelect", powerboxSelect);

        // initialize maps
        powerboxIDsSet = new HashSet<>();
        pow2edge = new HashMap<>();
        pow2panel = new HashMap<>();
        pow2totalLEDs = new HashMap<>();

        pow2totalCurrent = new HashMap<>();
        panelTotalCurrent = new HashMap<>();
        panelJboxTotalCurrent = new HashMap<>();

        powerboxIDsList = new ArrayList<>();

        load();
    }

    /**
     * Build our UI for this pattern manually.
     *
     * @param ui
     * @param uiDevice
     * @param powerDebugger
     */
    @Override
    public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, PowerDebugger powerDebugger) {
        uiDevice.setLayout(UI2dContainer.Layout.VERTICAL);
        uiDevice.setChildSpacing(6);
        uiDevice.setContentWidth(COL_WIDTH * 5);

        UITextBox tb;

        uiDevice.addChildren(
                controlLabel(ui, "Enter Powerbox ID").setWidth(COL_WIDTH * 3),
                tb = new UITextBox(0, 0, COL_WIDTH * 3, 16).setParameter(powerboxId),

                controlLabel(ui, "Select Powerbox ID").setWidth(COL_WIDTH * 3),
                new UIKnob(0, 0, powerboxSelect).setWidth(COL_WIDTH * 3),

                controlLabel(ui, "Reload \"power_assignments.tsv\"").setWidth(COL_WIDTH * 3),
                new UIButton(0, 0, COL_WIDTH * 4, 20) {
                    @Override
                    public void onToggle(boolean on) {
                        if (on) {
                            load();
                        }
                    }
                }.setLabel("-> reload").setMomentary(true),

                this.errLabel = controlLabel(ui, "Bad ID")
                        .setWidth(COL_WIDTH * 4)
        );

        this.errLabel.setVisible(false);
        this.powerboxId.addListener(this::repaint);
    }

    /**
     * Clear out from old selections of powerbox
     */
    private void clearEdgesAndPanels() {
        for (LXPoint p : this.model.points) {
            colors[p.index] = LXColor.BLACK;
        }
    }

    private void repaint(LXParameter ignore) {
        // turn off all errors
        this.errLabel.setVisible(false);

        // get the user-supplied ID to the powerbox
        String powID = powerboxId.getString();

        // is this a valid powerbox ID?
        if (!powerboxIDsSet.contains(powID)) {
            this.errLabel.setVisible(true);
            TE.log("PowerboxID=%s could not be found! Check your spelling", powerboxId.getString());
            return;
        }

        showPowerbox(powID);
    }

    /**
     * Light up panels and edges corresponding to this powerbox ID
     * @param powID
     */
    private void showPowerbox(String powID) {
        // how many edges and panels are connected?
        int numEdges = pow2edge.get(powID) != null ? pow2edge.get(powID).size() : 0;
        int numPanels = pow2panel.get(powID) != null ? pow2panel.get(powID).size() : 0;
        double totalCurrentFromPowerbox = pow2totalCurrent.get(powID);

        // clear edges & report back to user
        clearEdgesAndPanels();
        TE.log("Powerbox[%s]: edges=%d, panels=%d, current=%f LEDs=%f"
                , powID, numEdges, numPanels
                , totalCurrentFromPowerbox, pow2totalLEDs.get(powID));

        if (numEdges > 0) {
            //TE.log("Edges:");
            for (Object e : pow2edge.get(powID).toArray()) {
                String edgeID = (String) e;
                //TE.log("\tedge=%s", edgeID);
                TEEdgeModel edge = this.model.edgesById.get(edgeID);
                for (LXPoint ep : edge.points) {
                    colors[ep.index] = LXColor.WHITE;
                }
            }
        }

        if (numPanels > 0) {
            //TE.log("Panels:");
            for (Object e : pow2panel.get(powID).toArray()) {
                String panelID = (String) e;
                //TE.log("\tpanel=%s", panelID);
                TEPanelModel panel = this.model.panelsById.get(panelID);

                // should we only partially light this panel?
                String panelJboxCompositeID = powID + "__" + panelID;
                double currentFromJboxForPanel = panelJboxTotalCurrent.get(panelJboxCompositeID);
                double percOfPanelCurrentFromPowerbox =
                    currentFromJboxForPanel / panelTotalCurrent.get(panelID);
                int lastPointIdx = (int) (percOfPanelCurrentFromPowerbox * panel.points.length);

                // iterate through panel points, only lighting if enough
                // power from this powerbox
                int i = 0;
                for (LXPoint pp : panel.points) {
                    colors[pp.index] = LXColor.WHITE;
                    if (i == lastPointIdx)
                        break;
                    i++;
                }
            }
        }
    }

    /**
     * When a user changes the select param, we want to catch that and also update the
     * text input field.
     *
     * @param parameter
     */
    @Override
    public void onParameterChanged(LXParameter parameter) {
        if (parameter.equals(powerboxSelect)) {
            String powID = powerboxIDsList.get(powerboxSelect.getValuei());
            powerboxId.setValue(powID); // this will call showPowerbox() !
        }
    }

    /**
     * Load the .tsv from disk. Measurements come from here:
     * https://docs.google.com/spreadsheets/d/1C7VPybckgH9bWGxwtgMN_Ij1T__c5qc-k7yIhG-592Y/edit#gid=1763244801
     *
     * and generated with algorithm to minimize power distribution length/cost.
     */
    private void load() {
        Scanner s = this.model.loadFile("power_assignments.tsv");
        int i = 0;
        while (s.hasNextLine()) {
            String line = s.nextLine();

            // skip our header
            i++;
            if (i == 1) {
                //TE.log("skipping header...");
                continue;
            }

            // parse the line!
            try {
                String[] tokens = line.split("\\t");
                assert tokens.length == 8;
                //TE.log("Line: %s (len tokens=%d)", line, tokens.length);

                // parse fields
                //String stripID = tokens[0].strip();
                String panelOrEdgeId = tokens[1].strip();
                //String panelOrEdgeVertices = tokens[2].strip();
                double numLEDs = Double.parseDouble(tokens[3].strip());
                String jbox = tokens[4].strip();
                // String circuit = tokens[5].strip();
                double current = Double.parseDouble(tokens[6].strip());
                boolean isEdge = panelOrEdgeId.contains("-");
                int jboxVertex = Integer.parseInt(jbox.split("-")[0]);
                int jboxIdx = Integer.parseInt(jbox.split("-")[1]);

                // update maps
                powerboxIDsSet.add(jbox);

                // add to our list of corresponding edges or panels
                if (isEdge) {
                    if (!pow2edge.containsKey(jbox)) {
                        HashSet<String> tmp = new HashSet<String>();
                        tmp.add(panelOrEdgeId);
                        pow2edge.put(jbox, tmp);
                    } else {
                        HashSet<String> edgeIDs = pow2edge.get(jbox);
                        edgeIDs.add(panelOrEdgeId);
                    }

                } else {
                    if (!pow2panel.containsKey(jbox)) {
                        HashSet<String> tmp = new HashSet<String>();
                        tmp.add(panelOrEdgeId);
                        pow2panel.put(jbox, tmp);
                    } else {
                        HashSet<String> edgeIDs = pow2panel.get(jbox);
                        edgeIDs.add(panelOrEdgeId);
                    }

                    // total the current through this panel
                    String panelJboxCompositeID = jbox + "__" + panelOrEdgeId;
                    if (!panelJboxTotalCurrent.containsKey(panelJboxCompositeID))
                        panelJboxTotalCurrent.put(panelJboxCompositeID, current);
                    else
                        panelJboxTotalCurrent.put(
                                panelJboxCompositeID,
                                panelJboxTotalCurrent.get(panelJboxCompositeID) + current);
                }

                // update our current counter per powerbox
                if (!pow2totalCurrent.containsKey(jbox))
                    pow2totalCurrent.put(jbox, current);
                else
                    pow2totalCurrent.put(jbox, pow2totalCurrent.get(jbox) + current);

                // update counter current per panel
                if (!isEdge) {
                    if (!panelTotalCurrent.containsKey(panelOrEdgeId))
                        panelTotalCurrent.put(panelOrEdgeId, current);
                    else
                        panelTotalCurrent.put(panelOrEdgeId, panelTotalCurrent.get(panelOrEdgeId) + current);
                }

                // update our LED count
                if (!pow2totalLEDs.containsKey(jbox))
                    pow2totalLEDs.put(jbox, numLEDs);
                else
                    pow2totalLEDs.put(jbox, pow2totalLEDs.get(jbox) + numLEDs);

            } catch (Exception e) {
                TE.err(e.toString());
                e.printStackTrace();
                TE.err(line);
                return;
            }
        }

        // add powerbox IDs to list & set range
        powerboxIDsList.clear();
        for (Object o : powerboxIDsSet.toArray()) {
            powerboxIDsList.add((String)o);
        }
        Collections.sort(powerboxIDsList);

        powerboxSelect.setRange(0, powerboxIDsSet.size());
    }

    @Override
    protected void run(double v) {

    }

    @Override
    public void disposeDeviceControls(LXStudio.UI ui, UIDevice uiDevice, PowerDebugger powerDebugger) {
        UIDeviceControls.super.disposeDeviceControls(ui, uiDevice, powerDebugger);
    }
}
