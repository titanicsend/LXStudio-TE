package titanicsend.ui.modulator;

import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIIndicator;
import heronarts.glx.ui.component.UIIntegerBox;
import heronarts.glx.ui.component.UIMeter;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import titanicsend.modulator.dmx.DmxDualRangeModulator;

public class UIDmxDualRangeModulator implements UIModulatorControls<DmxDualRangeModulator> {

  private static final int METER_HEIGHT = 32;
  private static final int HEIGHT = METER_HEIGHT + 30;

  @Override
  public void buildModulatorControls(
      UI ui, UIModulator uiModulator, DmxDualRangeModulator modulator) {
    uiModulator.setContentHeight(HEIGHT);

    UI2dContainer inputs = UI2dContainer.newHorizontalContainer(HEIGHT, 4);
    addColumn(
            inputs,
            48,
            new UIIntegerBox(48, 16, modulator.universe),
            controlLabel(ui, "Universe", 48))
        .setChildSpacing(6);
    addColumn(
            inputs,
            48,
            new UIIntegerBox(48, 16, modulator.channel),
            controlLabel(ui, "Channel", 48))
        .setChildSpacing(6);

    UI2dContainer outputs = UI2dContainer.newHorizontalContainer(HEIGHT, 4);
    final UIMeter meter1 =
        (UIMeter) UIMeter.newVerticalMeter(ui, modulator.output1, 12, METER_HEIGHT).setX(8);
    addColumn(
            outputs,
            24,
            controlLabel(ui, "Zero", 24),
            new UIIndicator(ui, 12, 12, modulator.isZero).setTriggerable(true).setX(8))
        .setChildSpacing(4);
    addColumn(
            outputs,
            24,
            controlLabel(ui, "Out1", 24),
            new UIIndicator(ui, 12, 12, modulator.range1active).setTriggerable(true).setX(8),
            meter1)
        .setChildSpacing(4);
    addColumn(
            outputs,
            24,
            controlLabel(ui, "Out2", 26),
            new UIIndicator(ui, 12, 12, modulator.range2active).setTriggerable(true).setX(8),
            UIMeter.newVerticalMeter(ui, modulator.output2, 12, METER_HEIGHT).setX(8))
        .setChildSpacing(4);
    outputs.setX(uiModulator.getContentWidth() - outputs.getWidth());

    inputs.setY(2);
    outputs.setY(2);
    uiModulator.addChildren(inputs, outputs);

    // uiModulator.setModulationSourceUI(meter1);
  }
}
