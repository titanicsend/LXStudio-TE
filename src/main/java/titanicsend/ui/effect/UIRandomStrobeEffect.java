package titanicsend.ui.effect;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.effect.RandomStrobeEffect;

public class UIRandomStrobeEffect implements UIDeviceControls<RandomStrobeEffect> {

  private UI2dComponent speed, minFreq, maxFreq, minFreqLabel, maxFreqLabel, tempoDivision, tempoPhaseOffset;

  @Override
  public void buildDeviceControls(UI ui, UIDevice uiDevice, RandomStrobeEffect strobe) {
    uiDevice.setLayout(UI2dContainer.Layout.HORIZONTAL);
    uiDevice.setChildSpacing(4);

    addColumn(uiDevice,
      "Rate",
      newButton(strobe.tempoSync),
      this.speed = newKnob(strobe.speed),
      this.minFreq = newDoubleBox(strobe.minFrequency),
      this.minFreqLabel = controlLabel(ui, "Min"),
      this.maxFreq = newDoubleBox(strobe.maxFrequency),
      this.maxFreqLabel = controlLabel(ui, "Max"),
      this.tempoDivision = newEnumBox(strobe.tempoDivision),
      this.tempoPhaseOffset = newKnob(strobe.tempoPhaseOffset)
    ).setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice,
      "Shape",
      newDropMenu(strobe.waveshape),
      newKnob(strobe.depth),
      newKnob(strobe.bias)
    ).setChildSpacing(6);

    uiDevice.addListener(strobe.tempoSync, (p) -> { updateTempoSync(strobe); });
    updateTempoSync(strobe);
  }

  private void updateTempoSync(RandomStrobeEffect strobe) {
    boolean isSync = strobe.tempoSync.isOn();
    this.speed.setVisible(!isSync);
    this.minFreq.setVisible(!isSync);
    this.maxFreq.setVisible(!isSync);
    this.minFreqLabel.setVisible(!isSync);
    this.maxFreqLabel.setVisible(!isSync);
    this.tempoDivision.setVisible(isSync);
    this.tempoPhaseOffset.setVisible(isSync);
  }

}
