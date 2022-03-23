package titanicsend.pattern.alex;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEPattern;

import java.util.HashMap;
import java.util.Map;

@LXCategory("Combo FG")
public class BrightScreen extends TEPattern {
    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PANEL,
                    "Color of the screen");

    public BrightScreen(LX lx) {
        super(lx);
    }

    public void run(double deltaMs) {
        int color = this.color.calcColor();

        for (TEEdgeModel edge : model.edgesById.values()) {
            for (LXPoint point : edge.points) {
                colors[point.index] = color;
            }
        }

        for (TEPanelModel panel : model.panelsById.values()) {
            for (LXPoint point : panel.points) {
                colors[point.index] = color;
            }
        }
    }
}