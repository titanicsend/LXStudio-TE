package titanicsend.ui.modulator;

import heronarts.glx.ui.component.UIIntegerBox;
import heronarts.glx.ui.component.UIMeter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import titanicsend.modulator.dmx.Dmx16bitModulator;

public class UIDmx16bitModulator implements UIModulatorControls<Dmx16bitModulator> {

  private static final int HEIGHT = 32;

  public void buildModulatorControls(LXStudio.UI ui, UIModulator uiModulator, final Dmx16bitModulator dmx) {
    uiModulator.setContentHeight(HEIGHT);

    uiModulator.addChildren(
      new UIIntegerBox(0, 2, 48, 16).setParameter(dmx.universe),
      controlLabel(ui, "Universe", 48).setPosition(0, 21),
      new UIIntegerBox(52, 2, 48, 16).setParameter(dmx.channel),
      controlLabel(ui, "Channel", 48).setPosition(52, 21),
      horizontalBreak(ui, 88).setPosition(104, 9),
      UIMeter.newVerticalMeter(ui, dmx, 12, HEIGHT).setX(uiModulator.getContentWidth() - 12)
    );
  }

}
