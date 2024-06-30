package titanicsend.pattern.alex;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import titanicsend.color.TEColorType;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEPattern;

@LXCategory("Combo FG")
public class BrightScreen extends TEPattern {
  public final LinkedColorParameter color =
      registerColor("Color", "color", TEColorType.PRIMARY, "Color of the screen");

  public BrightScreen(LX lx) {
    super(lx);
  }

  public void run(double deltaMs) {
    int color = this.color.calcColor();

    for (TEEdgeModel edge : this.modelTE.getEdges()) {
      for (LXPoint point : edge.points) {
        colors[point.index] = color;
      }
    }

    for (TEPanelModel panel : this.modelTE.getPanels()) {
      for (LXPoint point : panel.points) {
        colors[point.index] = color;
      }
    }
  }
}
