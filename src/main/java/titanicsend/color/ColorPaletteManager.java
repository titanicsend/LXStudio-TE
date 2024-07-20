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

@LXCategory(LXCategory.COLOR)
@LXComponentName("Look Color Palette")
public class ColorPaletteManager extends LXComponent {

  public final CompoundParameter hue =
      new CompoundParameter("H", 0, 0, 360)
          .setDescription("Sets the amount of hue shift to apply");

  public final CompoundParameter saturation =
      new CompoundParameter("S", 100, 0, 100)
          .setUnits(CompoundParameter.Units.PERCENT)
          .setDescription("Sets the amount to increase or decrease saturation");

  public final CompoundParameter brightness =
      new CompoundParameter("B", 100, 0, 100)
          .setUnits(CompoundParameter.Units.PERCENT)
          .setPolarity(CompoundParameter.Polarity.BIPOLAR)
          .setDescription("Sets the amount to increase or decrease brightness");

  public final ColorParameter color1 = new ColorParameter("Color1", LXColor.BLACK);
  public final ColorParameter color2 = new ColorParameter("Color2", LXColor.BLACK);
  public final ColorParameter color3 = new ColorParameter("Color3", LXColor.BLACK);
  public final BooleanParameter toggleCue =
      new BooleanParameter("Toggle Cue", false)
          .setDescription("Swap the cue and active swatches");

  public enum PaletteStrategy {
    MONO,
    ANALOGOUS,
    GOLDEN_RATIO_CONJUGATE,
    SPLIT_COMPLEMENTARY,
    COMPLEMENTARY,
    TRIADIC,
  }

  public final EnumParameter<PaletteStrategy> paletteStrategy =
      new EnumParameter<>("Palette Strategy", PaletteStrategy.TRIADIC)
          .setDescription(
              "Color theory rule to use when generating the secondary and tertiary colors");

  // update this so we know whether to re-render the palette
  public PaletteStrategy currPaletteStrategy = PaletteStrategy.TRIADIC;

  public static final String DEFAULT_NAME = "CUE";
  public static final int DEFAULT_INDEX = 0;

  private final String name;
  private final int index;

  private LXSwatch managedSwatch;

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
    addParameter("paletteStrategy", this.paletteStrategy);
    addParameter("color1", this.color1);
    addParameter("color2", this.color2);
    addParameter("color3", this.color3);
    // run `getManagedSwatch` to ensure the swatch is created, has the right name/num colors
    this.managedSwatch = managedSwatch();
  }

  public LXSwatch managedSwatch() {
    // ensure there are at least enough swatches in the global palette list to fetch
    // the correct index for this "managed swatch".
    while (this.lx.engine.palette.swatches.size() <= (this.index + 1)) {
      this.lx.engine.palette.saveSwatch();
    }
    this.managedSwatch = this.lx.engine.palette.swatches.get(this.index);
    this.managedSwatch.label.setValue(this.name);
    if (this.managedSwatch.colors.size() < LXSwatch.MAX_COLORS) {
      for (int i = this.managedSwatch.colors.size(); i < LXSwatch.MAX_COLORS; ++i) {
        this.managedSwatch.addColor();
      }
    }
    return this.managedSwatch;
  }

  public void updateSwatches() {
    updateSwatches(this.lx.engine.palette.swatch);
  }
  // Send to target color in global palette
  protected void updateSwatches(LXSwatch swatch) {
    setColorAtPosition(swatch, TEColorType.PRIMARY, this.color1.getColor());
    setColorAtPosition(swatch, TEColorType.SECONDARY, this.color2.getColor());
    setColorAtPosition(swatch, TEColorType.SECONDARY_BACKGROUND, this.color3.getColor());
  }

  protected void setColorAtPosition(LXSwatch swatch, TEColorType teColorType, int color) {
    swatch.getColor(teColorType.swatchIndex()).primary.setColor(color);
    swatch.getColor(teColorType.swatchIndex()).mode.setValue(LXDynamicColor.Mode.FIXED);
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    float hue = this.hue.getValuef();
    float saturation = this.saturation.getValuef();
    float brightness = this.brightness.getValuef();
    int color1 = LXColor.hsb(hue, saturation, brightness);
    if (color1 != this.color1.getColor() || this.currPaletteStrategy != this.paletteStrategy.getEnum()) {
      this.color1.setColor(color1);
      this.currPaletteStrategy = this.paletteStrategy.getEnum();
      updateColors2And3();
      updateSwatches(managedSwatch());
    }
  }

  private void updateColors2And3() {
    float hue = this.color1.hue.getValuef();
    float saturation = this.color1.saturation.getValuef();
    float brightness = this.color1.brightness.getValuef();
    int color2;
    int color3;
    switch(this.paletteStrategy.getEnum()) {
      case MONO:
        color2 = LXColor.BLACK;
        color3 = LXColor.BLACK;
        break;
      case GOLDEN_RATIO_CONJUGATE:
        color2 = LXColor.hsb(hue * 0.6180339, saturation, brightness);
        color3 = LXColor.BLACK;
        break;
      case COMPLEMENTARY:
        color2 = LXColor.hsb(hue + 180, saturation, brightness);
        color3 = LXColor.BLACK;
        break;
      case SPLIT_COMPLEMENTARY:
        color2 = LXColor.hsb(hue + 150, saturation, brightness);
        color3 = LXColor.hsb(hue + 210, saturation, brightness);
        break;
      case TRIADIC:
        color2 = LXColor.hsb(hue + 120, saturation, brightness);
        color3 = LXColor.hsb(hue + 240, saturation, brightness);
        break;
      case ANALOGOUS:
        color2 = LXColor.hsb(hue + 30, saturation, brightness);
        color3 = LXColor.hsb(hue - 30, saturation, brightness);
        break;
      default:
        throw new RuntimeException("Unknown PaletteStrategy: " + this.paletteStrategy.getEnum());
    }
    this.color2.setColor(color2);
    this.color3.setColor(color3);
  }
}
