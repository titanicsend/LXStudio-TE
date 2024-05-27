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
import heronarts.lx.color.LXPalette;
import heronarts.lx.color.LXSwatch;
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
  public final ColorParameter color2 = new ColorParameter("Color2", LXColor.BLACK);
  public final ColorParameter color3 = new ColorParameter("Color3", LXColor.BLACK);

  public final EnumParameter<DmxColorModulator.ColorPosition> colorPosition =
      new EnumParameter<DmxColorModulator.ColorPosition>("Color Position", DmxColorModulator.ColorPosition.THREE)
          .setDescription(
              "Destination color position (1-based) in the global palette current swatch");

  public final EnumParameter<DmxColorModulator.ColorPosition> secondPosition =
      new EnumParameter<DmxColorModulator.ColorPosition>("2nd Position", DmxColorModulator.ColorPosition.FOUR)
          .setDescription(
              "Destination color position (1-based) in the global palette current swatch");

  public final EnumParameter<DmxColorModulator.ColorPosition> thirdPosition =
      new EnumParameter<DmxColorModulator.ColorPosition>("3rd Position", DmxColorModulator.ColorPosition.FIVE)
          .setDescription(
              "Destination color position (1-based) in the global palette current swatch");

  public enum PaletteType {
    MONO,
    TRIADIC,
    ANALOGOUS,
    SPLIT_COMPLEMENTARY,
    COMPLEMENTARY,
  }

  public final EnumParameter<PaletteType> paletteType =
      new EnumParameter<PaletteType>("Color Position", PaletteType.TRIADIC)
          .setDescription(
              "Destination color position (1-based) in the global palette current swatch");

  public LookColorPaletteEffect(LX lx) {
    super(lx);
    addParameter("hue", this.hue);
    addParameter("saturation", this.saturation);
    addParameter("brightness", this.brightness);
    addParameter("colorPosition", this.colorPosition);
    addParameter("2ndPosition", this.secondPosition);
    addParameter("3rdPosition", this.thirdPosition);
    addParameter("color", this.color);
    addParameter("paletteType", this.paletteType);
  }

  public LXSwatch getActiveSwatch() {
    return this.lx.engine.palette.swatch;
  }

  public LXSwatch getCueSwatch() {
    return findOrCreateCueSwatch();
  }

  @Override
  protected void run(double deltaMs, double amount) {
    float hue = this.hue.getValuef();
    float saturation = this.saturation.getValuef();
    float brightness = this.brightness.getValuef();

    int color = LXColor.hsb(hue, saturation, brightness);
    int color2 = color;
    int color3 = color;
    this.color.setColor(color);

    if (this.paletteType.getEnum() == PaletteType.MONO) {
      color2 = LXColor.BLACK;
      color3 = LXColor.BLACK;
    } else if (this.paletteType.getEnum() == PaletteType.COMPLEMENTARY) {
      color2 = LXColor.hsb(hue + 180, saturation, brightness);
      color3 = LXColor.BLACK;
    } else if (this.paletteType.getEnum() == PaletteType.SPLIT_COMPLEMENTARY) {
      color2 = LXColor.hsb(hue + 150, saturation, brightness);
      color3 = LXColor.hsb(hue + 210, saturation, brightness);
    } else if (this.paletteType.getEnum() == PaletteType.TRIADIC) {
      color2 = LXColor.hsb(hue + 120, saturation, brightness);
      color3 = LXColor.hsb(hue + 240, saturation, brightness);
    } else if (this.paletteType.getEnum() == PaletteType.ANALOGOUS) {
      color2 = LXColor.hsb(hue + 30, saturation, brightness);
      color3 = LXColor.hsb(hue - 30, saturation, brightness);
    }
    this.color2.setColor(color2);
    this.color3.setColor(color3);

    // Send to target color in global palette
    LXSwatch activeSwatch = this.lx.engine.palette.swatch;
    setColorAtPosition(activeSwatch, this.colorPosition.getEnum(), color);
    setColorAtPosition(activeSwatch, this.secondPosition.getEnum(), color2);
    setColorAtPosition(activeSwatch, this.thirdPosition.getEnum(), color3);

    LXSwatch cueSwatch = findOrCreateCueSwatch();
    setColorAtPosition(cueSwatch, this.colorPosition.getEnum(), color);
    setColorAtPosition(cueSwatch, this.secondPosition.getEnum(), color2);
    setColorAtPosition(cueSwatch, this.thirdPosition.getEnum(), color3);
  }

  protected void setColorAtPosition(LXSwatch swatch, DmxColorModulator.ColorPosition colorPosition, int color) {
    if (colorPosition != DmxColorModulator.ColorPosition.NONE) {
      swatch.getColor(colorPosition.index).primary.setColor(color);
      swatch.getColor(colorPosition.index).mode.setValue(LXDynamicColor.Mode.FIXED);
    }
  }

  protected LXSwatch findOrCreateCueSwatch() {
    LXPalette palette = this.lx.engine.palette;
    if (this.lx.engine.palette.swatches.size() == 0) {
      palette.saveSwatch();
    }
    LXSwatch cueSwatch = palette.swatches.get(0);
    cueSwatch.label.setValue("CUE");
    return cueSwatch;
  }
}
