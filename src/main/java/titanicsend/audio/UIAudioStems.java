package titanicsend.audio;

import heronarts.glx.ui.UI;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UICollapsibleSection;
import heronarts.glx.ui.component.UIDoubleBox;
import heronarts.glx.ui.component.UILabel;
import heronarts.glx.ui.component.UIMeter;
import heronarts.lx.parameter.LXNormalizedParameter;

public class UIAudioStems extends UICollapsibleSection {

  private static final float VERTICAL_SPACING = 6;

  public UIAudioStems(UI ui, AudioStems audioStems, float w) {
    super(ui, 0, 0, w, 0);
    this.setTitle("AUDIO STEMS");
    this.setLayout(Layout.VERTICAL, VERTICAL_SPACING);
    this.setPadding(2, 0);

    UI2dContainer meters = UI2dContainer.newHorizontalContainer(50, 4);
    addMeter(ui, meters, audioStems.bass);
    addMeter(ui, meters, audioStems.drums);
    addMeter(ui, meters, audioStems.vocals);
    addMeter(ui, meters, audioStems.other);
    meters.setX(10);
    meters.addToContainer(this);

    UI2dContainer.newHorizontalContainer(16, 10,
      new UILabel.Control(ui, 0, 0, 24, 16, audioStems.gain.getLabel()),
      new UIDoubleBox(26, 0, 54, 16, audioStems.gain)
    ).addToContainer(this);
  }

  private void addMeter(UI ui, UI2dContainer container, LXNormalizedParameter parameter) {
    UI2dContainer.newVerticalContainer(35, 0,
      new UIMeter(ui, parameter, 8, 0, 12, 36),
      new UILabel(0, 0, 35, 12, parameter.getLabel())
      )
    .addToContainer(container);
  }
}
