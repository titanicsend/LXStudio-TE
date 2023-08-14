package titanicsend.effect;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import heronarts.glx.ui.UI2dContainer.Layout;
import heronarts.glx.ui.component.UITextBox;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.model.TEPanelModel;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@LXCategory("Utility")
public class PanelAdjustEffect extends TEEffect implements UIDeviceControls<PanelAdjustEffect> {

    private static final String RESOURCES_PATH = "./resources/vehicle/";
    private static final int MAX_ADJUST = 200000;
    private final Gson serializer = new GsonBuilder().setPrettyPrinting().create();

    public final StringParameter panelIdParameter = new StringParameter("PanelId");
    public final CompoundParameter xAdjust = new CompoundParameter("xAdjust", 0, -MAX_ADJUST, MAX_ADJUST);
    public final CompoundParameter yAdjust = new CompoundParameter("yAdjust", 0, -MAX_ADJUST, MAX_ADJUST);
    public final CompoundParameter zAdjust = new CompoundParameter("zAdjust", 0, -MAX_ADJUST, MAX_ADJUST);
    private final BooleanParameter resetPanelButton = new BooleanParameter("ResetPanel");
    private final BooleanParameter resetAllButton = new BooleanParameter("ResetAll");
    private final BooleanParameter saveButton = new BooleanParameter("Save");

    private static final Map<String, TEPanelModel.Adjustment> PANEL_ID_TO_ADJUSTMENT = new HashMap<>();
    private TEPanelModel.Adjustment currentAdjustment;

    public PanelAdjustEffect(LX lx) {
        super(lx);
        addParameter("PanelId", panelIdParameter);
        addParameter("xAdjust", xAdjust);
        addParameter("yAdjust", yAdjust);
        addParameter("zAdjust", zAdjust);
        addParameter("resetPanel", resetPanelButton);
        addParameter("resetAll", resetAllButton);
        addParameter("save", saveButton);

        try {
            Type type = new TypeToken<Map<String, TEPanelModel.Adjustment>>() {}.getType();
            PANEL_ID_TO_ADJUSTMENT.putAll(serializer.fromJson(
                    Files.readString(Path.of(RESOURCES_PATH + "panel_adjustments.txt")), type));
        } catch (IOException e) {
            LX.warning("Could not find saved adjustments file");
        }
    }

    @Override
    protected void onEnable() {
        for (String panelId : PANEL_ID_TO_ADJUSTMENT.keySet()) {
            if (isValidPanelId(panelId)) {
                modelTE.panelsById.get(panelId).setAdjustment(PANEL_ID_TO_ADJUSTMENT.get(panelId));
            }
        }
    }

    @Override
    protected void onDisable() {
        clearAdjustments();
    }

    @Override
    protected void run(double v, double v1) {
        //do nothing; the good stuff is onEnable/disable
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
        if (parameter == this.panelIdParameter) {
            String panelId = this.panelIdParameter.getString();
            currentAdjustment = PANEL_ID_TO_ADJUSTMENT.get(panelId);
            if (currentAdjustment == null) {
                currentAdjustment = new TEPanelModel.Adjustment();
                if (isValidPanelId(panelId)) {
                    PANEL_ID_TO_ADJUSTMENT.put(panelId, currentAdjustment);
                }
            }
            currentAdjustment = currentAdjustment != null ? currentAdjustment : new TEPanelModel.Adjustment();
            xAdjust.setValue(currentAdjustment.x);
            yAdjust.setValue(currentAdjustment.y);
            zAdjust.setValue(currentAdjustment.z);
        } else if (parameter == xAdjust) {
            currentAdjustment.x = (float) xAdjust.getValue();
            refreshCurrentAdjustment();
        } else if (parameter == yAdjust) {
            currentAdjustment.y = (float) yAdjust.getValue();
            refreshCurrentAdjustment();
        } else if (parameter == zAdjust) {
            currentAdjustment.z = (float) zAdjust.getValue();
            refreshCurrentAdjustment();
        } else if (parameter == resetPanelButton) {
            currentAdjustment.x = 0;
            currentAdjustment.y = 0;
            currentAdjustment.z = 0;
            resetAdjustParams();
        } else if (parameter == resetAllButton) {
            PANEL_ID_TO_ADJUSTMENT.clear();
            clearAdjustments();
            resetAdjustParams();
        } else if (parameter == saveButton) {
            try {
                try (PrintWriter out = new PrintWriter(RESOURCES_PATH + "panel_adjustments.txt")) {
                    out.println(serializer.toJson(PANEL_ID_TO_ADJUSTMENT));
                }
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void refreshCurrentAdjustment() {
        if (isValidPanelId(panelIdParameter.getString())) {
            TEPanelModel panelModel = modelTE.panelsById.get(panelIdParameter.getString());
            panelModel.setAdjustment(currentAdjustment);
        }
    }

    private void clearAdjustments() {
        for (TEPanelModel panelModel : this.modelTE.panelsById.values()) {
            panelModel.clearAdjustment();
        }
    }

    private boolean isValidPanelId(String panelId) {
        return this.modelTE.panelsById.get(panelId) != null;
    }

    @Override
    public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, PanelAdjustEffect effect) {
        uiDevice.setLayout(Layout.VERTICAL_GRID);
        uiDevice.setChildSpacing(6);
        uiDevice.setContentWidth(COL_WIDTH);

        UITextBox tb;
        uiDevice.addChildren(
                tb = new UITextBox(0, 0, COL_WIDTH, 16),
                controlLabel(ui, "PanelId"),
                newKnob(xAdjust),
                newKnob(yAdjust),
                newKnob(zAdjust),
                newButton(resetPanelButton).setMomentary(true),
                newButton(resetAllButton).setMomentary(true),
                newButton(saveButton).setMomentary(true)
        );
        tb.setParameter(panelIdParameter);
        tb.setEmptyValueAllowed(true);
    }

    private void resetAdjustParams() {
        xAdjust.setValue(0);
        yAdjust.setValue(0);
        zAdjust.setValue(0);
    }

    //Let's not save the exact state of the controls
    //Otherwise, when it loads the project file, it will set the controls back to the state when saved
    // which will then impact the actual adjustments
    @Override
    public void save(LX lx, JsonObject object) {
        panelIdParameter.setValue("");
        resetAdjustParams();
        super.save(lx, object);
    }

}
