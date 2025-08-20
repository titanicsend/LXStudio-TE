package titanicsend.ui.effect;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.effect.RandomStrobeEffect;

public class UIRandomStrobeEffect implements UIDeviceControls<RandomStrobeEffect> {

  private UI2dComponent runBeats,
      runBeatsLabel,
      runTime,
      runTimeLabel,
      speed,
      minFreq,
      maxFreq,
      minFreqLabel,
      maxFreqLabel,
      tempoDivision,
      tempoPhaseOffset;

  @Override
  public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, RandomStrobeEffect strobe) {
    uiDevice.setLayout(UI2dContainer.Layout.HORIZONTAL);
    uiDevice.setChildSpacing(4);

    addColumn(
            uiDevice,
            "Duration",
            new UIButton(50, strobe.launch)
                .setHeight(50)
                .setTextAlignment(VGraphics.Align.CENTER, VGraphics.Align.MIDDLE),
            newButton(strobe.tempoSync),
            this.runBeats = newDropMenu(strobe.runBeats),
            this.runBeatsLabel = controlLabel(ui, "Run Beats"),
            this.runTime = newDoubleBox(strobe.runTimeMs),
            this.runTimeLabel = controlLabel(ui, "Run Time"))
        .setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    addColumn(
            uiDevice,
            "Rate",
            // Non-sync (milliseconds) mode
            this.speed = newKnob(strobe.speed),
            this.minFreq = newDoubleBox(strobe.minFrequency),
            this.minFreqLabel = controlLabel(ui, "Min"),
            this.maxFreq = newDoubleBox(strobe.maxFrequency),
            this.maxFreqLabel = controlLabel(ui, "Max"),
            // Sync (tempo) mode
            this.tempoDivision = newDropMenu(strobe.tempoDivision),
            this.tempoPhaseOffset = newKnob(strobe.tempoPhaseOffset))
        .setChildSpacing(6);

    addVerticalBreak(ui, uiDevice);

    addColumn(
            uiDevice,
            "Shape",
            newDropMenu(strobe.waveshape),
            newKnob(strobe.depth),
            newKnob(strobe.bias))
        .setChildSpacing(6);

    // Toggle UI element visibility based on Sync parameter
    uiDevice.addListener(
        strobe.tempoSync,
        (p) -> {
          updateTempoSync(strobe);
        },
        true);
  }

  private void updateTempoSync(RandomStrobeEffect strobe) {
    boolean isSync = strobe.tempoSync.isOn();
    this.runBeats.setVisible(isSync);
    this.runBeatsLabel.setVisible(isSync);
    this.runTime.setVisible(!isSync);
    this.runTimeLabel.setVisible(!isSync);
    this.speed.setVisible(!isSync);
    this.minFreq.setVisible(!isSync);
    this.maxFreq.setVisible(!isSync);
    this.minFreqLabel.setVisible(!isSync);
    this.maxFreqLabel.setVisible(!isSync);
    this.tempoDivision.setVisible(isSync);
    this.tempoPhaseOffset.setVisible(isSync);
  }
}
