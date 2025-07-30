package titanicsend.resolume.ui;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UICollapsibleSection;
import titanicsend.resolume.ResolumePlugin;

public class UIResolumePlugin extends UICollapsibleSection {

  private static final float VERTICAL_SPACING = 4;

  public UIResolumePlugin(UI ui, ResolumePlugin plugin, float w) {
    super(ui, 0, 0, w, 0);
    this.setTitle("RESOLUME");
    this.setLayout(Layout.VERTICAL, VERTICAL_SPACING);
    this.setPadding(2, 0);

    addChildren(
      new UIButton(getContentWidth(), 16, plugin.setUpNow)
        .setBorderRounding(4)
    );
  }

}