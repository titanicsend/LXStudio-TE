/**
 * Copyright 2023- Justin K. Belcher, Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 * @author Justin K. Belcher <justin@jkb.studio>
 */

package titanicsend.modulator.dmx;

import heronarts.glx.ui.UI2dComponent;
import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.component.UIButton;
import heronarts.glx.ui.component.UIDropMenu;
import heronarts.glx.ui.component.UIIntegerBox;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UILabel;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXPalette;
import heronarts.lx.dmx.AbstractDmxModulator;
import heronarts.lx.dmx.DmxColorModulator;
import heronarts.lx.dmx.LXDmxEngine;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.fixture.UIFixture;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import titanicsend.color.ColorPaletteManager;

/**
 * Extracts a color from three DMX channels starting at a given address.
 */
@LXModulator.Global("DMX Director Color")
@LXModulator.Device("DMX Director Color")
@LXCategory(LXCategory.DMX)
public class DmxDirectorColorModulator extends AbstractDmxModulator
  implements LXOscComponent, UIModulatorControls<DmxDirectorColorModulator> {

  public final EnumParameter<LXDmxEngine.ByteOrder> byteOrder =
    new EnumParameter<LXDmxEngine.ByteOrder>("Byte Order", LXDmxEngine.ByteOrder.RGB);

  public final BooleanParameter updatePalette =
    new BooleanParameter("Palette", false)
      .setDescription("Updates the color palette manager's active swatch with the DMX color");

  public final DiscreteParameter paletteIndex =
    new LXPalette.IndexSelector("Index")
      .setDescription("Target index in the global palette's active swatch");

  public final ColorParameter color =
    new ColorParameter("Color", LXColor.BLACK)
      .setDescription("Color received by DMX");

  public DmxDirectorColorModulator(LX lx) {
    this(lx, "DMX Director Color");
  }

  // Change Chromatik abstract constructor to receive LX parameter?
  LX lx;

  public DmxDirectorColorModulator(LX lx, String label) {
    super(label, 3);
    this.lx = lx;
    addParameter("byteOrder", this.byteOrder);
    addParameter("updatePalette", this.updatePalette);
    addParameter("paletteIndex", this.paletteIndex);
    addParameter("color", this.color);
    setMappingSource(false);
    register();
  }

  ColorPaletteManager paletteManager;

  private void register() {
    this.paletteManager = (ColorPaletteManager) this.lx.engine.getChild("paletteManagerA");
  }

  @Override
  protected double computeValue(double deltaMs) {
    final LXDmxEngine.ByteOrder byteOrder = this.byteOrder.getEnum();

    final int color = this.lx.engine.dmx.getColor(
      this.universe.getValuei(),
      this.channel.getValuei(),
      byteOrder
    );

    // Store color locally for preview
    this.color.setColor(color);

    // Send to target color in global palette
    if (this.updatePalette.isOn()) {
      final int index = this.paletteIndex.getValuei() - 1;
      while (this.lx.engine.palette.swatch.colors.size() <= index) {
        this.lx.engine.palette.swatch.addColor().primary.setColor(LXColor.BLACK);
      }
      setPaletteManagerColor(color);
    }

    return this.color.getValue();
  }

  private void setPaletteManagerColor(int color) {
    float h = LXColor.h(color);
    float s = LXColor.s(color);
    float b = LXColor.b(color);
    if (this.paletteManager != null) {
      this.paletteManager.hue.setValue(h);
      this.paletteManager.saturation.setValue(s);
      this.paletteManager.brightness.setValue(b);
      // Push the managed swatch to the global palette immediately,
      // so it doesn't need to be "pinned".
      this.paletteManager.pushSwatch.trigger();
    }
  }

  public int getColor() {
    return this.color.getColor();
  }

  private static final int HEIGHT = 32;

  private UILabel label(LXStudio.UI ui, String label, float width, float x) {
    return (UILabel)
      controlLabel(ui, label, width)
        .setPosition(x, 20);
  }

  UI2dComponent colorUi;

  public void buildModulatorControls(LXStudio.UI ui, UIModulator uiModulator, final DmxDirectorColorModulator dmx) {
    uiModulator.setContentHeight(HEIGHT);
    uiModulator.setContentHeight(UIKnob.HEIGHT + 4);
//    uiModulator.setLayout(UI2dContainer.Layout.VERTICAL);
    uiModulator.setChildSpacing(2);


    uiModulator.addChildren(
      // new UI2dContainer(0, 0, uiModulator.getContentWidth(), HEIGHT)
      //  .addChildren(
        new UIIntegerBox(0, 0, 32, 16, this.universe),
        label(ui, "Univ", 32, 0),

        new UIIntegerBox(36, 0, 40, 16, this.channel),
        label(ui, "Chnl", 28, 36),
        UIFixture.constructDmxTooltip().setPlacement(UIButton.Tooltip.Placement.BOTTOM_LEFT).setPosition(64, 19),

        new UIDropMenu(80, 0, 36, 16, this.byteOrder),
        label(ui, "Order", 36, 80),

        new UIButton(120, 2, 16, 12, this.updatePalette).setIcon(ui.theme.iconRedo),

        new UIDropMenu(140, 0, 48, 16, this.paletteIndex),
        //new UIButton(140, 18, 48, 14, dmx.setPaletteFixed).setTextOffset(0, 1),

      colorUi = new UI2dComponent(uiModulator.getContentWidth() - 16, 0, 16, HEIGHT) {}
          .setBorderColor(ui.theme.controlBorderColor)
      // )
    );

    uiModulator.addListener(this.color, p -> {
      colorUi.setBackgroundColor(this.color.getColor());
    }, true);

  }

}
