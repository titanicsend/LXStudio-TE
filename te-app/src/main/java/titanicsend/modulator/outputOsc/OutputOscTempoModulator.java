package titanicsend.modulator.outputOsc;

import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LXCategory;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import heronarts.lx.utils.LXUtils;

@LXModulator.Global("Output OSC Tempo")
@LXModulator.Device("Output OSC Tempo")
@LXCategory("Output")
public class OutputOscTempoModulator extends LXModulator
    implements LXOscComponent, UIModulatorControls<OutputOscTempoModulator> {

  public final StringParameter path = new StringParameter("Path", "/te/tempo");

  public OutputOscTempoModulator() {
    this("Output OSC Tempo");
  }

  public OutputOscTempoModulator(String label) {
    super(label);
    addParameter("path", this.path);
  }

  private boolean canSend() {
    // Copied from LXComponent: These checks are necessary for bootstrapping, before the OSC engine
    // is spun up
    return (this.lx != null)
        && (this.lx.engine != null)
        && (this.lx.engine.osc != null)
        && (this.lx.engine.osc.transmitActive.isOn() && (this.lx.engine.output.enabled.isOn()));
  }

  @Override
  protected double computeValue(double deltaMs) {
    String path = this.path.getString();
    float value = this.lx.engine.tempo.bpm.getValuef();
    if (!LXUtils.isEmpty(path) && canSend()) {
      this.lx.engine.osc.sendMessage(path, value);
    }
    return 0;
  }

  @Override
  public void buildModulatorControls(
      LXStudio.UI ui, UIModulator uiModulator, OutputOscTempoModulator modulator) {
    uiModulator.setContentHeight(UIKnob.HEIGHT + 4);
    uiModulator.setLayout(UI2dContainer.Layout.HORIZONTAL);
    uiModulator.setChildSpacing(2);
    uiModulator.addChildren(
        newTextBox(this.path).setWidth(150).setTextAlignment(VGraphics.Align.LEFT),
        newDoubleBox(this.lx.engine.tempo.bpm)
            .setEditable(false)
            .setTextAlignment(VGraphics.Align.LEFT)
            .setWidth(40));
  }

  @Override
  public void disposeModulatorControls(
      LXStudio.UI ui, UIModulator uiModulator, OutputOscTempoModulator modulator) {
    UIModulatorControls.super.disposeModulatorControls(ui, uiModulator, modulator);
  }
}
