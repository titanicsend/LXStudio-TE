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

package titanicsend.color;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.color.LXPalette;
import heronarts.lx.color.LXSwatch;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.modulator.dmx.DmxColorModulator;

@LXCategory(LXCategory.COLOR)
@LXComponentName("Look Color Palette")
public class ColorPaletteManager extends LXComponent {

  public final CompoundParameter hue =
      new CompoundParameter("H", 0, -360, 360)
          .setPolarity(CompoundParameter.Polarity.BIPOLAR)
          .setDescription("Sets the amount of hue shift to apply");

  public final CompoundParameter saturation =
      new CompoundParameter("S", 100, -100, 100)
          .setUnits(CompoundParameter.Units.PERCENT)
          .setPolarity(CompoundParameter.Polarity.BIPOLAR)
          .setDescription("Sets the amount to increase or decrease saturation");

  public final CompoundParameter brightness =
      new CompoundParameter("B", 100, -100, 100)
          .setUnits(CompoundParameter.Units.PERCENT)
          .setPolarity(CompoundParameter.Polarity.BIPOLAR)
          .setDescription("Sets the amount to increase or decrease brightness");

  public final ColorParameter color1 = new ColorParameter("Color1", LXColor.BLACK);
  public final ColorParameter color2 = new ColorParameter("Color2", LXColor.BLACK);
  public final ColorParameter color3 = new ColorParameter("Color3", LXColor.BLACK);
  // update this so we know whether to re-render the palette
  public PaletteType currPaletteType = PaletteType.TRIADIC;

  public final EnumParameter<DmxColorModulator.ColorPosition> color1Pos =
      new EnumParameter<DmxColorModulator.ColorPosition>("Color Position", DmxColorModulator.ColorPosition.THREE)
          .setDescription(
              "Destination color position (1-based) in the global palette current swatch");

  public final EnumParameter<DmxColorModulator.ColorPosition> color2Pos =
      new EnumParameter<DmxColorModulator.ColorPosition>("2nd Position", DmxColorModulator.ColorPosition.FOUR)
          .setDescription(
              "Destination color position (1-based) in the global palette current swatch");

  public final EnumParameter<DmxColorModulator.ColorPosition> color3Pos =
      new EnumParameter<DmxColorModulator.ColorPosition>("3rd Position", DmxColorModulator.ColorPosition.FIVE)
          .setDescription(
              "Destination color position (1-based) in the global palette current swatch");

  public final BooleanParameter toggleCue =
      new BooleanParameter("Toggle Cue", false)
          .setDescription("Swap the cue and active swatches");

  public enum PaletteType {
    MONO,
    TRIADIC,
    ANALOGOUS,
    SPLIT_COMPLEMENTARY,
    COMPLEMENTARY,
    // TODO(look): Jon Idea of Golden Ratio Conjugate (mult hue by 1.6181)
  }

  public final EnumParameter<PaletteType> paletteType =
      new EnumParameter<PaletteType>("Color Position", PaletteType.TRIADIC)
          .setDescription(
              "Destination color position (1-based) in the global palette current swatch");

  public static final String DEFAULT_NAME = "CUE";
  public static final int DEFAULT_INDEX = 0;

  private final String name;
  private final int index;

  public ColorPaletteManager(LX lx) {
    this(lx, DEFAULT_NAME, DEFAULT_INDEX);
  }

  public ColorPaletteManager(LX lx, String name, int index) {
    super(lx);
    this.name = name;
    this.index = index;
    addParameter("hue", this.hue);
    addParameter("saturation", this.saturation);
    addParameter("brightness", this.brightness);
    addParameter("paletteType", this.paletteType);
    addParameter("color1Position", this.color1Pos);
    addParameter("color2Position", this.color2Pos);
    addParameter("color3Position", this.color3Pos);
    addParameter("color1", this.color1);
    addParameter("color2", this.color2);
    addParameter("color3", this.color3);

    // ensure there are at least enough swatches in the global palette list to fetch
    // the correct index for this "managed swatch". TODO: listen in case it's deleted?
    if (lxPalette().swatches.size() <= (this.index + 1)) {
      lxPalette().saveSwatch();
    }
    // run `getManagedSwatch` to ensure the swatch is created, has the right name/num colors
  }

  protected LXPalette lxPalette() {
    return this.lx.engine.palette;
  }

  protected LXSwatch managedSwatch() {
    LXSwatch cueSwatch = lxPalette().swatches.get(this.index);
    cueSwatch.label.setValue(this.name);
    if (cueSwatch.colors.size() < LXSwatch.MAX_COLORS) {
      for (int i = cueSwatch.colors.size(); i < LXSwatch.MAX_COLORS; ++i) {
        cueSwatch.addColor();
      }
    }
    return cueSwatch;
  }

  public void swapCueSwatch() {
    LXSwatch activeSwatch = this.lx.engine.palette.swatch;

    setColorAtPosition(activeSwatch, this.color1Pos.getEnum(), this.color1.getColor());
    setColorAtPosition(activeSwatch, this.color2Pos.getEnum(), this.color2.getColor());
    setColorAtPosition(activeSwatch, this.color3Pos.getEnum(), this.color3.getColor());

//    int activeColor1 = active.getColor(this.color1Pos.getEnum().index).primary.getColor();
//    this.hue.setValue(LXColor.h(activeColor1));
//    this.saturation.setValue(LXColor.s(activeColor1));
//    this.brightness.setValue(LXColor.b(activeColor1));
//

//    for (int i = 0; i < LXSwatch.MAX_COLORS; ++i) {
////      int activeColor = active.getColor(i).primary.getColor();
////      int cueColor = cue.getColor(i).primary.getColor();
//      active.getColor(i).primary.setColor(cueColor);
//      cue.getColor(i).primary.setColor(activeColor);
//    }
  }

  // TODO: properly introspect "parameter" and decide what to do more carefully
  public void onParameterChanged(LXParameter parameter) {
    float hue = this.hue.getValuef();
    float saturation = this.saturation.getValuef();
    float brightness = this.brightness.getValuef();
    int color1 = LXColor.hsb(hue, saturation, brightness);
    if (color1 != this.color1.getColor() || this.currPaletteType != this.paletteType.getEnum()) {
      this.color1.setColor(color1);
      this.currPaletteType = this.paletteType.getEnum();

      updateColors2And3(color1);
      updateSwatches(managedSwatch());
//      setColorAtPosition(cueSwatch, this.color1Pos.getEnum(), color1);
//      setColorAtPosition(cueSwatch, this.color2Pos.getEnum(), color2);
//      setColorAtPosition(cueSwatch, this.color3Pos.getEnum(), color3);
    }
  }

  private void updateColors2And3(int color1) {
    this.currPaletteType = this.paletteType.getEnum();

    float hue = this.color1.hue.getValuef();
    float saturation = this.color1.saturation.getValuef();
    float brightness = this.color1.brightness.getValuef();
    int color2 = color1;
    int color3 = color1;
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
  }

  private void updateSwatches(LXSwatch swatch) {
    // Send to target color in global palette
    setColorAtPosition(swatch, this.color1Pos.getEnum(), this.color1.getColor());
    setColorAtPosition(swatch, this.color2Pos.getEnum(), this.color2.getColor());
    setColorAtPosition(swatch, this.color3Pos.getEnum(), this.color3.getColor());
  }

  protected void setColorAtPosition(LXSwatch swatch, DmxColorModulator.ColorPosition colorPosition, int color) {
    if (colorPosition != DmxColorModulator.ColorPosition.NONE) {
      swatch.getColor(colorPosition.index).primary.setColor(color);
      swatch.getColor(colorPosition.index).mode.setValue(LXDynamicColor.Mode.FIXED);
    }
  }
}
