package titanicsend.ui.color;

import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIControls;
import titanicsend.color.ColorPaletteManager;

/** Palette Manager collapsible section for a UI Pane. Contains one or two manager UIs. */
public class UIColorPaletteManagerSection extends UICollapsibleSection implements UIControls {
  public static final boolean DISPLAY_TWO_MANAGED_SWATCHES = false;

  public UIColorPaletteManagerSection(
      LXStudio.UI ui,
      ColorPaletteManager paletteManagerA,
      ColorPaletteManager paletteManagerB,
      float w) {
    super(ui, 0, 0, w, 0);

    setTitle("PALETTE MANAGER");
    setLayout(Layout.VERTICAL, 2F);

    // The expanded state will be saved and reloaded with project files
    setExpandedParameter(paletteManagerA.isExpanded);

    new UIColorPaletteManager(ui, paletteManagerA, getContentWidth()).addToContainer(this);

    if (paletteManagerB != null) {
      horizontalBreak(ui, this.width).addToContainer(this);

      new UIColorPaletteManager(ui, paletteManagerB, getContentWidth()).addToContainer(this);
    }
  }

  public static void addToLeftGlobalPane(
      LXStudio.UI ui, ColorPaletteManager cueMgr, ColorPaletteManager auxMgr) {
    UI2dContainer parentContainer = ui.leftPane.global;
    new UIColorPaletteManagerSection(ui, cueMgr, auxMgr, parentContainer.getContentWidth())
        .addBeforeSibling(ui.leftPane.palette);
  }

  public static void addToRightPerformancePane(
      LXStudio.UI ui, ColorPaletteManager cueMgr, ColorPaletteManager auxMgr) {
    UI2dContainer parentContainer = ui.rightPerformance.tools;
    new UIColorPaletteManagerSection(ui, cueMgr, auxMgr, parentContainer.getContentWidth())
        .addToContainer(parentContainer, parentContainer.getChildren().size() - 1);
  }
}
