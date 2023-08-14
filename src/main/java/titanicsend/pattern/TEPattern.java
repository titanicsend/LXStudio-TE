package titanicsend.pattern;

import heronarts.lx.LX;
import heronarts.lx.Tempo;
import heronarts.lx.audio.GraphicMeter;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.color.LXSwatch;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.studio.TEApp;
import titanicsend.dmx.pattern.DmxPattern;
import titanicsend.lx.LXGradientUtils;
import titanicsend.model.TELaserModel;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;
import titanicsend.model.justin.ColorCentral;
import titanicsend.model.justin.LXVirtualDiscreteParameter;
import titanicsend.model.justin.SwatchParameter;
import titanicsend.model.justin.ColorCentral.ColorCentralListener;
import titanicsend.util.TEColor;
import titanicsend.util.TEMath;

import java.util.*;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

public abstract class TEPattern extends DmxPattern {
  private final TEPanelModel sua;
  private final TEPanelModel sdc;

  protected final TEWholeModel modelTE;

  public TEWholeModel getModelTE() {
    return this.modelTE;
  }

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
  protected LXGradientUtils.ColorStops paletteGradient = new LXGradientUtils.ColorStops();     // [X] [X] [X] [X] [X]  All five color entries
  protected LXGradientUtils.ColorStops primaryGradient = new LXGradientUtils.ColorStops();     // [X] [ ] [X] [ ] [ ]  Background primary -> Primary
  protected LXGradientUtils.ColorStops secondaryGradient = new LXGradientUtils.ColorStops();   // [ ] [ ] [ ] [X] [X]  Background secondary -> Secondary
  protected LXGradientUtils.ColorStops foregroundGradient = new LXGradientUtils.ColorStops();  // [ ] [ ] [X] [X] [ ]  Primary -> Secondary

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

  // VIRTUAL COLOR SWATCH PARAMETER

  // Pass-through to color swatch selection per channel
  public class LXVirtualSwatchParameter extends LXVirtualDiscreteParameter<SwatchParameter> implements ColorCentralListener {

    public LXVirtualSwatchParameter(String label) {
      super(label);

      setIncrementMode(IncrementMode.RELATIVE);
      setWrappable(false);

      if (ColorCentral.isLoaded()) {
        link();
      } else {
        listenForLoad();
      }
    }

    public void listenForLoad() {
      if (getParameter() == null) {
        ColorCentral.listenOnce(this);
      }
    }

    @Override
    public void ColorCentralLoaded() {
      link();
    }

    public void link() {
      if (getParameter() == null) {
        if (ColorCentral.isLoaded()) {
          LXChannel channel = getChannel();
          if (channel != null) {
            setParameter(ColorCentral.get().get(channel).selectedSwatch);
          }
        }
      }
    }

    // Type-specific pass-through
    public LXSwatch getSwatch() {
        SwatchParameter p = getParameter();
        if (p != null) {
            return ColorCentral.get().getSwatch(p.getObject());
        }
        return lx.engine.palette.swatch;
    }
  }

  // Virtual Swatch parameter: pass-through to ColorCentral's current swatch for this channel
  // Note this is a non-standard use of Palette Swatches and not recommended to do it this way,
  // but we're doing it as a safety mechanism on this short timeline before performance.
  public final LXVirtualSwatchParameter swatchParameter =
      new LXVirtualSwatchParameter("Swatch");

  protected TEPattern(LX lx) {
    super(lx);
	// NOTE(mcslee): in newer LX version, colors array does not exist at instantiation
	// time. If this call was truly necessary, it will need to be refactored to happen elsewhere
    // this.clearPixels();

    this.modelTE = TEApp.wholeModel;

    this.sua = this.modelTE.panelsById.get("SUA");
    this.sdc = this.modelTE.panelsById.get("SDC");

    this.paletteGradient.setNumStops(5);
    this.primaryGradient.setNumStops(3);
    this.secondaryGradient.setNumStops(3);
    this.foregroundGradient.setNumStops(3);
    updateGradients();

    // Patterns are created, then added to channel. Channel should be available on next engine loop.
    lx.engine.addTask(() -> {
        linkChannelParameters(lx);
    });
  }

  private void linkChannelParameters(LX lx) {
      // Finally safe to assume a channel has been assigned
      this.swatchParameter.link();
  }

  @Override
  public void onInactive() {
    clearPixels();
    super.onInactive();
  }

  @Override
  protected void onModelChanged(LXModel model) {
    // If the View changes, clear all pixels because some might not be used by the pattern.
    // With view-per-pattern, this can now get called when pattern is inactive.
    if (this.colors != null) {
      // Active pattern
      clearPixels();
    }
    super.onModelChanged(model);
  }


  /*
   * Color methods
   */
  @Deprecated
  public LinkedColorParameter registerColor(String label, String path, ColorType colorType, String description) {
    LinkedColorParameter lcp = new LinkedColorParameter(label)
            .setDescription(description);
    addParameter(path, lcp);
    lcp.mode.setValue(LinkedColorParameter.Mode.PALETTE);
    lcp.index.setValue(colorType.index);
    return lcp;
  }

  protected LXSwatch getSwatch() {
    return this.swatchParameter.getSwatch();
  }

  protected LXDynamicColor getSwatchColor(int index) {
    return getSwatch().getColor(index);
  }

  // If a pattern uses the standard gradients, call this in run() to ensure
  // palette changes are known and transitions are smooth
  protected void updateGradients() {
    paletteGradient.stops[0].set(getSwatchColor(0));
    paletteGradient.stops[1].set(getSwatchColor(1));
    paletteGradient.stops[2].set(getSwatchColor(2));
    paletteGradient.stops[3].set(getSwatchColor(3));
    paletteGradient.stops[4].set(getSwatchColor(4));
    primaryGradient.stops[0].set(getSwatchColor(ColorType.PRIMARY.swatchIndex()));
    primaryGradient.stops[1].set(getSwatchColor(ColorType.BACKGROUND.swatchIndex()));
    primaryGradient.stops[2].set(getSwatchColor(ColorType.PRIMARY.swatchIndex()));
    secondaryGradient.stops[0].set(getSwatchColor(ColorType.SECONDARY.swatchIndex()));
    secondaryGradient.stops[1].set(getSwatchColor(ColorType.SECONDARY_BACKGROUND.swatchIndex()));
    secondaryGradient.stops[2].set(getSwatchColor(ColorType.SECONDARY.swatchIndex()));
    foregroundGradient.stops[0].set(getSwatchColor(ColorType.PRIMARY.swatchIndex()));
    foregroundGradient.stops[1].set(getSwatchColor(ColorType.SECONDARY.swatchIndex()));
    foregroundGradient.stops[2].set(getSwatchColor(ColorType.PRIMARY.swatchIndex()));
  }

  /**
   * Given a value in 0..1 (and wrapped back outside that range)
   * Return a color within the primaryGradient
   * @param lerp as a frac
   * @return LXColor
   */
  @Deprecated
  public int getPrimaryGradientColor(float lerp) {
    /* HSV2 mode wraps returned colors around the color wheel via the shortest
     * hue distance. In other words, we usually want a gradient to go from yellow
     * to red via orange, not via lime, green, cyan, blue, purple, red.
     */
    return primaryGradient.getColor(
            TEMath.trianglef(lerp / 2), // Allow wrapping
            LXGradientUtils.BlendMode.HSVM.function);
  }

  /**
   * Given a value in 0..1 (and wrapped back outside that range)
   * Return a color within the secondaryGradient
   * @param lerp
   * @return
   */
  @Deprecated
  public int getSecondaryGradientColor(float lerp) {
    /* HSV2 mode wraps returned colors around the color wheel via the shortest
     * hue distance. In other words, we usually want a gradient to go from yellow
     * to red via orange, not via lime, green, cyan, blue, purple, red.
     */
    return secondaryGradient.getColor(
            TEMath.trianglef(lerp / 2), // Allow wrapping
            LXGradientUtils.BlendMode.HSVM.function);
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
      if (this.modelTE.isGapPoint(point)) {
        colors[this.modelTE.getGapPointIndex()] = GAP_PIXEL_COLOR;
      } else {
        colors[point.index] = TEColor.TRANSPARENT;
      }
    }
  }

  // For patterns that only want to operate on edges
  public void setEdges(int color) {
    for (LXPoint point : this.modelTE.edgePoints) {
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
    for (TEPanelModel panel : this.modelTE.panelsById.values()) {
      if (panel.panelType.equals(TEPanelModel.SOLID)) {
        panel.virtualColor.rgb = colors[panel.points[0].index];
      }
    }
    for (TELaserModel laser : this.modelTE.lasersById.values()) {
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
