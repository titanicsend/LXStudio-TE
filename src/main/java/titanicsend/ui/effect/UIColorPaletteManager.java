package titanicsend.ui.effect;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UIColor;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.glx.ui.component.UIColorPicker;
import heronarts.glx.ui.component.UIDoubleBox;
import heronarts.glx.ui.component.UIDropMenu;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.component.UISlider;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXSwatch;
import heronarts.lx.studio.ui.device.UIControls;
import heronarts.lx.studio.ui.global.UIPalette;
import java.lang.reflect.Field;
import titanicsend.color.ColorPaletteManager;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.utils.LXUtils;


public class UIColorPaletteManager extends UICollapsibleSection implements UIControls {
  private static final int LABEL_WIDTH = 56;
  private static final float SLIDER_WIDTH = 120.0F;
  private static final float GRADIENT_HEIGHT = 8.0F;
  // WIP(look): feature flags for UI elements I'm unsure about / experimenting with.
  private static final boolean DISPLAY_MANAGED_SWATCH_ROWS = false;
  private static final boolean DISPLAY_GRADIENTS_ABOVE_SLDIERS = false;

  private final float width;
  private LXSwatch cueSwatch;
  private LXSwatch auxSwatch;

  public UIColorPaletteManager(LXStudio.UI ui, ColorPaletteManager cueMgr, ColorPaletteManager auxMgr, float w, float xOffset) {
    super(ui, xOffset, 0, w, 0);
    this.width = w;

    this.setLayout(Layout.VERTICAL, 2F);
    this.setPadding(2, 0);
    this.setTitle("PALETTE MANAGER");

    cueSwatch = cueMgr.managedSwatch();
    auxSwatch = auxMgr.managedSwatch();
    UI2dContainer swatchDisplayContainer = UI2dContainer.newVerticalContainer(this.width-12, 6F);
    swatchRow(ui, ui.lx.engine.palette.swatch, "Act").addToContainer(swatchDisplayContainer);


    if (DISPLAY_MANAGED_SWATCH_ROWS) {
        // TODO: these don't seem to get updated as the palette updates - just stay red in the UI
        swatchRow(ui, cueSwatch, "Cue").addToContainer(swatchDisplayContainer);
        if (auxMgr != null) {
            swatchRow(ui, auxSwatch, "Aux").addToContainer(swatchDisplayContainer);
        }
    }

    swatchDisplayContainer.addToContainer(this);

    horizontalBreak(ui, this.width).addToContainer(this);

    buildPaletteSelectionRow(cueMgr).addToContainer(this);

    horizontalBreak(ui, this.width).addToContainer(this);

    buildColorSlidersRow(cueMgr).addToContainer(this);

    this.addListener(
        cueMgr.toggleCue,
        (p) -> {
          cueMgr.updateSwatches();
//          cueSwatch = cueMgr.managedSwatch();
        });

    if (auxMgr != null) {
      horizontalBreak(ui, this.width).addToContainer(this);

      buildPaletteSelectionRow(auxMgr).addToContainer(this);

      horizontalBreak(ui, this.width).addToContainer(this);

      buildColorSlidersRow(auxMgr).addToContainer(this);

      this.addListener(
          auxMgr.toggleCue,
          (p) -> {
            auxMgr.updateSwatches();
//            auxSwatch = auxMgr.managedSwatch();
          });
    }
  }

  private UI2dContainer swatchRow(UI ui, LXSwatch swatch, String label) {
    UI2dContainer elem = row(label, 20,
        new UIPalette.Swatch(ui, swatch, 0, 0, 80, UIColorPicker.Corner.TOP_LEFT)
    );
    elem.setPadding(2F)
        .setBorderRounding(2)
        .setBackgroundColor(ui.theme.listItemBackgroundColor)
        .setWidth(this.width-12)
        .setHeight(24);
    return elem;
  }

  private UI2dContainer buildPaletteSelectionRow(ColorPaletteManager paletteMgr) {
    UI2dContainer paletteSelectionRow = UI2dContainer.newHorizontalContainer(40F, 2F);

    float swatchHeight = 40F;
    float swatchWidth = swatchHeight;

    addColumn(
        paletteSelectionRow,
        newHierarchichalSwatch(paletteMgr, swatchWidth, swatchHeight)
    ).setWidth(swatchWidth);

    addColumn(
        paletteSelectionRow,
        newKnob(paletteMgr.paletteType)
    );

    addColumn(
        paletteSelectionRow,
        newButton(paletteMgr.toggleCue, 40F)
            .setActiveLabel("ON")
            .setInactiveLabel("SWAP")
            .setMomentary(true)
    );

    return paletteSelectionRow;
  }

  private UI2dContainer newHierarchichalSwatch(ColorPaletteManager paletteMgr, float totalWidth, float totalHeight) {
    float swatchChildSpacing = 2F;
    float swatchTotalHeight = totalHeight - swatchChildSpacing/2;
    float swatchSegmentHeight = swatchTotalHeight / 2;
    float swatchSegmentWidth = (totalWidth/2) - (swatchChildSpacing/2);

    UI2dContainer squareSwatch = UI2dContainer.newVerticalContainer(
        totalWidth,
        swatchChildSpacing,
        new UISingleColorDisplay(paletteMgr.color1, totalWidth, swatchSegmentHeight),
        UI2dContainer.newHorizontalContainer(
            swatchSegmentHeight,
            2F,
            new UISingleColorDisplay(paletteMgr.color2, swatchSegmentWidth, swatchSegmentHeight),
            new UISingleColorDisplay(paletteMgr.color3, swatchSegmentWidth, swatchSegmentHeight)
        )
    );
    return squareSwatch;
  }

  private UI2dContainer buildColorSlidersRow(ColorPaletteManager paletteMgr) {
    float SLIDER_SPACING = 4;
    float controlWidth = this.width / 3;
    float sliderWidth = controlWidth - (2*SLIDER_SPACING);
    float height = 42;
    UI2dContainer colorSlidersRow = UI2dContainer.newHorizontalContainer(height, SLIDER_SPACING);
    if (DISPLAY_GRADIENTS_ABOVE_SLDIERS) {
      addColumn(
          colorSlidersRow,
          new UIHueDisplay(paletteMgr.hue, sliderWidth),
          new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, sliderWidth, 12.0F, paletteMgr.hue)
      );
      addColumn(
          colorSlidersRow,
          new UISaturationDisplay(sliderWidth),
          new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, sliderWidth, 12.0F, paletteMgr.saturation)
      );
      addColumn(
          colorSlidersRow,
          new UIBrightnessDisplay(sliderWidth),
          new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, sliderWidth, 12.0F, paletteMgr.brightness)
      );
    } else {
      addColumn(
          colorSlidersRow,
          new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, sliderWidth, 12.0F, paletteMgr.hue)
      );
      addColumn(
          colorSlidersRow,
          new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, sliderWidth, 12.0F, paletteMgr.saturation)
      );
      addColumn(
          colorSlidersRow,
          new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, sliderWidth, 12.0F, paletteMgr.brightness)
      );
    }

    return colorSlidersRow;
  }

  private UI2dContainer row(String label, UI2dComponent component) {
    return row(label, LABEL_WIDTH, component);
  }

  private UI2dContainer row(String label, int labelWidth, UI2dComponent component) {
    return UI2dContainer.newHorizontalContainer(
        16, 4, new UILabel(labelWidth, 12, label), component);
  }

  public static void addToLeftGlobalPane(LXStudio.UI ui, ColorPaletteManager cueMgr, ColorPaletteManager auxMgr) {
    UI2dContainer parentContainer = ui.leftPane.global;
    float xOffsetWithinParent = 0;
    new UIColorPaletteManager(ui, cueMgr, auxMgr, parentContainer.getContentWidth(), xOffsetWithinParent)
        .addToContainer(parentContainer, 3);
  }

  public static void addToRightPerformancePane(LXStudio.UI ui, ColorPaletteManager cueMgr, ColorPaletteManager auxMgr) {
    LXStudio.UI.MainContext mainContext = getLXUIMainContext(ui);
    UI2dContainer parentContainer = mainContext.rightPerformance;
    float xOffsetWithinParent = 14;
    new UIColorPaletteManager(ui, cueMgr, auxMgr, parentContainer.getContentWidth() - 28, xOffsetWithinParent)
        .addToContainer(parentContainer, parentContainer.getChildren().size()-1);
  }

  private static LXStudio.UI.MainContext getLXUIMainContext(LXStudio.UI ui) {
    try {
      // use reflection to access the private field `mainContext` in `LXStudio.UI`
      Field privateField = LXStudio.UI.class.getDeclaredField("mainContext");
      privateField.setAccessible(true);
      return (LXStudio.UI.MainContext)privateField.get(ui);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private class UISingleColorDisplay extends UI2dComponent {
    private int color = LXColor.BLACK;

    private UISingleColorDisplay(ColorParameter colorParameter, float dimension) {
      this(colorParameter, dimension, dimension);
    }
    private UISingleColorDisplay(ColorParameter colorParameter, float w, float h) {
      super(0, 0, w, h);
      this.addListener(colorParameter, (p) -> {
        this.color = ((ColorParameter)p).getColor();
        this.redraw();
      });
    }

    public void onDraw(UI ui, VGraphics vg) {
      vg.fillColor(this.color);
      vg.rect(0, 0, this.getWidth(), this.getHeight());
      vg.fill();
    }
  }

  private class UIHueDisplay extends UIGradientDisplay {
    private double hueOffset = 0.0;

    private UIHueDisplay(BoundedParameter hueOffset, float gradientWidth) {
      super(gradientWidth, GRADIENT_HEIGHT);
      this.addListener(hueOffset, (p) -> {
        this.hueOffset = (double)hueOffset.getValuef();
        this.redraw();
      });
    }

    public void onDraw(UI ui, VGraphics vg) {
      int numStops = 12;
      float stopWidth = this.width / (float)numStops;

      for(int i = 0; i < numStops; ++i) {
        float x0 = stopWidth * (float)i;
        float x1 = LXUtils.clampf(x0 + stopWidth + 0.5F, 0.0F, this.width);
        vg.beginPath();
        vg.fillLinearGradient(x0, 0.0F, x1, 0.0F, LXColor.hsb(this.hueOffset + (double)(i * 360 / numStops), 100.0, 100.0), LXColor.hsb(this.hueOffset + (double)((i + 1) * 360 / numStops), 100.0, 100.0));
        vg.rect(x0, 0.0F, x1 - x0, this.height);
        vg.fill();
      }
    }
  }

  private class UISaturationDisplay extends UIGradientDisplay {
    private UISaturationDisplay(float gradientWidth) {
      super(gradientWidth, GRADIENT_HEIGHT);
    }

    public void onDraw(UI ui, VGraphics vg) {
      vg.beginPath();
      vg.fillLinearGradient(0.0F, 0.0F, this.width, 0.0F, LXColor.hsb(0.0F, 0.0F, 50.0F), LXColor.hsb(0.0F, 100.0F, 100.0F));
      vg.rect(0.0F, 0.0F, this.width, this.height);
      vg.fill();
    }
  }

  private class UIBrightnessDisplay extends UIGradientDisplay {
    private UIBrightnessDisplay(float gradientWidth) {
      super(gradientWidth, GRADIENT_HEIGHT);
    }

    public void onDraw(UI ui, VGraphics vg) {
      vg.beginPath();
      vg.fillLinearGradient(0.0F, 0.0F, this.width, 0.0F, -16777216, -1);
      vg.rect(0.0F, 0.0F, this.width, this.height);
      vg.fill();
    }
  }

  private class UIGradientDisplay extends UI2dComponent {
    protected UIGradientDisplay(float gradientWidth, float gradientHeight) {
      super(0.0F, 0.0F, gradientWidth, gradientHeight);
    }
  }
}
