package titanicsend.pattern.mike;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEPerformancePattern;

import java.util.*;

@LXCategory("TE Examples")
public class Checkers extends TEPerformancePattern {

  private final HashMap<TEPanelModel, Integer> panelGroup;

  public Checkers(LX lx) {
    super(lx);

    addCommonControls();

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
  protected void runTEAudioPattern(double deltaMs) {
    int color1 = calcColor();
    int color2 = calcColor2();

    for (Map.Entry<TEPanelModel, Integer> entry : this.panelGroup.entrySet()) {
      TEPanelModel panel = entry.getKey();
      int panelGroup = entry.getValue();
      int rgb = panelGroup == 0 ? color1 : color2;
      for (LXPoint point : panel.points) {
        if (model.isGapPoint(point)) continue;
        colors[point.index] = rgb;
      }
    }
    this.updateVirtualColors(deltaMs);
  }

}