package titanicsend.ui.effect;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.glx.ui.component.UIColorPicker;
import heronarts.glx.ui.component.UIDoubleBox;
import heronarts.glx.ui.component.UIDropMenu;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.component.UISlider;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
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
  private final float width;

  public UIColorPaletteManager(LXStudio.UI ui, ColorPaletteManager paletteMgr, float w, float xOffset) {
    super(ui, xOffset, 0, w, 0);
    this.width = w;

    this.setLayout(Layout.VERTICAL, 0);
    this.setPadding(2, 0);
    this.setTitle("PALETTE MANAGER");
//    this.setContentWidth(this.width);

    float SLIDER_SPACING = 4;
    float controlWidth = this.width / 3;
    float sliderWidth = controlWidth - (2*SLIDER_SPACING);

    UI2dContainer container1 = UI2dContainer.newHorizontalContainer(controlWidth, SLIDER_SPACING);
    addColumn(
        container1,
        new UIHueDisplay(paletteMgr.hue, sliderWidth),
        new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, sliderWidth, 12.0F, paletteMgr.hue)
    );
    addColumn(
        container1,
        new UISaturationDisplay(sliderWidth),
        new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, sliderWidth, 12.0F, paletteMgr.saturation)
    );
    addColumn(
        container1,
        new UIBrightnessDisplay(sliderWidth),
        new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, sliderWidth, 12.0F, paletteMgr.brightness)
    );
    container1.addToContainer(this);

    float MAIN_COLOR_SIZE = 28F;
    UI2dContainer container2 = UI2dContainer.newHorizontalContainer(this.width, 2F);
    addColumn(
        container2,
        new UISingleColorDisplay(paletteMgr.color1, MAIN_COLOR_SIZE)
    ).setWidth(MAIN_COLOR_SIZE);
    addColumn(
        container2,
//        new UILabel(56.0F, "Palette Type"),
        newKnob(paletteMgr.paletteType)
    );
    addColumn(
        container2,
        new UISingleColorDisplay(paletteMgr.color2, MAIN_COLOR_SIZE)
    ).setWidth(MAIN_COLOR_SIZE);
    addColumn(
        container2,
        new UISingleColorDisplay(paletteMgr.color3, MAIN_COLOR_SIZE)
    ).setWidth(MAIN_COLOR_SIZE);
    container2.addToContainer(this);

//    UI2dContainer container2 = UI2dContainer.newHorizontalContainer(controlWidth, 6);
//    addColumn(container2, new UILabel(CHAR_WIDTH, "S"), new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, controlWidth - CHAR_WIDTH - 6, 12.0F, paletteMgr.saturation)); //"Saturation"));
//    UI2dContainer container3 = UI2dContainer.newHorizontalContainer(controlWidth, 6);
//    addColumn(container3, new UILabel(CHAR_WIDTH, "B"), new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, controlWidth - CHAR_WIDTH - 6, 12.0F, paletteMgr.brightness)); //Brightness"));
//
//    float gradientWidth = this.width / 2;
//
//    UI2dContainer hueSlider = UI2dContainer.newHorizontalContainer(gradientWidth, 2);
//

//    addColumn(
//        container1,
//
////        new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, gradientWidth, 16.0F, paletteMgr.hue),
////        new UIColorPaletteManager.UISaturationDisplay(gradientWidth).addToContainer(uiContainer),
//        new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, gradientWidth, 16.0F, paletteMgr.saturation),
////        new UIColorPaletteManager.UIBrightnessDisplay(gradientWidth).addToContainer(uiContainer),
//        new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, gradientWidth , 16.0F, paletteMgr.brightness)
//    ).setChildSpacing(6).setWidth(gradientWidth);


//    buildDeviceControls(ui, this, paletteMgr);
  }

  public void buildDeviceControls(LXStudio.UI ui, UI2dContainer uiContainer, ColorPaletteManager effect) {
//    uiContainer.setLayout(UI2dContainer.Layout.HORIZONTAL);
//    uiContainer.setChildSpacing(4);
//    uiContainer.setContentWidth(120F + 56F + 120F);



    addVerticalBreak(ui, uiContainer);

    addColumn(
        uiContainer,
        new UILabel(56.0F, "Position"),
        new UIDropMenu(56.0F, 16, effect.color1Pos),
        new UILabel(56.0F, "Palette Type"),
        newKnob(effect.paletteType),
        newButton(effect.toggleCue)
    ).setChildSpacing(6).setWidth(56F);

    addVerticalBreak(ui, uiContainer);

    addColumn(
        uiContainer,
        row("Act", 20, new UIPalette.Swatch(ui, effect.getActiveSwatch(), 0, 0, 80, UIColorPicker.Corner.TOP_LEFT)),
        row("Cue", 20, new UIPalette.Swatch(ui, effect.getCueSwatch(), 0, 0, 80, UIColorPicker.Corner.TOP_LEFT))
    ).setChildSpacing(6).setWidth(SLIDER_WIDTH+10);

    uiContainer.addListener(
        effect.toggleCue,
        (p) -> {
          effect.swapCueSwatch();
        });
  }

  private UI2dContainer row(String label, UI2dComponent component) {
    return row(label, LABEL_WIDTH, component);
  }

  private UI2dContainer row(String label, int labelWidth, UI2dComponent component) {
    return UI2dContainer.newHorizontalContainer(
        16, 4, new UILabel(labelWidth, 12, label), component);
  }

  public static void addToLeftGlobalPane(LXStudio.UI ui, ColorPaletteManager colorPaletteManager) {
    UI2dContainer parentContainer = ui.leftPane.global;
    float xOffsetWithinParent = 0;
    new UIColorPaletteManager(ui, colorPaletteManager, parentContainer.getContentWidth(), xOffsetWithinParent)
        .addToContainer(parentContainer, 3);
  }

  public static void addToRightPerformancePane(LXStudio.UI ui, ColorPaletteManager colorPaletteManager) {
    LXStudio.UI.MainContext mainContext = getLXUIMainContext();
    UI2dContainer parentContainer = mainContext.rightPerformance;
    float xOffsetWithinParent = 0;
    new UIColorPaletteManager(ui, colorPaletteManager, parentContainer.getContentWidth(), xOffsetWithinParent)
        .addToContainer(parentContainer, 1);
  }

  private static LXStudio.UI.MainContext getLXUIMainContext() {
    try {
      Field privateField = LXStudio.UI.class.getDeclaredField("mainContext");

      // Set the accessibility as true
      privateField.setAccessible(true);

      // Store the value of private field in variable
      LXStudio.UI.MainContext mainContext = (LXStudio.UI.MainContext)privateField.get(ui);

      return mainContext;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private class UISingleColorDisplay extends UI2dComponent {
    private int color = LXColor.BLACK;

    private UISingleColorDisplay(ColorParameter colorParameter, float dimension) {
      super(0, 0, dimension, dimension);
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
