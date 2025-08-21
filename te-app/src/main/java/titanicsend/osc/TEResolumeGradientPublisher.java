package titanicsend.osc;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.color.LXPalette;
import heronarts.lx.color.LXSwatch;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameterListener;
import titanicsend.lx.LXGradientUtils;

/**
 * Minimal utility that listens to the active LX palette colors (primary/secondary) and logs
 * changes. This establishes the integration point for later OSC publishing.
 */
public class TEResolumeGradientPublisher extends LXComponent
    implements LXSwatch.Listener, LXPalette.Listener {

  private final LXSwatch activeSwatch;
  private LXDynamicColor color0;
  private LXDynamicColor color1;
  private boolean pendingLog0 = false;
  private boolean pendingLog1 = false;
  private boolean pendingPublish = false;
  // Global logging switch (disabled by default)
  private boolean enableLogging = false;

  public final BooleanParameter enabled =
      new BooleanParameter("Enabled", false).setDescription("Enable OSC publishing to Resolume");

  // Base OSC path for the Resolume effect
  private static final String OSC_EFFECT_BASE =
      "/composition/video/effects/colorize16palette/effect/";

  private final LXParameterListener color0Listener =
      (p) -> {
        scheduleLog(0);
      };

  private final LXParameterListener color1Listener =
      (p) -> {
        scheduleLog(1);
      };

  public TEResolumeGradientPublisher(LX lx) {
    super(lx);
    this.activeSwatch = lx.engine.palette.swatch;
    addParameter("enabled", this.enabled);
    bindToActiveSwatch();
  }

  private void bindToActiveSwatch() {
    this.activeSwatch.addListener(this);
    rebindColors();
  }

  private void unbindFromActiveSwatch() {
    this.activeSwatch.removeListener(this);
    unbindColorListeners();
  }

  private void unbindColorListeners() {
    if (this.color0 != null) {
      this.color0.color.removeListener(this.color0Listener);
      this.color0 = null;
    }
    if (this.color1 != null) {
      this.color1.color.removeListener(this.color1Listener);
      this.color1 = null;
    }
  }

  private void rebindColors() {
    unbindColorListeners();
    int numColors = this.activeSwatch.colors.size();
    if (numColors > 0) {
      this.color0 = this.activeSwatch.getColor(0);
      this.color0.color.addListener(this.color0Listener, true);
    }
    if (numColors > 1) {
      this.color1 = this.activeSwatch.getColor(1);
      this.color1.color.addListener(this.color1Listener, true);
    }
  }

  private void logColorChange(int index, LXDynamicColor dynamicColor) {
    if (dynamicColor == null) {
      return;
    }
    float h = dynamicColor.color.hue.getValuef();
    float s = dynamicColor.color.saturation.getValuef();
    float b = dynamicColor.color.brightness.getValuef();
    int cFromHSB = LXColor.hsb(h, s, b);
    String hex = String.format("#%06X", (0xFFFFFF & cFromHSB));
    LX.log(
        String.format(
            "Palette color%d changed: %s hsb(%.1f, %.1f, %.1f) rgb(%d,%d,%d)",
            index,
            hex,
            h,
            s,
            b,
            (cFromHSB & LXColor.R_MASK) >>> LXColor.R_SHIFT,
            (cFromHSB & LXColor.G_MASK) >>> LXColor.G_SHIFT,
            (cFromHSB & LXColor.B_MASK)));
  }

  private void scheduleLog(int index) {
    // Debounce/coalesce: a single palette color change emits multiple parameter events
    // (hue, saturation, brightness). Scheduling onto the engine task queue ensures
    // we log only once per user change rather than 2-3 times, and do so on the
    // engine thread after all parameter updates have settled for this tick.
    if (enableLogging) {
      if (index == 0) {
        if (pendingLog0) return;
        pendingLog0 = true;
        lx.engine.addTask(
            () -> {
              pendingLog0 = false;
              logColorChange(0, this.color0);
            });
      } else if (index == 1) {
        if (pendingLog1) return;
        pendingLog1 = true;
        lx.engine.addTask(
            () -> {
              pendingLog1 = false;
              logColorChange(1, this.color1);
            });
      }
    }

    // Also coalesce OSC publishing to one action after changes to either color0 or color1
    if (!pendingPublish) {
      pendingPublish = true;
      lx.engine.addTask(
          () -> {
            pendingPublish = false;
            publishGradientIfReady();
          });
    }
  }

  private boolean canSendOsc() {
    return this.enabled.isOn()
        && this.lx != null
        && this.lx.engine != null
        && this.lx.engine.osc != null;
  }

  private void sendOsc(String address, float value) {
    if (canSendOsc()) {
      lx.engine.osc.sendMessage(address, value);
    }
  }

  // Build a 16-color OKLab gradient with these anchors:
  // index 0 = black, index 4 = color0, index 15 = color1
  private void publishGradientIfReady() {
    if (this.color0 == null || this.color1 == null) return;

    // Prepare ColorStops for OKLab blending
    LXGradientUtils.ColorStop stopBlack = new LXGradientUtils.ColorStop();
    stopBlack.setRGB(LXColor.BLACK);
    LXGradientUtils.ColorStop stop0 = new LXGradientUtils.ColorStop();
    stop0.set(this.color0);
    LXGradientUtils.ColorStop stop1 = new LXGradientUtils.ColorStop();
    stop1.set(this.color1);

    for (int i = 0; i < 16; i++) {
      int color;
      if (i <= 4) {
        float t = (i) / 4f; // 0..1 from black to color0
        // Ease-out cubic for smoother approach to color0 at index 4
        float tEase = 1f - (1f - t) * (1f - t) * (1f - t);
        color = LXGradientUtils.BlendFunction.OKLAB.blend(stopBlack, stop0, tEase);
      } else {
        float t = (i - 4) / 11f; // 0..1 from color0 to color1 over indices 4..15
        color = LXGradientUtils.BlendFunction.OKLAB.blend(stop0, stop1, t);
      }

      float hueNorm = LXColor.h(color) / 360f;
      float satNorm = LXColor.s(color) / 100f;
      float briNorm = LXColor.b(color) / 100f;

      String base = OSC_EFFECT_BASE + "lxcolor" + (i + 1);
      sendOsc(base + "/hue", hueNorm);
      sendOsc(base + "/saturation", satNorm);
      sendOsc(base + "/brightness", briNorm);
    }
  }

  // LXSwatch.Listener
  @Override
  public void colorAdded(LXSwatch swatch, LXDynamicColor color) {
    // Rebind when colors are added to ensure we track index 0 and 1 only
    rebindColors();
  }

  @Override
  public void colorRemoved(LXSwatch swatch, LXDynamicColor color) {
    rebindColors();
  }

  // LXPalette.Listener
  @Override
  public void swatchAdded(LXPalette palette, LXSwatch swatch) {}

  @Override
  public void swatchRemoved(LXPalette palette, LXSwatch swatch) {}

  @Override
  public void swatchMoved(LXPalette palette, LXSwatch swatch) {}

  @Override
  public void dispose() {
    unbindFromActiveSwatch();
    super.dispose();
  }
}
