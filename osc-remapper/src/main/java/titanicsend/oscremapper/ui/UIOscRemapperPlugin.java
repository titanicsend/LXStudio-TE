package titanicsend.oscremapper.ui;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UICollapsibleSection;
import titanicsend.oscremapper.OscRemapperPlugin;

public class UIOscRemapperPlugin extends UICollapsibleSection {

  private static final float VERTICAL_SPACING = 4;

  public UIOscRemapperPlugin(UI ui, OscRemapperPlugin plugin, float w) {
    super(ui, 0, 0, w, 0);
    this.setTitle("OSC OUTPUT CONFIGS");
    this.setLayout(Layout.VERTICAL, VERTICAL_SPACING);
    this.setPadding(2, 0);

    addChildren(
        new UIButton(getContentWidth(), 16, plugin.setUpOscOutputs).setBorderRounding(4),
        new UIButton(getContentWidth(), 16, plugin.reloadYamlConfig).setBorderRounding(4),

        // OSC Message Capture section
        new UICollapsibleSection(ui, 0, 0, getContentWidth(), 0) {
          {
            setTitle("OSC REMAPPER");
            setLayout(Layout.VERTICAL, 2);
            setPadding(4, 0);

            addChildren(
                new UIButton(getContentWidth(), 16, plugin.oscRemappingEnabled)
                    .setBorderRounding(4));
            addChildren(
                new UIButton(getContentWidth(), 16, plugin.remapperLoggingEnabled)
                    .setBorderRounding(4));
          }
        });
  }
}
