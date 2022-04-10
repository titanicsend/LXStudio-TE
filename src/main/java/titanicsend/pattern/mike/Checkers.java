package titanicsend.pattern.mike;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEPattern;

import java.util.*;

@LXCategory("Panel FG")
public class Checkers extends TEPattern {

  public final LinkedColorParameter oddColor =
          registerColor("Odd", "odd", ColorType.PANEL,
                  "Color of the odd panels");

  public final LinkedColorParameter evenColor =
          registerColor("Even", "even", ColorType.SECONDARY,
                  "Color of the even panels");

  protected final HashMap<TEPanelModel, Integer> panelGroup;

  public Checkers(LX lx) {
    super(lx);

    this.panelGroup = new HashMap<>();
    List<TEPanelModel> queue = new ArrayList<>(model.panelsById.values());
    while (!queue.isEmpty()) {
      TEPanelModel panel = queue.remove(0);
      if (!this.panelGroup.containsKey(panel)) {
        // If not yet grouped, put it in Group 0, with a special exception
        // for SUA and PUA, which should be group 1 so they look different
        // from SUF/PUF
        int group = panel.id.endsWith("UA") ? 1 : 0;
        this.panelGroup.put(panel, group);
      }
      int thisPanelGroup = this.panelGroup.get(panel);
      int newColor = 1 - thisPanelGroup;  // Invert this panel's group
      for (TEPanelModel neighbor : panel.neighbors()) {
        if (this.panelGroup.containsKey(neighbor)) continue;  // Already grouped
        this.panelGroup.put(neighbor, newColor);
        queue.add(0, neighbor);
      }
    }
  }

  @Override
  public void run(double deltaMs) {
    int color0 = this.oddColor.calcColor();
    int color1 = this.evenColor.calcColor();

    for (Map.Entry<TEPanelModel, Integer> entry : this.panelGroup.entrySet()) {
      TEPanelModel panel = entry.getKey();
      int panelGroup = entry.getValue();
      int rgb = panelGroup == 0 ? color0 : color1;
      for (LXPoint point : panel.points) colors[point.index] = rgb;
    }
    this.updateVirtualColors(deltaMs);
  }
}