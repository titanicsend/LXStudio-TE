package titanicsend.color;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.color.LXSwatch;
import titanicsend.lx.LXGradientUtils;

/**
 * Calculates TE gradients once per frame in a global singleton
 */
public class TEGradientSource {

  private static TEGradientSource current;

  public static TEGradientSource get() {
    return current;
  }

  private final LX lx;

  // Keep black for building gradients
  private final LXDynamicColor black;

  private static LXGradientUtils.ColorStops initColorStops(int numStops) {
    LXGradientUtils.ColorStops colorStops = new LXGradientUtils.ColorStops();
    colorStops.setNumStops(numStops);
    return colorStops;
  }

  /**
   * Primary -> Secondary -> Tertiary -> (Wrap to Primary)
   */
  public LXGradientUtils.ColorStops normalGradient = initColorStops(4);
  /**
   * Primary -> Black -> (Wrap to Primary)
   */
  public LXGradientUtils.ColorStops darkGradient = initColorStops(3);

  public TEGradientSource(LX lx) {
    current = this;
    this.lx = lx;

    // Initialize a black dynamicColor that can be used every frame
    LXSwatch blackSwatch = new LXSwatch(lx);
    this.black = blackSwatch.getColor(0);
    this.black.primary.setColor(LXColor.BLACK);

    lx.engine.addLoopTask((p) -> {
      loop();
    });
  }

  /**
   * Refresh gradients from the global palette.
   * Called every engine loop.
   */
  private void loop() {
    updateGradients(this.lx.engine.palette.swatch);
  }

  private void updateGradients(LXSwatch swatch) {
    normalGradient.stops[0].set(swatch.getColor(TEColorType.PRIMARY.swatchIndex()));
    normalGradient.stops[1].set(swatch.getColor(TEColorType.SECONDARY.swatchIndex()));
    normalGradient.stops[2].set(swatch.getColor(TEColorType.TERTIARY.swatchIndex()));
    normalGradient.stops[3].set(swatch.getColor(TEColorType.PRIMARY.swatchIndex()));
    darkGradient.stops[0].set(swatch.getColor(TEColorType.PRIMARY.swatchIndex()));
    darkGradient.stops[1].set(this.black);
    darkGradient.stops[2].set(swatch.getColor(TEColorType.PRIMARY.swatchIndex()));
  }

}
