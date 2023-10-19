package titanicsend.ui.modulator;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIIntegerBox;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import titanicsend.modulator.dmx.DmxGridModulator;

public class UIDmxGridModulator implements UIModulatorControls<DmxGridModulator> {

  private static final int HEIGHT = 36;
  private static final int LABEL_WIDTH = 48;
  private static final int PARAM_WIDTH = 42;

  @Override
  public void buildModulatorControls(UI ui, UIModulator uiModulator, DmxGridModulator modulator) {
    uiModulator.setContentHeight(HEIGHT);

    UI2dContainer.newVerticalContainer(100, 2)
    .addChildren(
      row(ui, "Universe", modulator.universe),
      row(ui, "Channel", modulator.channel)
      )
    .addToContainer(uiModulator);

    UI2dContainer.newVerticalContainer(100, 2)
    .addChildren(
      row(ui, "Rows", modulator.rows),
      row(ui, "Columns", modulator.columns)
      )
    .setX(110)
    .addToContainer(uiModulator);
  }

  private UI2dContainer row(UI ui, String label, DiscreteParameter parameter) {
    return row(ui, label, LABEL_WIDTH, new UIIntegerBox(PARAM_WIDTH, 16, parameter));
  }
  
  private UI2dContainer row(UI ui, String label, int labelWidth, UI2dComponent component) {
    return UI2dContainer.newHorizontalContainer(16, 4,
      new UILabel(labelWidth, 16, label)
        .setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE)
        .setFont(ui.theme.getControlFont()),
      component
      );
  }
}
