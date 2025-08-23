package titanicsend.ui.effect;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.effect.ShakeEffect;
import titanicsend.preset.UIUserPresetCollection;

public class UIShakeEffect implements UIDeviceControls<ShakeEffect> {

  private UI2dComponent speed, depth, tempoSync, tempoDivision;

  @Override
  public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, ShakeEffect shake) {
    uiDevice.setLayout(UI2dContainer.Layout.HORIZONTAL);
    uiDevice.setChildSpacing(4);

    addColumn(
            uiDevice,
            new UIButton(50, shake.trigger)
                .setHeight(50)
                .setTextAlignment(VGraphics.Align.CENTER, VGraphics.Align.MIDDLE),
            this.tempoSync = newButton(shake.tempoSync),
            this.tempoDivision = newKnob(shake.tempoDivision))
        .setChildSpacing(10);

    addVerticalBreak(ui, uiDevice);

    addColumn(uiDevice, this.speed = newKnob(shake.speed), this.depth = newKnob(shake.depth))
        .setChildSpacing(10);

    addVerticalBreak(ui, uiDevice);

    // User Presets list
    new UIUserPresetCollection(ui, shake, uiDevice.getContentHeight()).addToContainer(uiDevice);
  }
}
