package titanicsend.app.director;

import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.glx.ui.component.UISlider;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIControls;
import titanicsend.app.director.Director.Filter;

public class UIDirector extends UICollapsibleSection implements UIControls {

  private static final float VERTICAL_SPACING = 4;
  private static final float FADER_WIDTH = 20;
  private static final float FADER_HEIGHT = 45;
  private static final int COLUMNS = 4;

  public UIDirector(UI ui, Director director, float w) {
    super(ui, 0, 0, w, 30);
    this.setTitle("DIRECTOR");
    this.setLayout(Layout.VERTICAL, VERTICAL_SPACING);

    final float columnWidth = this.getContentWidth() / COLUMNS;

    UI2dContainer row = addRow();
    int column = 0;

    for (Filter filter : director.filters) {
      addFader(ui, row, columnWidth, filter.fader);
      column++;
      if (column == COLUMNS) {
        this.addChildren(row);
        row = addRow();
        column = 0;
      }
    }

    addFader(ui, row, columnWidth, director.main);
    this.addChildren(row);
  }

  private UI2dContainer addRow() {
    return UI2dContainer.newHorizontalContainer(FADER_HEIGHT + 15, 0);
  }

  private void addFader(
    UI ui,
    UI2dContainer uiDevice,
    float columnWidth,
    LXListenableNormalizedParameter parameter) {

    addColumn(uiDevice, columnWidth,
      new UISlider(
        UISlider.Direction.VERTICAL,
        (columnWidth - FADER_WIDTH) / 2,
        0,
        20,
        FADER_HEIGHT,
        parameter)
      .setShowLabel(false)
      .setWidth(20),
      controlLabel(ui, parameter.getLabel(), columnWidth)
      );
  }
}