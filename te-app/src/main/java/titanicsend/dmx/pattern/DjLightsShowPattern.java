package titanicsend.dmx.pattern;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.color.TEColorType;
import titanicsend.dmx.model.AdjStealthModel;
import titanicsend.dmx.model.DmxModel;
import titanicsend.ui.UIUtils;

/**
 * Streamlined DJ lights pattern for show control with essential parameters only.
 * Extends DjLightsPattern for DMX control and TEPattern's palette/swatch integration.
 * Provides clean interface with color control, saturation adjustment, and core positioning.
 */
@LXCategory("DMX")
public class DjLightsShowPattern extends DjLightsPattern
    implements UIDeviceControls<DjLightsShowPattern> {

  // Core visible controls - override the pan/tilt from parent to add descriptions
  public final CompoundParameter brightness =
      new CompoundParameter("Brightness", 0.5)
          .setDescription("Overall brightness (master dimmer)");

  // Use LinkedColorParameter for full palette/swatch integration
  public final LinkedColorParameter color =
      new LinkedColorParameter("Color")
          .setDescription("Light color from palette/swatch");

  public final CompoundParameter saturation =
      new CompoundParameter("Saturation", 1.0)
          .setDescription("Additional saturation adjustment (0=grayscale, 1=full color)");

  public DjLightsShowPattern(LX lx) {
    super(lx);

    // Add visible parameters first (LinkedColorParameter needs parent before configuration)
    addParameter("pan", this.pan);
    addParameter("tilt", this.tilt);
    addParameter("focus", this.focus);
    addParameter("brightness", this.brightness);
    addParameter("color", this.color);
    addParameter("saturation", this.saturation);

    // Configure color parameter AFTER adding it to the pattern
    this.color.mode.setValue(LinkedColorParameter.Mode.PALETTE);
    this.color.index.setValue(TEColorType.PRIMARY.index);

    // Set defaults
    this.brightness.setValue(0.5);
    this.focus.setValue(0.5);

    // Configure remote controls for MIDI/OSC mapping
    this.setCustomRemoteControls(
        new LXListenableNormalizedParameter[] {
          this.pan,
          this.tilt,
          this.focus,
          this.brightness,
          this.color.hue,
          this.color.saturation,
          this.color.brightness,
          this.saturation
        });
  }

  /**
   * Applies saturation adjustment to a color.
   * @param baseColor The original color
   * @param saturationMultiplier Saturation factor (0-1)
   * @return Modified color with adjusted saturation
   */
  private int applySaturation(int baseColor, double saturationMultiplier) {
    float h = LXColor.h(baseColor);
    float s = LXColor.s(baseColor);
    float b = LXColor.b(baseColor);

    // Apply saturation multiplier
    s = (float)(s * saturationMultiplier);
    s = Math.max(0, Math.min(100, s)); // Clamp to 0-100

    return LXColor.hsb(h, s, b);
  }

  /**
   * Converts an LX color to RGBW values for DMX lights.
   * Extracts white component and adjusts RGB accordingly.
   */
  private static class RGBWColor {
    int r, g, b, w;

    RGBWColor(int color, double brightnessMultiplier) {
      // Extract RGB components
      int red = (color >> 16) & 0xff;
      int green = (color >> 8) & 0xff;
      int blue = color & 0xff;

      // Calculate white component (minimum of RGB)
      w = Math.min(Math.min(red, green), blue);

      // Subtract white from RGB to get pure colors
      r = red - w;
      g = green - w;
      b = blue - w;

      // Apply brightness multiplier
      r = (int)(r * brightnessMultiplier);
      g = (int)(g * brightnessMultiplier);
      b = (int)(b * brightnessMultiplier);
      w = (int)(w * brightnessMultiplier);
    }
  }

  @Override
  protected void run(double deltaMs) {
    // Get parameter values
    double panValue = this.pan.getNormalized();
    double tiltValue = this.tilt.getNormalized();
    double focusValue = this.focus.getNormalized();
    double brightnessValue = this.brightness.getNormalized();
    double saturationValue = this.saturation.getNormalized();

    // Get color from LinkedColorParameter which handles palette/swatch integration
    int baseColor = this.color.calcColor();

    // Apply saturation adjustment
    int adjustedColor = applySaturation(baseColor, saturationValue);

    // Convert to RGBW with brightness applied
    RGBWColor rgbw = new RGBWColor(adjustedColor, brightnessValue);

    // Apply to all beacon fixtures
    for (DmxModel d : this.modelTE.getBeacons()) {
      if (d instanceof AdjStealthModel) {
        // Position controls
        setDmxNormalized(d, AdjStealthModel.INDEX_PAN, panValue);
        setDmxNormalized(d, AdjStealthModel.INDEX_TILT, tiltValue);

        // Color controls
        setDmxValue(d, AdjStealthModel.INDEX_RED, rgbw.r);
        setDmxValue(d, AdjStealthModel.INDEX_GREEN, rgbw.g);
        setDmxValue(d, AdjStealthModel.INDEX_BLUE, rgbw.b);
        setDmxValue(d, AdjStealthModel.INDEX_WHITE, rgbw.w);

        // Brightness (using master dimmer channel)
        setDmxNormalized(d, AdjStealthModel.INDEX_DIMMER, brightnessValue);

        // Focus
        setDmxNormalized(d, AdjStealthModel.INDEX_FOCUS, focusValue);

        // Fixed values for hidden parameters
        setDmxValue(d, AdjStealthModel.INDEX_SHUTTER, AdjStealthModel.SHUTTER_OPEN); // Always open
        setDmxValue(d, AdjStealthModel.INDEX_COLOR_TEMP, 0); // Default color temp
        setDmxValue(d, AdjStealthModel.INDEX_COLOR_EFFECT, 0); // No color effects
        setDmxValue(d, AdjStealthModel.INDEX_COLOR_FADE, 0); // No color fade
        setDmxValue(d, AdjStealthModel.INDEX_PT_SPEED, 0); // Instant pan/tilt movement
        setDmxValue(d, AdjStealthModel.INDEX_PROGRAMS, 0); // No programs
      }
    }

    for (LXPoint point : this.lx.getModel().points) {
      colors[point.index] = LXColor.hsb(
          LXColor.h(adjustedColor),
          LXColor.s(adjustedColor) * (float)saturationValue,
          LXColor.b(adjustedColor) * (float)brightnessValue);
    }
  }

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, DjLightsShowPattern device) {
    UIUtils.buildMftStyleDeviceControls(ui, uiDevice, device);
  }
}