/**
 * Copyright 2013- Mark C. Slee, Heron Arts LLC
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
 */

package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import titanicsend.modulator.dmx.DmxColorModulator;

@LXCategory(LXCategory.COLOR)
@LXComponentName("Look Color Palette")
public class LookColorPaletteEffect extends LXEffect {

  public final CompoundParameter hue =
      new CompoundParameter("Hue", 0, -360, 360)
          .setPolarity(CompoundParameter.Polarity.BIPOLAR)
          .setDescription("Sets the amount of hue shift to apply");

  public final CompoundParameter saturation =
      new CompoundParameter("Saturation", 0, -100, 100)
          .setUnits(CompoundParameter.Units.PERCENT)
          .setPolarity(CompoundParameter.Polarity.BIPOLAR)
          .setDescription("Sets the amount to increase or decrease saturation");

  public final CompoundParameter brightness =
      new CompoundParameter("Brightness", 0, -100, 100)
          .setUnits(CompoundParameter.Units.PERCENT)
          .setPolarity(CompoundParameter.Polarity.BIPOLAR)
          .setDescription("Sets the amount to increase or decrease brightness");

  public final ColorParameter color = new ColorParameter("Color", LXColor.BLACK);

  public final EnumParameter<DmxColorModulator.ColorPosition> colorPosition =
      new EnumParameter<DmxColorModulator.ColorPosition>("Color Position", DmxColorModulator.ColorPosition.THREE)
          .setDescription(
              "Destination color position (1-based) in the global palette current swatch");

  public LookColorPaletteEffect(LX lx) {
    super(lx);
    addParameter("hue", this.hue);
    addParameter("saturation", this.saturation);
    addParameter("brightness", this.brightness);
    addParameter("colorPosition", this.colorPosition);
    addParameter("color", this.color);
  }

  @Override
  protected void run(double deltaMs, double amount) {
    float hue = this.hue.getValuef();
    float saturation = this.saturation.getValuef();
    float brightness = this.brightness.getValuef();

    int color = LXColor.hsb(hue, saturation, brightness);
    this.color.setColor(color);

    // Send to target color in global palette
    DmxColorModulator.ColorPosition colorPosition = this.colorPosition.getEnum();
    if (colorPosition != DmxColorModulator.ColorPosition.NONE) {
//      while (this.lx.engine.palette.swatch.colors.size() <= colorPosition.index) {
//        this.lx.engine.palette.swatch.addColor().primary.setColor(LXColor.BLACK);
//      }
      this.lx.engine.palette.swatch.getColor(colorPosition.index).primary.setColor(color);
      this.lx
          .engine
          .palette
          .swatch
          .getColor(colorPosition.index)
          .mode
          .setValue(LXDynamicColor.Mode.FIXED);
    }
  }
}
