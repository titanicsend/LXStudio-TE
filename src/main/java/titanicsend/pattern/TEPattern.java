package titanicsend.pattern;

import static java.lang.Math.PI;
import static java.lang.Math.sin;

import heronarts.lx.LX;
import heronarts.lx.Tempo;
import heronarts.lx.audio.GraphicMeter;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.FunctionalParameter;
import heronarts.lx.parameter.LXListenableParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.studio.TEApp;
import java.util.*;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import titanicsend.color.TEColorType;
import titanicsend.dmx.pattern.DmxPattern;
import titanicsend.model.TELaserModel;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;
import titanicsend.util.TEColor;

public abstract class TEPattern extends DmxPattern {
  private final TEPanelModel sua;
  private final TEPanelModel sdc;

  protected final TEWholeModel modelTE;

  public TEWholeModel getModelTE() {
    return this.modelTE;
  }

  public final TriggerParameter captureDefaults =
    new TriggerParameter("SetDefaults", this::captureDefaults)
    .setDescription("Set current parameter values as the default values for this pattern instance");

  private final Map<String, Double> defaults  = new LinkedHashMap<String, Double>();

  protected TEPattern(LX lx) {
    super(lx);
    this.modelTE = TEApp.wholeModel;

    this.sua = this.modelTE.getPanel("SUA");
    this.sdc = this.modelTE.getPanel("SDC");

    addParameter("setDefaults", this.captureDefaults);
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
  public LinkedColorParameter registerColor(
      String label, String path, TEColorType colorType, String description) {
    LinkedColorParameter lcp = new LinkedColorParameter(label).setDescription(description);
    addParameter(path, lcp);
    lcp.mode.setValue(LinkedColorParameter.Mode.PALETTE);
    lcp.index.setValue(colorType.index);
    return lcp;
  }

  /**
   * Get a ColorType's color from the Swatch
   *
   * @param type
   * @return
   */
  public int getSwatchColor(TEColorType type) {
    return lx.engine.palette.getSwatchColor(type.swatchIndex()).getColor();
  }

  // During construction, make gap points show up in red
  public static final int GAP_PIXEL_COLOR = TEColor.TRANSPARENT;

  // Compare to LXLayeredComponent's clearColors(), which is declared final.
  public void clearPixels() {
    for (LXPoint point : this.model.points) {
      if (this.modelTE.isGapPoint(point)) {
        colors[point.index] = GAP_PIXEL_COLOR;
      } else {
        colors[point.index] = TEColor.TRANSPARENT;
      }
    }
  }

  // For patterns that only want to operate on edges
  public void setEdges(int color) {
    for (LXPoint point : this.modelTE.getEdgePoints()) {
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
    for (TEPanelModel panel : this.modelTE.getPanels()) {
      if (panel.panelType.equals(TEPanelModel.SOLID)) {
        panel.virtualColor.rgb = colors[panel.points[0].index];
      }
    }
    for (TELaserModel laser : this.modelTE.getLasers()) {
      laser.control.update(deltaMsec);
      laser.color = colors[laser.model.points[0].index];
    }
  }

  /*
   *  Audio and tempo methods
   */

  /**
   * Get the fraction into a measure, assuming a four beat measure
   *
   * @return 0..1 ramp of progress (fraction) into the current measure
   */
  public double wholeNote() {
    return lx.engine.tempo.getBasis(Tempo.Division.WHOLE);
  }

  /**
   * Get the fraction into a musical phrase, assuming 8 * 4 beat phrases
   *
   * @return 0..1 ramp of progress (fraction) into the current phrase
   */
  public double phrase() {
    return lx.engine.tempo.getCompositeBasis() / 32 % 1.0D;
  }

  // Sine modulator alternative between 0 and 1 on beat
  public double sinePhaseOnBeat() {
    return .5 * sin(PI * lx.engine.tempo.getCompositeBasis()) + .5;
  }

  /**
   * Get the fraction into a measure for any defined measure length
   *
   * @return 0..1 ramp of progress (fraction) into the current measure
   */
  public double measure() {
    return (lx.engine.tempo.getCompositeBasis()
        % lx.engine.tempo.beatsPerBar.getValue()
        / lx.engine.tempo.beatsPerBar.getValue());
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
  @Deprecated
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

  /**
   * Set all current parameter values as the defaults for this pattern instance
   */
  public void captureDefaults() {
    for (LXParameter p : this.getParameters()) {
      if (p instanceof LXListenableParameter && !isHiddenControl(p)) {
        captureDefault((LXListenableParameter)p);
      }
    }
  }

  /**
   * Set a parameter's current value as its default and remember new default for file save/load.
   */
  protected void captureDefault(LXListenableParameter p) {
    if (p.getParent() != this) {
      throw new UnsupportedOperationException("Can not apply default value, parameter is not child of pattern.");
    }

    if (p instanceof StringParameter) {
      // Placeholder: StringParameter does not have a public method for setting the default value
    } else {
      // Use base value for modulated parameters
      double value = p instanceof CompoundParameter ? ((CompoundParameter)p).getBaseValue() : p.getValue();
      this.defaults.put(p.getPath(), value);
      ((LXListenableParameter)p).reset(value);
    }
  }

  private static final String KEY_DEFAULTS = "defaults";

  @Override
  public void save(LX lx, JsonObject obj) {
    super.save(lx, obj);
    obj.add(KEY_DEFAULTS, toObject(lx, this.defaults));
  }

  public static JsonObject toObject(LX lx, Map<String, Double> map) {
    JsonObject obj = new JsonObject();
    for (Entry<String, Double> entry : map.entrySet()) {
      obj.addProperty(entry.getKey(), entry.getValue());
    }
    return obj;
  }

  @Override
  public void load(LX lx, JsonObject obj) {
    super.load(lx, obj);

    this.defaults.clear();
    if (obj.has(KEY_DEFAULTS)) {
      JsonObject defaultsObject = obj.getAsJsonObject(KEY_DEFAULTS);
      for (Entry<String, JsonElement> defaultEntry : defaultsObject.entrySet()) {
        loadDefault(defaultEntry.getKey(), defaultEntry.getValue());
      }
    }
  }

  private void loadDefault(String path, JsonElement defaultElement) {
    LXParameter parameter = this.getParameter(path);
    if (parameter == null) {
      LX.error("Parameter " + path + " not found, default value will be discarded");
      return;
    } else if (!(parameter instanceof LXListenableParameter)) {
      LX.error(
          "Unable to restore default value, parameter " + path + " is not LXListenableParameter");
      return;
    } else if (parameter instanceof StringParameter || parameter instanceof FunctionalParameter) {
      LX.error("Unable to restore default value, parameter " + path + " is invalid type");
      return;
    }

    try {
      Double value = defaultElement.getAsDouble();
      this.defaults.put(path, value);
      // Calling reset(value) overrides the current value that was just loaded from file.
      // To avoid, uncomment these two lines:
      // double currentValue = parameter.getValue();
      ((LXListenableParameter) parameter).reset(value);
      // parameter.setValue(currentValue);
    } catch (Exception x) {
      LX.error(
          x,
          "Invalid format loading default parameter value "
              + path
              + " from JSON value: "
              + defaultElement);
    }
  }

  /**
   * utility method for use during the static-to-dynamic model transition.
   * IMPORTANT:  There is a performance cost to this, so it should be
   * removed when we no longer need to support the static model.
   */
   public float getXn(LXPoint p) {
    return (modelTE.isStatic()) ? p.zn : p.xn;
  }
}
