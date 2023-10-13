package titanicsend.ui.modulator;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIDropMenu;
import heronarts.glx.ui.component.UIIntegerBox;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.vg.VGraphics.Align;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import titanicsend.modulator.dmx.DmxColorModulator;

public class UIDmxColorModulator implements UIModulatorControls<DmxColorModulator> {

  private static final int HEIGHT = 54;
  private static final int LABEL_WIDTH = 56;
  private static final int PARAM_WIDTH = 42;

  @Override
  public void buildModulatorControls(UI ui, UIModulator uiModulator, DmxColorModulator modulator) {
    uiModulator.setContentHeight(HEIGHT);

    UI2dContainer.newVerticalContainer(100, 2)
    .addChildren(
        row("Universe", new UIIntegerBox(PARAM_WIDTH, 16, modulator.universe)),
        row("Channel", new UIIntegerBox(PARAM_WIDTH, 16, modulator.channel)),
        row("Byte Order", new UIDropMenu(PARAM_WIDTH, modulator.byteOrder))
        )
    .addToContainer(uiModulator);

    UI2dContainer.newVerticalContainer(70, 2)
    .addChildren(
        new UILabel(70, 16, "Palette Color").setTextAlignment(Align.LEFT, Align.MIDDLE),
        new UIDropMenu(56, 16, modulator.colorPosition).setX(6)
        )
    .setX(128)
    .addToContainer(uiModulator);
  }

  private UI2dContainer row(String label, UI2dComponent component) {
    return row(label, LABEL_WIDTH, component);
  }

  private UI2dContainer row(String label, int labelWidth, UI2dComponent component) {
    return UI2dContainer.newHorizontalContainer(16, 4,
        new UILabel(labelWidth, 12, label),
        component);
  }
}
