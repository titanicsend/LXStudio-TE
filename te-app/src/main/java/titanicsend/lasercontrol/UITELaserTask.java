package titanicsend.lasercontrol;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.device.UIControls;
import titanicsend.ui.UITEColorControl;

public class UITELaserTask extends UI2dContainer implements UIControls {

  private static final int CHECKBOX_LABEL_WIDTH = 60;

  public UITELaserTask(LXStudio.UI ui, float w) {
    super(0, 0, w, 0);
    setLayout(Layout.HORIZONTAL, 4);

    TELaserTask laserTask = TELaserTask.get();

    UI2dContainer checkboxes =
        newVerticalContainer(
            CHECKBOX_LABEL_WIDTH + 14,
            0,
            newCheckbox(ui, laserTask.sendBrightness, "Brightness"),
            newCheckbox(ui, laserTask.sendColor, "Color"),
            newCheckbox(ui, laserTask.sendTempo, "BPM"));
    float h = checkboxes.getHeight();

    addChildren(
        newVerticalContainer(
            47,
            0,
            new UILabel(0, (h - 20) / 2, 45, 12, "Laser")
                .setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE)
                .setFont(ui.theme.getLabelFont())
                .setMargin(7, 0, 0, 0),
            new UILabel(0, (h - 20) / 2, 45, 12, "Sync")
                .setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE)
                .setFont(ui.theme.getLabelFont())),
        checkboxes,
        new UITEColorControl(0, (h - UIKnob.HEIGHT) / 2, laserTask.color));

    setContentHeight(h);
  }

  private UI2dContainer newCheckbox(LXStudio.UI ui, BooleanParameter p, String label) {
    return newHorizontalContainer(
        14,
        4,
        new UIButton.Toggle(p),
        new UILabel(CHECKBOX_LABEL_WIDTH, label)
            .setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE)
            .setFont(ui.theme.getControlFont()));
  }

  @Override
  protected void drawBorder(UI ui, VGraphics vg) {
    super.drawBorder(ui, vg);
    vg.beginPath();
    vg.strokeColor(ui.theme.controlTextColor);
    vg.line(48, 0, 30, 20);
    vg.line(30, 20, 48, 40);
    vg.stroke();
  }
}
