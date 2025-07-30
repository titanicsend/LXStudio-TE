package titanicsend.resolume.modulator;

import heronarts.glx.ui.component.UISlider;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import titanicsend.resolume.parameter.ResolumeCompoundParameter;
import titanicsend.resolume.ResolumeVariable;

@LXCategory("Resolume")
@LXModulator.Global("Resolume Brightness")
@LXModulator.Device("Resolume Brightness")
public class ResolumeBrightnessModulator extends LXModulator implements UIModulatorControls<ResolumeBrightnessModulator> {

  public final ResolumeCompoundParameter brightness;

  public ResolumeBrightnessModulator(LX lx) {
    super("Resolume Brightness");
    this.brightness = new ResolumeCompoundParameter(lx, ResolumeVariable.BRIGHTNESS);

    addParameter("brightness", this.brightness);
  }

  @Override
  protected double computeValue(double deltaMs) {
    return 0;
  }

  @Override
  public void buildModulatorControls(LXStudio.UI ui, UIModulator uiModulator, ResolumeBrightnessModulator modulator) {
    uiModulator.setLayout(UIModulator.Layout.VERTICAL, 4);
    uiModulator.addChildren(
      new UISlider(UISlider.Direction.HORIZONTAL, 100, 20, this.brightness)
    );
  }
}