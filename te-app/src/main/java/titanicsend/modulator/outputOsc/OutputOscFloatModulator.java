package titanicsend.modulator.outputOsc;

import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LXCategory;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import heronarts.lx.utils.LXUtils;

@LXModulator.Global("Output OSC Float")
@LXModulator.Device("Output OSC Float")
@LXCategory("Output")
public class OutputOscFloatModulator extends LXModulator
    implements LXOscComponent, UIModulatorControls<OutputOscFloatModulator> {

  public final StringParameter path = new StringParameter("Path", "/te/oscOutput");

  public final CompoundParameter value =
      new CompoundParameter("Float", 0, 0, 1).setDescription("Output value to send");

  public OutputOscFloatModulator() {
    this("Output OSC Float");
  }

  public OutputOscFloatModulator(String label) {
    super(label);
    addParameter("path", this.path);
    addParameter("value", this.value);
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
    float value = this.value.getValuef();
    if (!LXUtils.isEmpty(path) && canSend()) {
      this.lx.engine.osc.sendMessage(path, value);
    }
    return 0;
  }

  @Override
  public void buildModulatorControls(
      LXStudio.UI ui, UIModulator uiModulator, OutputOscFloatModulator modulator) {
    uiModulator.setContentHeight(UIKnob.HEIGHT + 4);
    uiModulator.setLayout(UI2dContainer.Layout.HORIZONTAL);
    uiModulator.setChildSpacing(2);
    uiModulator.addChildren(
        newTextBox(this.path).setWidth(150).setTextAlignment(VGraphics.Align.LEFT).setY(12),
        newKnob(this.value));
  }

  @Override
  public void disposeModulatorControls(
      LXStudio.UI ui, UIModulator uiModulator, OutputOscFloatModulator modulator) {
    UIModulatorControls.super.disposeModulatorControls(ui, uiModulator, modulator);
  }
}
