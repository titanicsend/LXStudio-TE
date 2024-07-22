package titanicsend.color;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.color.LXSwatch;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.TriggerParameter;

@LXCategory(LXCategory.COLOR)
@LXComponentName("Color Palette Manager")
public class ColorPaletteManager extends LXComponent {

  public static final String DEFAULT_SWATCH_NAME = "SWATCH A";
  public static final int DEFAULT_SWATCH_INDEX = 0;

  public enum PaletteStrategy {
    MONO,
    ANALOGOUS,
    GOLDEN_RATIO_CONJUGATE,
    SPLIT_COMPLEMENTARY,
    COMPLEMENTARY,
    TRIADIC,
  }

  public final EnumParameter<PaletteStrategy> paletteStrategy =
      new EnumParameter<>("Palette Strategy", PaletteStrategy.TRIADIC)
          .setDescription(
              "Color theory rule to use when generating the secondary and tertiary colors");

  public final CompoundParameter hue =
      new CompoundParameter("H", 0, 0, 360)
          .setDescription("Sets the amount of hue shift to apply");

  public final CompoundParameter saturation =
      new CompoundParameter("S", 100, 0, 100)
          .setUnits(CompoundParameter.Units.PERCENT)
          .setDescription("Sets the amount to increase or decrease saturation");

  public final CompoundParameter brightness =
      new CompoundParameter("B", 100, 0, 100)
          .setUnits(CompoundParameter.Units.PERCENT)
          .setDescription("Sets the amount to increase or decrease brightness");

  /**
   * If hue, saturation, or brightness change, update color1
   */
  private final LXParameterListener colorListener = (p) -> {
    this.color1.setColor(
        LXColor.hsb(
            this.hue.getValuef(), this.saturation.getValuef(), this.brightness.getValuef()));
  };

  public final ColorParameter color1 = new ColorParameter("Color1", LXColor.BLACK);
  public final ColorParameter color2 = new ColorParameter("Color2", LXColor.BLACK);
  public final ColorParameter color3 = new ColorParameter("Color3", LXColor.BLACK);

  /**
   * If color1 or palette strategy change, update colors 2 and 3
   */
  private final LXParameterListener paletteListener = (p) -> {
    updateColors2And3();
    updateManagedSwatch();
    if (this.pinSwatch.isOn()) {
      pushToActiveSwatch();
    }
  };

  public final TriggerParameter pushSwatch =
      new TriggerParameter("Push Swatch", this::pushToActiveSwatch)
          .setDescription("Push the managed swatch to the global active swatch");

  public final BooleanParameter pinSwatch =
      new BooleanParameter("Pin Swatch", false)
          .setDescription("Pin the managed swatch to the global active swatch, " +
              "so any changes are reflected in the global palette immediately.");

  /**
   * Name of the managed swatch in Chromatik global palette list
   */
  private final String swatchName;

  /**
   * Position of the managed swatch in Chromatik's global palette list
   */
  private final int swatchIndex;

  /**
   * Pointer to the managed LXSwatch. Public so that it can be accessed from UIColorPaletteManager.
   */
  public LXSwatch managedSwatch;

  public ColorPaletteManager(LX lx) {
    this(lx, DEFAULT_SWATCH_NAME, DEFAULT_SWATCH_INDEX);
  }

  public ColorPaletteManager(LX lx, String swatchName, int swatchIndex) {
    super(lx);

    this.swatchName = swatchName;
    this.swatchIndex = swatchIndex;

    // ensure the swatch is created, has the right name/num colors
    refreshManagedSwatch();

    addParameter("hue", this.hue);
    addParameter("saturation", this.saturation);
    addParameter("brightness", this.brightness);
    addParameter("paletteStrategy", this.paletteStrategy);
    addParameter("color1", this.color1);
    addParameter("color2", this.color2);
    addParameter("color3", this.color3);
    addParameter("pushSwatch", this.pushSwatch);
    addParameter("pinSwatch", this.pinSwatch);

    this.hue.addListener(colorListener);
    this.saturation.addListener(colorListener);
    this.brightness.addListener(colorListener);
    this.color1.addListener(paletteListener);
    this.paletteStrategy.addListener(paletteListener);
  }

  /**
   * Update the reference `this.managedSwatch`, creating it if it doesn't exist.
   */
  public void refreshManagedSwatch() {
    // ensure there are at least enough swatches in the global palette list to fetch
    // the correct index for this "managed swatch".
    while (this.lx.engine.palette.swatches.size() <= (this.swatchIndex + 1)) {
      this.lx.engine.palette.saveSwatch();
    }
    this.managedSwatch = this.lx.engine.palette.swatches.get(this.swatchIndex);
    this.managedSwatch.label.setValue(this.swatchName);
    if (this.managedSwatch.colors.size() < LXSwatch.MAX_COLORS) {
      for (int i = this.managedSwatch.colors.size(); i < LXSwatch.MAX_COLORS; ++i) {
        this.managedSwatch.addColor();
      }
    }
  }

  private void updateColors2And3() {
    float hue = this.color1.hue.getValuef();
    float saturation = this.color1.saturation.getValuef();
    float brightness = this.color1.brightness.getValuef();
    int color2;
    int color3;
    switch(this.paletteStrategy.getEnum()) {
      case MONO:
        color2 = LXColor.BLACK;
        color3 = LXColor.BLACK;
        break;
      case GOLDEN_RATIO_CONJUGATE:
        color2 = LXColor.hsb(hue * 0.6180339, saturation, brightness);
        color3 = LXColor.BLACK;
        break;
      case COMPLEMENTARY:
        color2 = LXColor.hsb(hue + 180, saturation, brightness);
        color3 = LXColor.BLACK;
        break;
      case SPLIT_COMPLEMENTARY:
        color2 = LXColor.hsb(hue + 150, saturation, brightness);
        color3 = LXColor.hsb(hue + 210, saturation, brightness);
        break;
      case TRIADIC:
        color2 = LXColor.hsb(hue + 120, saturation, brightness);
        color3 = LXColor.hsb(hue + 240, saturation, brightness);
        break;
      case ANALOGOUS:
        color2 = LXColor.hsb(hue + 30, saturation, brightness);
        color3 = LXColor.hsb(hue - 30, saturation, brightness);
        break;
      default:
        throw new RuntimeException("Unknown PaletteStrategy: " + this.paletteStrategy.getEnum());
    }
    this.color2.setColor(color2);
    this.color3.setColor(color3);
  }

  private void updateManagedSwatch() {
    refreshManagedSwatch();
    setColorAtPosition(TEColorType.PRIMARY, this.color1.getColor());
    setColorAtPosition(TEColorType.SECONDARY, this.color2.getColor());
    setColorAtPosition(TEColorType.SECONDARY_BACKGROUND, this.color3.getColor());
  }

  private void setColorAtPosition(TEColorType teColorType, int color) {
    this.managedSwatch.getColor(teColorType.swatchIndex()).primary.setColor(color);
    this.managedSwatch.getColor(teColorType.swatchIndex()).mode.setValue(LXDynamicColor.Mode.FIXED);
  }

  public void pushToActiveSwatch() {
    this.managedSwatch.recall.trigger();
  }

  @Override
  public void dispose() {
    this.hue.removeListener(colorListener);
    this.saturation.removeListener(colorListener);
    this.brightness.removeListener(colorListener);
    this.color1.removeListener(paletteListener);
    this.paletteStrategy.removeListener(paletteListener);
    super.dispose();
  }
}
