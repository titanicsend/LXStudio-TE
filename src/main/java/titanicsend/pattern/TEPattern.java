package titanicsend.pattern;

import heronarts.lx.LX;
import heronarts.lx.Tempo;
import heronarts.lx.audio.GraphicMeter;
import heronarts.lx.color.GradientUtils;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.pattern.LXModelPattern;
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

  public enum TEGradient {
    FULL_PALETTE("Full Palette"),
    PRIMARY("Primary"),
    SECONDARY("Secondary"),
    FOREGROUND("Foreground");

    public final String label;

    private TEGradient(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  };

  // Whole palette gradient across all 5 stops. Usually starts and ends with black.
  protected GradientUtils.ColorStops paletteGradient = new GradientUtils.ColorStops();     // [X] [X] [X] [X] [X]  All five color entries
  protected GradientUtils.ColorStops primaryGradient = new GradientUtils.ColorStops();     // [X] [ ] [X] [ ] [ ]  Background primary -> Primary
  protected GradientUtils.ColorStops secondaryGradient = new GradientUtils.ColorStops();   // [ ] [ ] [ ] [X] [X]  Background secondary -> Secondary
  protected GradientUtils.ColorStops foregroundGradient = new GradientUtils.ColorStops();  // [ ] [ ] [X] [X] [ ]  Primary -> Secondary

  // See TE Art Direction Standards: https://docs.google.com/document/d/16FGnQ8jopCGwQ0qZizqILt3KYiLo0LPYkDYtnYzY7gI/edit
  public enum ColorType {
    // These are 1-based UI indices; to get to a 0-based palette swatch index, subtract 1
    BACKGROUND(1), // Background color - should usually be black or transparent
    TRANSITION(2), // Transitions a background to the primary, commonly just the background again
    PRIMARY(3),    // Primary color of any edge or panel pattern
    SECONDARY(4),  // Secondary color; optional, commonly set to SECONDARY_BACKGROUND or PRIMARY
    SECONDARY_BACKGROUND(5);  // Background color if transitioning from SECONDARY. Commonly set to same color as BACKGROUND.

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
	// NOTE(mcslee): in newer LX version, colors array does not exist at instantiation
	// time. If this call was truly necessary, it will need to be refactored to happen elsewhere
    // this.clearPixels();
	  
    this.sua = this.model.panelsById.get("SUA");
    this.sdc = this.model.panelsById.get("SDC");

    this.primaryGradient.setNumStops(3);
    this.secondaryGradient.setNumStops(3);
    this.foregroundGradient.setNumStops(3);
    updateGradients();
  }

  @Override
  public void onInactive() {
    clearPixels();
    super.onInactive();
  }


  /*
   * Color methods
   */

  public LinkedColorParameter registerColor(String label, String path, ColorType colorType, String description) {
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
    primaryGradient.stops[0].set(lx.engine.palette.getSwatchColor(ColorType.PRIMARY.swatchIndex()));
    primaryGradient.stops[1].set(lx.engine.palette.getSwatchColor(ColorType.BACKGROUND.swatchIndex()));
    primaryGradient.stops[2].set(lx.engine.palette.getSwatchColor(ColorType.PRIMARY.swatchIndex()));
    secondaryGradient.stops[0].set(lx.engine.palette.getSwatchColor(ColorType.SECONDARY.swatchIndex()));
    secondaryGradient.stops[1].set(lx.engine.palette.getSwatchColor(ColorType.SECONDARY_BACKGROUND.swatchIndex()));
    secondaryGradient.stops[2].set(lx.engine.palette.getSwatchColor(ColorType.SECONDARY.swatchIndex()));
    foregroundGradient.stops[0].set(lx.engine.palette.getSwatchColor(ColorType.PRIMARY.swatchIndex()));
    foregroundGradient.stops[1].set(lx.engine.palette.getSwatchColor(ColorType.SECONDARY.swatchIndex()));
    foregroundGradient.stops[2].set(lx.engine.palette.getSwatchColor(ColorType.PRIMARY.swatchIndex()));
  }

  /**
   * Given a value in 0..1 (and wrapped back outside that range)
   * Return a color within the primaryGradient
   * @param lerp as a frac
   * @return LXColor
   */
  public int getPrimaryGradientColor(float lerp) {
    /* HSV2 mode wraps returned colors around the color wheel via the shortest
     * hue distance. In other words, we usually want a gradient to go from yellow
     * to red via orange, not via lime, green, cyan, blue, purple, red.
     */
    return primaryGradient.getColor(
            TEMath.trianglef(lerp / 2), // Allow wrapping
            GradientUtils.BlendMode.HSV2.function);
  }

  /**
   * Given a value in 0..1 (and wrapped back outside that range)
   * Return a color within the secondaryGradient
   * @param lerp
   * @return
   */
  public int getSecondaryGradientColor(float lerp) {
    /* HSV2 mode wraps returned colors around the color wheel via the shortest
     * hue distance. In other words, we usually want a gradient to go from yellow
     * to red via orange, not via lime, green, cyan, blue, purple, red.
     */
    return secondaryGradient.getColor(
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

  // During construction, make gap points show up in red
  static public final int GAP_PIXEL_COLOR = TEColor.TRANSPARENT;
  
  // Compare to LXLayeredComponent's clearColors(), which is declared final.
  public void clearPixels() {
    for (LXPoint point : this.model.points) {
      if (this.model.isGapPoint(point)) {
        colors[this.model.gapPoint.index] = GAP_PIXEL_COLOR;
      } else {
        colors[point.index] = TEColor.TRANSPARENT;
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
