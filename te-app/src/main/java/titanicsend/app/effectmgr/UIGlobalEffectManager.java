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

  public static void addToLeftGlobalPane(
      LXStudio.UI ui, GlobalEffectManager effectManager, int index) {
    UI2dContainer parentContainer = ui.leftPane.global;
    new UIGlobalEffectManager(ui, effectManager, parentContainer.getContentWidth())
        .addToContainer(parentContainer, index);
  }

  public static void addToRightPerformancePane(
      LXStudio.UI ui, GlobalEffectManager effectManager, int index) {
    UI2dContainer parentContainer = ui.rightPerformance.tools;
    new UIGlobalEffectManager(ui, effectManager, parentContainer.getContentWidth())
        .addToContainer(parentContainer, parentContainer.getChildren().size() - 1);
  }
}
