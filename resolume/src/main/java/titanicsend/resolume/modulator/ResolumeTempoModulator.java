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
@LXModulator.Global("Resolume Tempo")
@LXModulator.Device("Resolume Tempo")
public class ResolumeTempoModulator extends LXModulator implements UIModulatorControls<ResolumeTempoModulator> {

  public final ResolumeCompoundParameter tempo;

  public ResolumeTempoModulator(LX lx) {
    super("Resolume Tempo");
    this.tempo = new ResolumeCompoundParameter(lx, ResolumeVariable.TEMPO_BPM);

    addParameter("tempo", this.tempo);
  }

  @Override
  protected double computeValue(double deltaMs) {
    return 0;
  }

  @Override
  public void buildModulatorControls(LXStudio.UI ui, UIModulator uiModulator, ResolumeTempoModulator modulator) {
    uiModulator.setLayout(UIModulator.Layout.VERTICAL, 4);
    uiModulator.addChildren(
      new UISlider(UISlider.Direction.HORIZONTAL, 100, 20, this.tempo)
    );
  }
}