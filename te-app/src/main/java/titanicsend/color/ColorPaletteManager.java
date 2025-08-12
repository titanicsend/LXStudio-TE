package titanicsend.color;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.color.LXPalette;
import heronarts.lx.color.LXSwatch;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.parameter.TriggerParameter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@LXCategory(LXCategory.COLOR)
@LXComponentName("Color Palette Manager")
public class ColorPaletteManager extends LXComponent implements LXOscComponent {

  public static final String DEFAULT_SWATCH_NAME = "MANAGED";
  public static final int DEFAULT_SWATCH_INDEX = 0;
  public static final int SWATCH_COLORS = 3;

  public interface Listener {
    void color1Changed(LXDynamicColor color1);
  }

  private final List<Listener> listeners = new ArrayList<>();

  public final BooleanParameter isExpanded =
      new BooleanParameter("Expanded", false).setDescription("Whether the UI section is expanded");

  public final EnumParameter<PaletteStrategy> paletteStrategy =
      new EnumParameter<>("Palette Strategy", PaletteStrategy.ANALOGOUS)
          .setDescription(
              "Color theory rule to use when generating the secondary and tertiary colors");

  /** Name of the managed swatch in Chromatik global palette list */
  private final String swatchName;

  /** Position of the managed swatch in Chromatik's global palette list */
  private final int swatchIndex;

  /** Reference to the managed LXSwatch. */
  private LXSwatch managedSwatch;

  /** Reference to the first color in the managed swatch */
  private LXDynamicColor color1;

  public final TriggerParameter pushSwatch =
      new TriggerParameter("Push Swatch", this::pushToActiveSwatch)
          .setDescription("Push the managed swatch to the global active swatch");

  public final BooleanParameter pinSwatch =
      new BooleanParameter("Pin Swatch", false)
          .setDescription(
              "Pin the managed swatch to the global active swatch, "
                  + "so any changes are pushed to the global palette immediately.");

  public ColorPaletteManager(LX lx) {
    this(lx, DEFAULT_SWATCH_NAME, DEFAULT_SWATCH_INDEX);
  }

  public ColorPaletteManager(LX lx, String swatchName, int swatchIndex) {
    super(lx);

    this.swatchName = swatchName;
    this.swatchIndex = swatchIndex;

    this.lx.addProjectListener(this.projectListener);
    this.lx.engine.palette.addListener(this.globalPaletteListener);
    this.paletteStrategy.addListener(this.paletteStrategyListener);

    addParameter("isExpanded", this.isExpanded);
    addParameter("paletteStrategy", this.paletteStrategy);
    addParameter("pushSwatch", this.pushSwatch);
    addParameter("pinSwatch", this.pinSwatch);

    // Ensure the swatch is created and has the right name
    refreshManagedSwatch();
  }

  private final LX.ProjectListener projectListener =
      new LX.ProjectListener() {
        public void projectChanged(File file, Change change) {
          if (change == Change.NEW || change == Change.OPEN) {
            refreshManagedSwatch();
          }
        }
      };

  private final LXPalette.Listener globalPaletteListener =
      new LXPalette.Listener() {
        @Override
        public void swatchRemoved(LXPalette palette, LXSwatch swatch) {
          if (swatch == managedSwatch) {
            // Swatch deleted, by user or project close
            // We won't create a new one until it's needed again, in case this is a project close.
            setManagedSwatch(null);
          }
        }

        @Override
        public void swatchAdded(LXPalette palette, LXSwatch swatch) {}

        @Override
        public void swatchMoved(LXPalette palette, LXSwatch swatch) {}
      };

  private final LXSwatch.Listener managedSwatchListener =
      new LXSwatch.Listener() {
        @Override
        public void colorAdded(LXSwatch swatch, LXDynamicColor color) {
          // Check index 0 to see if a color was added at the front
          LXDynamicColor newColor1 = swatch.getColor(0);
          if (newColor1 != color1) {
            setColor1(newColor1);
          }
          // We could force a reduction to our exact number of colors, but it's not critical
        }

        @Override
        public void colorRemoved(LXSwatch swatch, LXDynamicColor color) {
          // Likely will not happen, the UI prevents deleting the first color
          if (color1 == color) {
            // Oh no, our color was removed! Grasp quickly for a remaining one.
            refreshColor1();
          }
          // TODO: Force our exact number of colors? Would this cause a problem on shutdown?
          // refreshNumSwatchColors();
          enforceNumSwatchColors();
          //      LX.error("Restore a deleted color here!");
        }
      };

  // Managed Swatch

  /** Update the reference `this.managedSwatch`, creating it if it doesn't exist. */
  private void refreshManagedSwatch() {
    if (this.managedSwatch == null
        || !this.lx.engine.palette.swatches.contains(this.managedSwatch)) {
      // ensure there are at least enough swatches in the global palette list to fetch
      // the correct index for this "managed swatch".
      while (this.lx.engine.palette.swatches.size() <= (this.swatchIndex + 1)) {
        this.lx.engine.palette.saveSwatch();
      }
      LXSwatch swatch = this.lx.engine.palette.swatches.get(this.swatchIndex);
      setManagedSwatch(swatch);
    }
  }

  private void setManagedSwatch(LXSwatch swatch) {
    if (this.managedSwatch != swatch) {
      if (this.managedSwatch != null) {
        unregisterManagedSwatch();
      }

      this.managedSwatch = swatch;

      if (this.managedSwatch != null) {
        // Make sure the new managed swatch has the correct number of colors
        enforceNumSwatchColors();
        // Set the correct label
        this.managedSwatch.label.setValue(this.swatchName);
        // Listen for colors added/removed
        registerManagedSwatch();
        // Lock on to the first color
        refreshColor1();
      } else {
        setColor1(null);
      }
    }
  }

  /** Add/Remove colors from the managed swatch until it matches our desired number */
  private void enforceNumSwatchColors() {
    while (this.managedSwatch.colors.size() < SWATCH_COLORS) {
      this.managedSwatch.addColor();
    }
    while (this.managedSwatch.colors.size() > SWATCH_COLORS) {
      this.managedSwatch.removeColor();
    }
  }

  private void registerManagedSwatch() {
    this.managedSwatch.addListener(this.managedSwatchListener);
  }

  private void unregisterManagedSwatch() {
    this.managedSwatch.removeListener(this.managedSwatchListener);
  }

  /** Accessor for UIColorPaletteManager */
  public LXSwatch getManagedSwatch() {
    return this.managedSwatch;
  }

  // Color 1

  private void refreshColor1() {
    if (this.managedSwatch != null && !this.managedSwatch.colors.isEmpty()) {
      setColor1(this.managedSwatch.getColor(0));
    } else {
      setColor1(null);
    }
  }

  /** "Color1" is an object: the first LXDynamicColor in the managed swatch */
  private void setColor1(LXDynamicColor color1) {
    if (this.color1 != color1) {
      if (this.color1 != null) {
        unregisterColor1();
      }
      this.color1 = color1;
      if (this.color1 != null) {
        this.color1.mode.setValue(LXDynamicColor.Mode.FIXED);
        registerColor1();
        refreshDownstream(false);
      }
      for (Listener listener : this.listeners) {
        listener.color1Changed(this.color1);
      }
    }
  }

  private void registerColor1() {
    this.color1.color.addListener(this.color1Listener, true);
  }

  private void unregisterColor1() {
    this.color1.color.removeListener(this.color1Listener);
  }

  /** H/S/B of color1 changed. Propagate updates. */
  private final LXParameterListener color1Listener =
      (p) -> {
        refreshDownstream(false);
      };

  private final LXParameterListener paletteStrategyListener =
      (p) -> {
        if (this.color1 != null) {
          refreshDownstream(false);
        }
      };

  // Colors 2 & 3

  private static final int INDEX_COLOR_2 = 1;
  private static final int INDEX_COLOR_3 = 2;

  private void refreshDownstream(boolean pushNow) {
    updateColors2And3();
    if (pushNow || this.pinSwatch.isOn()) {
      pushToActiveSwatch();
    }
  }

  private void updateColors2And3() {
    float h = this.color1.color.hue.getValuef();
    float s = this.color1.color.saturation.getValuef();
    float b = this.color1.color.brightness.getValuef();

    // Calculate colors 2 & 3 using the selected strategy
    PaletteStrategy paletteStrategy = this.paletteStrategy.getEnum();
    int color2 = paletteStrategy.getColor2(h, s, b);
    int color3 = paletteStrategy.getColor3(h, s, b);

    // Set colors 2 & 3 to the managed swatch, which does not make them live yet
    int numColors = this.managedSwatch.colors.size();
    if (numColors > 1) {
      this.managedSwatch.getColor(INDEX_COLOR_2).color.setColor(color2);
      if (numColors > 2) {
        this.managedSwatch.getColor(INDEX_COLOR_3).color.setColor(color3);
      }
    }
  }

  private void setColorAtPosition(TEColorType teColorType, int color) {
    this.managedSwatch.getColor(teColorType.swatchIndex()).primary.setColor(color);
    this.managedSwatch.getColor(teColorType.swatchIndex()).mode.setValue(LXDynamicColor.Mode.FIXED);
  }

  /**
   * Begins a transition of the colors in the global swatch to the colors in the managed swatch. The
   * managed swatch does not become the global. It remains a queue slot.
   */
  private void pushToActiveSwatch() {
    if (this.managedSwatch != null) {
      // TODO: transitions are skipped when two properties change in rapid sequence
      // from a grid click (which changes hue & sat and throws two events).
      // LXCommand -> SetColor needs to not set hue & sat separately.
      this.managedSwatch.recall.trigger();
    }
  }

  // Public setters

  /** External mechanism to set a new color, such as from Director midi controller */
  public void setColor(int color) {
    if (this.managedSwatch == null) {
      // Our swatch must have been deleted. Find/create a new one.
      refreshManagedSwatch();
    }

    if (this.color1 != null) {
      this.color1.color.setColor(color);
      refreshDownstream(true);
    }
  }

  // Listeners

  public ColorPaletteManager addListener(Listener listener) {
    return addListener(listener, false);
  }

  public ColorPaletteManager addListener(Listener listener, boolean fireNow) {
    if (this.listeners.contains(Objects.requireNonNull(listener))) {
      throw new IllegalStateException("ColorPaletteManager.Listener has already been added.");
    }
    listeners.add(listener);
    if (fireNow) {
      listener.color1Changed(this.color1);
    }
    return this;
  }

  public ColorPaletteManager removeListener(Listener listener) {
    if (!this.listeners.contains(Objects.requireNonNull(listener))) {
      throw new IllegalStateException("ColorPaletteManager.Listener has not been added.");
    }
    return this;
  }

  @Override
  public void dispose() {
    setManagedSwatch(null);
    this.lx.removeProjectListener(this.projectListener);
    this.lx.engine.palette.removeListener(this.globalPaletteListener);
    this.paletteStrategy.removeListener(this.paletteStrategyListener);
    super.dispose();
  }
}
