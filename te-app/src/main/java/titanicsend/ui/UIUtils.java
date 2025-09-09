package titanicsend.ui;

import heronarts.glx.ui.UI2dContainer;
import heronarts.lx.LXDeviceComponent;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIDevice;
import titanicsend.preset.UIUserPresetCollection;

public class UIUtils {

  /** Build the normal device controls for TE */
  public static void buildTEDeviceControls(UI ui, UIDevice uiDevice, LXDeviceComponent device) {
    uiDevice.setLayout(UI2dContainer.Layout.HORIZONTAL, 2);

    uiDevice.addChildren(
        // Remote controls, MFT-layout
        new UIMFTControls(ui, device, uiDevice.getContentHeight()),

        // User Presets list
        new UIUserPresetCollection(ui, device, uiDevice.getContentHeight()));
  }
}
