package titanicsend.modulator.outputOsc;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIDropMenu;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.vg.VGraphics;
import heronarts.lx.LXCategory;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXPalette;
import heronarts.lx.dmx.LXDmxEngine;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.StringParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import heronarts.lx.utils.LXUtils;

@LXModulator.Global("Output OSC Color")
@LXModulator.Device("Output OSC Color")
@LXCategory("Output")
public class OutputOscColorModulator extends LXModulator
  implements LXOscComponent, UIModulatorControls<OutputOscColorModulator> {

  public final StringParameter path =
    new StringParameter("Path", "/te/color123");

  public final DiscreteParameter paletteIndex =
    new LXPalette.IndexSelector("Index")
      .setDescription("Target index in the global palette's active swatch");

  public final EnumParameter<LXDmxEngine.ByteOrder> byteOrder =
    new EnumParameter<LXDmxEngine.ByteOrder>("Byte Order", LXDmxEngine.ByteOrder.RGB);

  public final ColorParameter color =
    new ColorParameter("Color", LXColor.BLACK)
      .setDescription("Color to send");

  public OutputOscColorModulator() {
    this("Output OSC Color");
  }

  public OutputOscColorModulator(String label) {
    super(label);
    addParameter("path", this.path);
    addParameter("paletteIndex", this.paletteIndex);
    // addParameter("byteOrder", this.byteOrder);  // Not implemented
    addParameter("color", this.color);
  }

  private boolean canSend() {
    // Copied from LXComponent: These checks are necessary for bootstrapping, before the OSC engine
    // is spun up
    return (this.lx != null)
      && (this.lx.engine != null)
      && (this.lx.engine.osc != null)
      && (this.lx.engine.osc.transmitActive.isOn()
      && (this.lx.engine.output.enabled.isOn()));
  }

  @Override
  protected double computeValue(double deltaMs) {
    int paletteIndex = this.paletteIndex.getValuei() - 1;
    int color = this.lx.engine.palette.swatch.getColor(paletteIndex).getColor();
    this.color.setColor(color);
    String path = this.path.getString();
    if (!LXUtils.isEmpty(path) && canSend()) {
/*    LXDmxEngine.ByteOrder byteOrder = this.byteOrder.getEnum();
      LXColor.rgba(
        this.data[universe][channel + byteOrder.r],
        this.data[universe][channel + byteOrder.g],
        this.data[universe][channel + byteOrder.b],
        0xff
      );*/

      this.lx.engine.osc.sendMessage(path, color);
    }
    return 0;
  }

  private static final int HEIGHT = 32;

  UI2dComponent uiColor;

  @Override
  public void buildModulatorControls(LXStudio.UI ui, UIModulator uiModulator, OutputOscColorModulator modulator) {
    uiModulator.setContentHeight(UIKnob.HEIGHT + 4);
    uiModulator.setLayout(UI2dContainer.Layout.HORIZONTAL);
    uiModulator.setChildSpacing(2);

    uiModulator.addChildren(
      newTextBox(this.path)
        .setWidth(120)
        .setTextAlignment(VGraphics.Align.LEFT)
        .setY(12),

      new UIDropMenu(140, 0, 48, 16, this.paletteIndex),

      uiColor = new UI2dComponent(uiModulator.getContentWidth() - 16, 0, 16, HEIGHT) {}
        .setBorderColor(ui.theme.controlBorderColor)
    );

    uiModulator.addListener(this.color, p -> {
      uiColor.setBackgroundColor(this.color.getColor());
    }, true);
  }

  @Override
  public void disposeModulatorControls(LXStudio.UI ui, UIModulator uiModulator, OutputOscColorModulator modulator) {
    UIModulatorControls.super.disposeModulatorControls(ui, uiModulator, modulator);
  }
}
