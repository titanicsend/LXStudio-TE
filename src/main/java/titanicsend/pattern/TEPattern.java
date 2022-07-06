package titanicsend.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXLayeredComponent;
import heronarts.lx.Tempo;
import heronarts.lx.audio.GraphicMeter;
import heronarts.lx.color.GradientUtils;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.model.LXView;
import heronarts.lx.pattern.LXModelPattern;
import heronarts.lx.pattern.LXPattern;
import titanicsend.app.autopilot.TEPhrase;
import titanicsend.model.TELaserModel;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;
import titanicsend.util.TEColor;
import titanicsend.util.TEMath;

import java.util.*;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

public abstract class TEPattern extends LXModelPattern<TEWholeModel> {
  private final TEPanelModel sua;
  private final TEPanelModel sdc;
  protected GradientUtils.ColorStops paletteGradient = new GradientUtils.ColorStops();
  protected GradientUtils.ColorStops edgeGradient = new GradientUtils.ColorStops();
  protected GradientUtils.ColorStops panelGradient = new GradientUtils.ColorStops();

  public enum ColorType {
    // These are 1-based UI indices; to get to a 0-based palette index, subtract 1
    EDGE(1),      // Primary color to use on edges
    SECONDARY(2), // Secondary color to use on edges or panels (or lasers?)
    PANEL(3),     // Primary color to use on panels
    EDGE_BG(4),   // Background color to use on edges
    PANEL_BG(5);  // Background color to use on edges

    public final int index;  // The UI index (1-indexed)
    private ColorType(int index) {
      this.index = index;
    }

    // UI swatches are 1-indexed; internally, swatch arrays are 0-indexed
    public int swatchIndex() {
      return index - 1;
    }
  }

  protected TEPattern(LX lx) {
    super(lx);
    this.clearPixels();
    this.sua = this.model.panelsById.get("SUA");
    this.sdc = this.model.panelsById.get("SDC");

    this.edgeGradient.setNumStops(2);
    this.panelGradient.setNumStops(2);
    updateGradients();
  }


  /*
   * Color methods
   */


  protected LinkedColorParameter registerColor(String label, String path, ColorType colorType, String description) {
    LinkedColorParameter lcp = new LinkedColorParameter(label)
            .setDescription(description);
    addParameter(path, lcp);
    lcp.mode.setValue(LinkedColorParameter.Mode.PALETTE);
    lcp.index.setValue(colorType.index);
    return lcp;
  }

  // If a pattern uses the standard gradients, call this in run() to ensure
  // palette changes are known and transitions are smooth
  protected void updateGradients() {
    paletteGradient.setPaletteGradient(lx.engine.palette, 0, lx.engine.palette.swatch.colors.size());
    edgeGradient.stops[0].set(lx.engine.palette.getSwatchColor(ColorType.EDGE.swatchIndex()));
    edgeGradient.stops[1].set(lx.engine.palette.getSwatchColor(ColorType.SECONDARY.swatchIndex()));
    panelGradient.stops[0].set(lx.engine.palette.getSwatchColor(ColorType.PANEL.swatchIndex()));
    panelGradient.stops[1].set(lx.engine.palette.getSwatchColor(ColorType.SECONDARY.swatchIndex()));
  }

  /**
   * Given a value in 0..1 (and wrapped back outside that range)
   * Return a color within the edgeGradient
   * @param lerp
   * @return
   */
  public int getEdgeGradientColor(float lerp) {
    /* HSV2 mode wraps returned colors around the color wheel via the shortest
     * hue distance. In other words, we usually want a gradient to go from yellow
     * to red via orange, not via lime, green, cyan, blue, purple, red.
     */
    return edgeGradient.getColor(
            TEMath.trianglef(lerp / 2), // Allow wrapping
            GradientUtils.BlendMode.HSV2.function);
  }

  /**
   * Given a value in 0..1 (and wrapped back outside that range)
   * Return a color within the panelGradient
   * @param lerp
   * @return
   */
  public int getPanelGradientColor(float lerp) {
    /* HSV2 mode wraps returned colors around the color wheel via the shortest
     * hue distance. In other words, we usually want a gradient to go from yellow
     * to red via orange, not via lime, green, cyan, blue, purple, red.
     */
    return panelGradient.getColor(
            TEMath.trianglef(lerp / 2), // Allow wrapping
            GradientUtils.BlendMode.HSV2.function);
  }

  /**
   * Get a ColorType's color from the Swatch
   * @param type
   * @return
   */
  public int getSwatchColor(ColorType type) {
    return lx.engine.palette.getSwatchColor(type.swatchIndex()).getColor();
  }

  // Compare to LXLayeredComponent's clearColors(), which is declared final.
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

  // For patterns that only want to operate on edges
  public void setEdges(int color) {
    for (LXPoint point : this.model.edgePoints) {
      colors[point.index] = color;
    }
  }
  public void clearEdges() {
    setEdges(TEColor.TRANSPARENT);
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

  /*
   *  Audio and tempo methods
   */

  /**
   * Get the fraction into a measure, assuming a four beat measure
   * @return 0..1 ramp of progress (fraction) into the current measure
   */
  public double wholeNote() {
    return lx.engine.tempo.getBasis(Tempo.Division.WHOLE);
  }
  /**
   * Get the fraction into a musical phrase, assuming 8 * 4 beat phrases
   * @return 0..1 ramp of progress (fraction) into the current phrase
   */
  public double phrase() {
    return lx.engine.tempo.getCompositeBasis() / 32 % 1.0D;
  }

  //Sine modulator alternative between 0 and 1 on beat
  public double sinePhaseOnBeat() {
    return .5 * sin(PI * lx.engine.tempo.getCompositeBasis()) + .5;
  }

  /**
   * Get the fraction into a measure for any defined measure length
   * @return 0..1 ramp of progress (fraction) into the current measure
   */
  public double measure() {
    return (
            lx.engine.tempo.getCompositeBasis() %
            lx.engine.tempo.beatsPerMeasure.getValue() /
            lx.engine.tempo.beatsPerMeasure.getValue()
    );
  }

  public Tempo getTempo() {
    return lx.engine.tempo;
  }

  public GraphicMeter getEqualizer() {
    return lx.engine.audio.meter;
  }



  /*
     GigglePixel color sync protocol methods
   */

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
