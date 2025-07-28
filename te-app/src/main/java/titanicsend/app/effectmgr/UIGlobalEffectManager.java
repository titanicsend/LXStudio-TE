package titanicsend.app.effectmgr;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIControls;

public class UIGlobalEffectManager extends UICollapsibleSection implements UIControls {

  private final GlobalEffectManager effectManager;

  public UIGlobalEffectManager(UI ui, GlobalEffectManager effectManager, float w) {
    super(ui, w, 60);
    this.effectManager = effectManager;
  }

  public static void addToPane(
      LXStudio.UI ui, UI2dContainer pane, GlobalEffectManager effectManager, int index) {
    new UIGlobalEffectManager(ui, effectManager, pane.getContentWidth())
        .addToContainer(pane, index);
  }
}
