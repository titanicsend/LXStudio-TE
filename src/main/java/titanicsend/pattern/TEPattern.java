package titanicsend.pattern;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.pattern.LXModelPattern;
import titanicsend.model.TELaserModel;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;
import titanicsend.util.TEColor;

import java.util.*;

public abstract class TEPattern extends LXModelPattern<TEWholeModel> {
  private final TEPanelModel sua;
  private final TEPanelModel sdc;

  protected enum ColorType {
    EDGE(1),      // Primary color to use on edges
    SECONDARY(2), // Secondary color to use on edges or panels (or lasers?)
    PANEL(3);     // Primary color to use on panels
    public final int index;
    private ColorType(int index) {
      this.index = index;
    }
  }

  protected TEPattern(LX lx) {
    super(lx);
    this.clearPixels();
    this.sua = this.model.panelsById.get("SUA");
    this.sdc = this.model.panelsById.get("SDC");
  }

  protected LinkedColorParameter registerColor(String label, String path, ColorType colorType, String description) {
    LinkedColorParameter lcp = new LinkedColorParameter(label)
            .setDescription(description);
    addParameter(path, lcp);
    lcp.mode.setValue(LinkedColorParameter.Mode.PALETTE);
    lcp.index.setValue(colorType.index);
    return lcp;
  }

  public void clearPixels() {
    for (LXPoint point : this.model.points) {
      if (point.equals(this.model.gapPoint)) {
        // During construction, make gap points show up in red
        colors[this.model.gapPoint.index] = LXColor.rgb(255, 0, 0);
      } else {
        colors[point.index] = TEColor.TRANSPARENT; // Transparent
      }
    }
  }

  // Make the virtual model's solid panels and lasers get rendered to match
  // their LXPoint color
  // TODO: Return quickly if lasers/etc aren't being used
  public void updateVirtualColors(double deltaMsec) {
    for (TEPanelModel panel : this.model.panelsById.values()) {
      if (panel.panelType.equals(TEPanelModel.SOLID)) {
        panel.virtualColor.rgb = colors[panel.points[0].index];
      }
    }
    for (TELaserModel laser : this.model.lasersById.values()) {
      laser.control.update(deltaMsec);
      laser.color = colors[laser.points[0].index];
    }
  }

  // Returns a set of points that GP should use to make its palette broadcasts.
  // By default, it will pick a point in the middle of SUA and SDC panels and
  // a point in the middle of one of each of their edges. If your pattern would
  // prefer to use some other points as the source of its GP packets, override!
  public List<LXPoint> getGigglePixelPoints() {
    List<LXPoint> rv = new ArrayList<>();

    if (this.sua != null) {
      int halfway = this.sua.points.length / 2;
      if (halfway < this.sua.points.length) rv.add(this.sua.points[halfway]);

      halfway = this.sua.e0.points.length / 2;
      if (halfway < this.sua.e0.points.length) rv.add(this.sua.e0.points[halfway]);
    }

    if (this.sdc != null) {
      int halfway = this.sdc.points.length / 2;
      if (halfway < this.sdc.points.length) rv.add(this.sdc.points[halfway]);

      halfway = this.sdc.e0.points.length / 2;
      if (halfway < this.sdc.e0.points.length) rv.add(this.sdc.e0.points[halfway]);
    }
    return rv;
  }
}
