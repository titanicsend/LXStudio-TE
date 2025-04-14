package titanicsend.audio;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.glx.ui.component.UIDoubleBox;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.component.UIMeter;
import heronarts.glx.ui.vg.VGraphics;

public class UIAudioStems extends UICollapsibleSection {

  private static final float VERTICAL_SPACING = 4;
  private static final float BREAK_INSET = 2;

  public UIAudioStems(UI ui, AudioStems audioStems, float w) {
    super(ui, 0, 0, w, 0);
    this.setTitle("AUDIO STEMS");
    this.setLayout(Layout.VERTICAL, VERTICAL_SPACING);
    this.setPadding(2, 0);

    for (AudioStems.Stem stem : audioStems.stems) {
      newRow(ui, stem).addToContainer(this);
    }

    addChildren(
        new UI2dComponent(BREAK_INSET, 0, getWidth() - (2 * BREAK_INSET), 1) {}.setBackgroundColor(
                ui.theme.paneInsetColor)
            .setMargin(2, 0),
        UI2dContainer.newHorizontalContainer(
            16,
            4,
            new UILabel.Control(ui, 0, 0, 34, 16, audioStems.gain.getLabel()),
            new UIDoubleBox(26, 0, 50, 16, audioStems.gain)));
  }

  private UI2dContainer newRow(UI ui, AudioStems.Stem stem) {
    return UI2dContainer.newHorizontalContainer(
        16,
        4,
        new UILabel(0, 0, 50, 12, stem.label)
            .setFont(ui.theme.getControlFont())
            .setTextAlignment(VGraphics.Align.LEFT, VGraphics.Align.MIDDLE),
        new UIMeter(ui, stem.parameter, UIMeter.Axis.HORIZONTAL, 8, 1, 50, 14));
  }
}
