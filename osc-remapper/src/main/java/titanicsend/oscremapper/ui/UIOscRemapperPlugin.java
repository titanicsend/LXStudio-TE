package titanicsend.oscremapper.ui;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UICollapsibleSection;
import titanicsend.oscremapper.OscRemapperPlugin;

public class UIOscRemapperPlugin extends UICollapsibleSection {

  private static final float VERTICAL_SPACING = 4;

  public UIOscRemapperPlugin(UI ui, OscRemapperPlugin plugin, float w) {
    super(ui, 0, 0, w, 0);
    this.setTitle("OSC REMAPPER");
    this.setLayout(Layout.VERTICAL, VERTICAL_SPACING);
    this.setPadding(2, 0);

    addChildren(
        new UIButton(getContentWidth(), 16, plugin.setUpNow).setBorderRounding(4),
        new UIButton(getContentWidth(), 16, plugin.refreshConfig).setBorderRounding(4),

        // OSC Message Capture section
        new UICollapsibleSection(ui, 0, 0, getContentWidth(), 0) {
          {
            setTitle("OSC CAPTURE");
            setLayout(Layout.VERTICAL, 2);
            setPadding(4, 0);

            addChildren(
                new UIButton(getContentWidth(), 16, plugin.oscCaptureEnabled).setBorderRounding(4));
          }
        },

        // Logging Controls section
        new UICollapsibleSection(ui, 0, 0, getContentWidth(), 0) {
          {
            setTitle("LOGGING");
            setLayout(Layout.VERTICAL, 2);
            setPadding(4, 0);

            addChildren(
                new UIButton(getContentWidth(), 16, plugin.loggingEnabled).setBorderRounding(4));
          }
        });
  }
}
