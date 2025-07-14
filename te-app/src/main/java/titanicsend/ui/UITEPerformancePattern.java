package titanicsend.ui;

import heronarts.glx.ui.UI2dContainer.Layout;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIDevice;
import heronarts.lx.studio.ui.device.UIDeviceControls;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.preset.UIUserPresetCollection;

/** Device UI for TEPerformancePattern */
public class UITEPerformancePattern implements UIDeviceControls<TEPerformancePattern> {

  @Override
  public void buildDeviceControls(LXStudio.UI ui, UIDevice uiDevice, TEPerformancePattern device) {
    uiDevice.setLayout(Layout.HORIZONTAL, 2);

    uiDevice.addChildren(
        // Remote controls, MFT-layout
        new UIMFTControls(ui, device, uiDevice.getContentHeight()),

        // User Presets list
        new UIUserPresetCollection(ui, device, uiDevice.getContentHeight()));
  }
}
