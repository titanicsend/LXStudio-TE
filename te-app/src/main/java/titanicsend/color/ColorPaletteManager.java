package titanicsend.color;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.*;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.TriggerParameter;

import java.io.File;

@LXCategory(LXCategory.COLOR)
@LXComponentName("Color Palette Manager")
public class ColorPaletteManager extends LXComponent implements LXOscComponent {

  public static final String DEFAULT_SWATCH_NAME = "PALETTE MANAGER";
  public static final int DEFAULT_SWATCH_INDEX = 0;
  public static final int SWATCH_COLORS = 3;

  public enum PaletteStrategy {
    ANALOGOUS("Analogous") {
      public int getColor2(float h, float s, float b) {
        return LXColor.hsb(h + 30, s, b);
      }
      public int getColor3(float h, float s, float b) {
        return LXColor.hsb(h - 30, s, b);
      }
    },

    GOLDEN_RATIO_CONJUGATE("Golden Ratio Conjugate") {
      private final float paletteOffset = 360f * 0.618f;
      public int getColor2(float h, float s, float b) {
        // color2 will be offset by 0.61803^2 x 360 == 137.5 degrees
        return LXColor.hsb(h + paletteOffset * 0.618f, s, b);
      }
      public int getColor3(float h, float s, float b) {
        // color3 will be offset by 0.61803 x 360 == 227.5 degrees
        return LXColor.hsb(h + paletteOffset, s, b);
      }
    },

    SPLIT_COMPLEMENTARY("Split Complementary") {
      public int getColor2(float h, float s, float b) {
        return LXColor.hsb(h + 150, s, b);

      }
      public int getColor3(float h, float s, float b) {
        return LXColor.hsb(h + 210, s, b);
      }
    },

    MONO("Mono") {
      public int getColor2(float h, float s, float b) {
        return LXColor.hsb(h, s, b);
      }
      public int getColor3(float h, float s, float b) {
        return LXColor.hsb(h, s, b);
      }
    };

    public final String label;

    PaletteStrategy(String label) {
      this.label = label;
    }

    abstract public int getColor2(float h, float s, float b);
    abstract public int getColor3(float h, float s, float b);
  }

  public final EnumParameter<PaletteStrategy> paletteStrategy =
      new EnumParameter<>("Palette Strategy", PaletteStrategy.ANALOGOUS)
          .setDescription("Color theory rule to use when generating the secondary and tertiary colors");

  /**
   * Name of the managed swatch in Chromatik global palette list
   */
  private final String swatchName;

  /**
   * Position of the managed swatch in Chromatik's global palette list
   */
  private final int swatchIndex;

/*
  public final CompoundParameter hue =
      new CompoundParameter("H", 0, 0, 360).setDescription("Sets the amount of hue shift to apply");

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
/*  private final LXParameterListener hsbListener = (p) -> {
    this.setColor1();
  };*/

  /**
   * Reference to the managed LXSwatch. Public so that it can be accessed from UIColorPaletteManager.
   */
  private LXSwatch managedSwatch;

  public LXDynamicColor color1; // = new ColorParameter("Color1", LXColor.BLACK);
/*
  public LXDynamicColor color2 = new ColorParameter("Color2", LXColor.BLACK);
  public LXDynamicColor color3 = new ColorParameter("Color3", LXColor.BLACK);
*/

  public final TriggerParameter pushSwatch =
      new TriggerParameter("Push Swatch", this::pushToActiveSwatch)
          .setDescription("Push the managed swatch to the global active swatch");

  public final BooleanParameter pinSwatch =
      new BooleanParameter("Pin Swatch", false)
          .setDescription("Pin the managed swatch to the global active swatch, " +
              "so any changes are pushed to the global palette immediately.");

  public ColorPaletteManager(LX lx) {
    this(lx, DEFAULT_SWATCH_NAME, DEFAULT_SWATCH_INDEX);
  }

  public ColorPaletteManager(LX lx, String swatchName, int swatchIndex) {
    super(lx);

    this.swatchName = swatchName;
    this.swatchIndex = swatchIndex;

    this.lx.addProjectListener(this.projectListener);
    this.paletteStrategy.addListener(paletteStrategyListener);
    this.lx.engine.palette.addListener(this.globalPaletteListener);

/*    addParameter("hue", this.hue);
    addParameter("saturation", this.saturation);
    addParameter("brightness", this.brightness);*/
    addParameter("paletteStrategy", this.paletteStrategy);
/*    addParameter("color1", this.color1);
    addParameter("color2", this.color2);
    addParameter("color3", this.color3);*/
    addParameter("pushSwatch", this.pushSwatch);
    addParameter("pinSwatch", this.pinSwatch);

/*    // Set color1 to current values of hue/saturation/brightness, so it doesn't default to black.
    this.setColor1();

    this.hue.addListener(hsbListener);
    this.saturation.addListener(hsbListener);
    this.brightness.addListener(hsbListener);*/
  }

  private void initialize() {
    // ensure the swatch is created, has the right name/num colors
    refreshManagedSwatch();
    refreshNumSwatchColors();

    registerManagedSwatch();
    registerColor1();
  }

  private final LX.ProjectListener projectListener = new LX.ProjectListener() {
    public void projectChanged(File file, Change change) {
      if (change == Change.NEW || change == Change.OPEN) {
        initialize();
      }
    }
  };

  private final LXPalette.Listener globalPaletteListener = new LXPalette.Listener() {
    @Override
    public void swatchRemoved(LXPalette palette, LXSwatch swatch) {
      if (swatch == managedSwatch && managedSwatch != null) {
        // Swatch deleted, by user or project close
        unregisterColor1();
        unregisterManagedSwatch();
        managedSwatch = null;
        // We won't create a new one until it's needed again, in case this is a project close.
      }
    }

    @Override
    public void swatchAdded(LXPalette palette, LXSwatch swatch) { }

    @Override
    public void swatchMoved(LXPalette palette, LXSwatch swatch) { }
  };

  private final LXSwatch.Listener managedSwatchListener = new LXSwatch.Listener() {
    @Override
    public void colorAdded(LXSwatch swatch, LXDynamicColor color) {
      // Track new index 0 in case a color was added at the front
      LXDynamicColor newColor1 = swatch.getColor(0);
      if (color1 != newColor1) {
        if (color1 != null) {
          unregisterColor1();
        }
        color1 = newColor1;
        registerColor1();
      }
      // Except, force our exact number of colors
      // TODO: would this cause a problem on shutdown?
      // refreshNumSwatchColors();
    }
    @Override
    public void colorRemoved(LXSwatch swatch, LXDynamicColor color) {
      if (color1 != null && color1 == color) {
        unregisterColor1();
      }
      // Force our exact number of colors
      // TODO: would this cause a problem on shutdown?
      // refreshNumSwatchColors();
    }
  };

  /**
   * H/S/B of color1 changed.  Propagate updates.
   */
  private final LXParameterListener color1Listener = (p) -> {
    refreshDownstream();
  };

  private final LXParameterListener paletteStrategyListener = (p) -> {
    refreshDownstream();
  };

  /**
   * Update the reference `this.managedSwatch`, creating it if it doesn't exist.
   */
  public void refreshManagedSwatch() {
    if (this.managedSwatch == null) {
      // ensure there are at least enough swatches in the global palette list to fetch
      // the correct index for this "managed swatch".
      while (this.lx.engine.palette.swatches.size() <= (this.swatchIndex + 1)) {
        this.lx.engine.palette.saveSwatch();
      }
      this.managedSwatch = this.lx.engine.palette.swatches.get(this.swatchIndex);
      this.managedSwatch.label.setValue(this.swatchName);
    }
  }

  private void refreshDownstream() {
    updateColors2And3();
    if (this.pinSwatch.isOn()) {
      pushToActiveSwatch();
    }
  }

  private void refreshNumSwatchColors() {
    while (this.managedSwatch.colors.size() < SWATCH_COLORS) {
      this.managedSwatch.addColor();
    }
    while (this.managedSwatch.colors.size() > SWATCH_COLORS) {
      this.managedSwatch.removeColor();
    }
  }

  private void registerColor1() {
    this.color1.mode.setValue(LXDynamicColor.Mode.FIXED);
    this.color1.color.addListener(this.color1Listener, true);
  }

  private void unregisterColor1() {
    this.color1.color.removeListener(this.color1Listener);
  }

  private void registerManagedSwatch() {
    this.managedSwatch.addListener(this.managedSwatchListener);
  }
  private void unregisterManagedSwatch() {
    this.managedSwatch.removeListener(this.managedSwatchListener);
  }


  /**
   * Set the color1 parameter based on the current values of hue, saturation, and brightness.
   */
/*
  private void setColor1() {
    this.color1.setColor(
        LXColor.hsb(
            this.hue.getValuef(), this.saturation.getValuef(), this.brightness.getValuef()));
  }
*/

  private static final int INDEX_COLOR_2 = 1;
  private static final int INDEX_COLOR_3 = 2;

  private void updateColors2And3() {
    float h = this.color1.color.hue.getValuef();
    float s = this.color1.color.saturation.getValuef();
    float b = this.color1.color.brightness.getValuef();
    PaletteStrategy paletteStrategy = this.paletteStrategy.getEnum();
    int color2 = paletteStrategy.getColor2(h, s, b);
    int color3 = paletteStrategy.getColor3(h, s, b);

    this.managedSwatch.getColor(INDEX_COLOR_2).color.setColor(color2);
    this.managedSwatch.getColor(INDEX_COLOR_3).color.setColor(color3);
  }

/*  private void updateManagedSwatch() {
    refreshManagedSwatch();
    setColorAtPosition(TEColorType.PRIMARY, this.color1.getColor());
    setColorAtPosition(TEColorType.SECONDARY, this.color2.getColor());
    setColorAtPosition(TEColorType.TERTIARY, this.color3.getColor());
  }*/

  private void setColorAtPosition(TEColorType teColorType, int color) {
    this.managedSwatch.getColor(teColorType.swatchIndex()).primary.setColor(color);
    this.managedSwatch.getColor(teColorType.swatchIndex()).mode.setValue(LXDynamicColor.Mode.FIXED);
  }

  public void pushToActiveSwatch() {
    this.managedSwatch.recall.trigger();
  }

  /**
   * Call this to set a new color, such as from Director midi controller
   */
  public void setColor(int color) {
    if (this.managedSwatch == null) {
      initialize();
    }

    this.color1.color.setColor(color);
    refreshDownstream();
  }

  /**
   * Accessor for UIColorPaletteManager
   */
  public LXSwatch getManagedSwatch() {
    return this.managedSwatch;
  }

  public String getSwatchName() {
    return this.swatchName;
  }

  @Override
  public void dispose() {
/*    this.hue.removeListener(hsbListener);
    this.saturation.removeListener(hsbListener);
    this.brightness.removeListener(hsbListener);
    this.color1.removeListener(paletteStrategyListener);*/
    this.paletteStrategy.removeListener(paletteStrategyListener);
    if (this.managedSwatch != null) {
      unregisterManagedSwatch();
    }
    if (this.color1 != null) {
      unregisterColor1();
    }
    super.dispose();
  }
}
