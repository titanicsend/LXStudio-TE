package titanicsend.app.director;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.component.UISlider;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.studio.LXStudio.UI;
import heronarts.lx.studio.ui.device.UIControls;
import titanicsend.lasercontrol.UITELaserTask;

public class UIDirector extends UICollapsibleSection implements UIControls {

  private final Director director;

  private static final float VERTICAL_SPACING = 2;
  private static final float FADER_WIDTH = 20;
  private static final float FADER_HEIGHT = 45;
  private static final int COLUMNS = 8;

  private final float columnWidth;

  private final LXParameterListener laserBrightnessListener;

  public UIDirector(UI ui, Director director, float w) {
    super(ui, 0, 0, w, 30);
    this.setTitle("DIRECTOR");
    this.setLayout(Layout.VERTICAL, VERTICAL_SPACING);
    this.director = director;

    this.columnWidth = this.getContentWidth() / COLUMNS;

    UI2dContainer row = addRow();
    int column = 0;
    boolean labelUp = true;

    for (Filter filter : director.filters) {
      addFader(ui, row, filter.fader, column, labelUp);
      labelUp = !labelUp;
      column++;
      if (column == COLUMNS) {
        this.addChildren(row);
        row = addRow();
        column = 0;
        labelUp = true;
      }
    }

    // Lasers fader
    UISlider laserFader = addFader(ui, row, director.laserTask.brightness, column++, labelUp);
    labelUp = !labelUp;

    // Set laser fader background color to gray when not sending, like channel faders
    director.laserTask.brightness.addListener(
        this.laserBrightnessListener =
            (p) -> {
              if (director.laserTask.sendBrightness.isOn()) {
                laserFader.resetFillColor();
                laserFader.redraw(); // TODO: why is this not refreshing the slider background?
              } else {
                laserFader.setFillColor(ui.theme.controlDisabledValueColor);
                laserFader.redraw();
              }
            },
        true);

    // Master fader
    addFader(ui, row, director.master, column++, labelUp);
    this.addChildren(row);

    addChildren(
        // Horizontal break
        new UI2dComponent(2, 0, getContentWidth() - 4, 1) {}.setBackgroundColor(
                ui.theme.paneInsetColor)
            .setMargin(0, 0, 2, 0),

        // Laser sync options
        new UITELaserTask(ui, getContentWidth()));
  }

  private UI2dContainer addRow() {
    return new UI2dContainer(0, 0, this.getContentWidth(), FADER_HEIGHT + 30)
        .setLayout(UI2dContainer.Layout.NONE);
  }

  private UISlider addFader(
      UI ui,
      UI2dContainer uiDevice,
      LXListenableNormalizedParameter parameter,
      int column,
      boolean labelUp) {

    UISlider fader =
        (UISlider)
            new UISlider(
                    UISlider.Direction.VERTICAL,
                    (column * this.columnWidth) + ((this.columnWidth - FADER_WIDTH) / 2),
                    0,
                    FADER_WIDTH,
                    FADER_HEIGHT,
                    parameter)
                .setShowLabel(false)
                .setWidth(20)
                .addToContainer(uiDevice);

    new UILabel(this.columnWidth * 2, parameter.getLabel())
        .setFont(ui.theme.getControlFont())
        .setTextAlignment(VGraphics.Align.CENTER, VGraphics.Align.TOP)
        .setX((column - 0.5f) * this.columnWidth)
        .setY(FADER_HEIGHT + (labelUp ? 4 : 16))
        .addToContainer(uiDevice);

    return fader;
  }

  @Override
  public void dispose() {
    this.director.laserTask.brightness.removeListener(this.laserBrightnessListener);
    super.dispose();
  }
}
