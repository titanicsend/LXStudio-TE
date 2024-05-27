package titanicsend.ui.effect;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIColorPicker;
import heronarts.glx.ui.component.UIDropMenu;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.component.UISlider;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.color.LXColor;
import heronarts.lx.studio.ui.global.UIPalette;
import titanicsend.effect.LookColorPaletteEffect;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import heronarts.lx.utils.LXUtils;


public class UILookColorPaletteEffect implements UIDeviceControls<LookColorPaletteEffect> {
  private static final int LABEL_WIDTH = 56;
  private static final float SLIDER_WIDTH = 120.0F;

  public UILookColorPaletteEffect() {
  }

  public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, LookColorPaletteEffect effect) {
    uiDevice.setLayout(UI2dContainer.Layout.HORIZONTAL);
    uiDevice.setChildSpacing(4);
    uiDevice.setContentWidth(120F + 56F + 120F);

    addColumn(
        uiDevice,
        new UILookColorPaletteEffect.UIHueDisplay(effect.hue),
        new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, SLIDER_WIDTH, 16.0F, effect.hue),
        new UILookColorPaletteEffect.UISaturationDisplay().addToContainer(uiDevice),
        new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, SLIDER_WIDTH, 16.0F, effect.saturation).addToContainer(uiDevice),
        new UILookColorPaletteEffect.UIBrightnessDisplay().addToContainer(uiDevice),
        new UISlider(UISlider.Direction.HORIZONTAL, 0.0F, 0.0F, SLIDER_WIDTH, 16.0F, effect.brightness).addToContainer(uiDevice)
    ).setChildSpacing(6).setWidth(SLIDER_WIDTH);

    addVerticalBreak(ui, uiDevice);

    addColumn(
        uiDevice,
        new UILabel(56.0F, "Position"),
        new UIDropMenu(56.0F, 16, effect.color1Pos),
        new UILabel(56.0F, "Palette Type"),
        newKnob(effect.paletteType),
        newButton(effect.toggleCue)
    ).setChildSpacing(6).setWidth(56F);

    addVerticalBreak(ui, uiDevice);

    addColumn(
        uiDevice,
        row("Act", 20, new UIPalette.Swatch(ui, effect.getActiveSwatch(), 0, 0, 80, UIColorPicker.Corner.TOP_LEFT)),
        row("Cue", 20, new UIPalette.Swatch(ui, effect.getCueSwatch(), 0, 0, 80, UIColorPicker.Corner.TOP_LEFT))
    ).setChildSpacing(6).setWidth(SLIDER_WIDTH+10);

    uiDevice.addListener(
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

  private class UIHueDisplay extends UILookColorPaletteEffect.UIGradientDisplay {
    private double hueOffset = 0.0;

    private UIHueDisplay() {
      super();
    }

    private UIHueDisplay(BoundedParameter hueOffset) {
      super();
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

  private class UISaturationDisplay extends UILookColorPaletteEffect.UIGradientDisplay {
    private UISaturationDisplay() {
      super();
    }

    public void onDraw(UI ui, VGraphics vg) {
      vg.beginPath();
      vg.fillLinearGradient(0.0F, 0.0F, this.width, 0.0F, LXColor.hsb(0.0F, 0.0F, 50.0F), LXColor.hsb(0.0F, 100.0F, 100.0F));
      vg.rect(0.0F, 0.0F, this.width, this.height);
      vg.fill();
    }
  }

  private class UIBrightnessDisplay extends UILookColorPaletteEffect.UIGradientDisplay {
    private UIBrightnessDisplay() {
      super();
    }

    public void onDraw(UI ui, VGraphics vg) {
      vg.beginPath();
      vg.fillLinearGradient(0.0F, 0.0F, this.width, 0.0F, -16777216, -1);
      vg.rect(0.0F, 0.0F, this.width, this.height);
      vg.fill();
    }
  }

  private class UIGradientDisplay extends UI2dComponent {
    protected UIGradientDisplay() {
      super(0.0F, 0.0F, 120.0F, 8.0F);
    }
  }
}
