package titanicsend.color;

import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.GradientUtils;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.utils.LXUtils;
import titanicsend.lx.LXGradientUtils;

public class TEColorParameter extends ColorParameter implements GradientUtils.GradientFunction {

  // COLOR SOURCE

  public enum ColorSource {
    STATIC("Static"),
    NORMAL("Normal TE"),
    DARK("Dark TE");

    public final String label;

    private ColorSource(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  private final ColorSource COLOR_SOURCE_DEFAULT = ColorSource.NORMAL;

  public final EnumParameter<ColorSource> colorSource =
      new EnumParameter<ColorSource>("ColorSource", COLOR_SOURCE_DEFAULT) {
        @Override
        public LXParameter reset() {
          // JKB: Don't worry about this, just avoiding a minor bug
          // in EnumParameter. It'll be fixed soon.
          setValue(COLOR_SOURCE_DEFAULT);
          return this;
        }
      }.setDescription(
          "Whether to use global TE palette (preferred), or a static color unique to this pattern");

  // BLEND MODE FOR THE GRADIENT. Excluding RGB because it does not play well with gradients.

  public enum BlendMode {
    OKLAB,
    HSVM,
    HSVCW,
    HSVCCW
  }

  private final BlendMode BLEND_MODE_DEFAULT = BlendMode.HSVM;

  public final EnumParameter<BlendMode> blendMode =
      new EnumParameter<BlendMode>("BlendMode", BLEND_MODE_DEFAULT) {
        @Override
        public LXParameter reset() {
          // JKB: Don't worry about this, just avoiding a minor bug
          // in EnumParameter. It'll be fixed soon.
          setValue(BLEND_MODE_DEFAULT);
          return this;
        }
      }.setDescription("Blend mode for the gradient");

  // OFFSET

  // This custom wrapper class allows the device UI to render a color
  // picker on just this subparameter.
  public class TEColorOffsetParameter extends CompoundParameter {
    public TEColorOffsetParameter(String label) {
      super(label);
      setWrappable(true);
    }
  }

  private double lastOffset = 0;

  public final TEColorOffsetParameter offset =
      (TEColorOffsetParameter)
          new TEColorOffsetParameter("Offset") {
            @Override
            public BoundedParameter reset() {
              super.reset();
              // As the main user-facing sub-parameter, reset the color picker in STATIC mode.
              if (colorSource.getEnum() == ColorSource.STATIC) {
                brightness.reset();
                saturation.reset();
                hue.reset();
              }
              return this;
            }
          }.setDescription(
              "Allows user variation of solid color.  If Static, adjusts hue offset. If Palette, adjusts normalized position within gradient.");

  private final LXParameterListener offsetListener =
      (p) -> {
        double value = p.getValue();
        // When SolidColorSource is STATIC, turning the offset pushes the
        // hue position so the UI hue indicator stays in sync.
        if (colorSource.getEnum() == ColorSource.STATIC) {
          hue.incrementNormalized(value - lastOffset);
        }
        lastOffset = value;
      };

  private final TEGradientSource gradientSource;

  public TEColorParameter(TEGradientSource gradientSource, String label) {
    this(gradientSource, label, 0xff000000);
  }

  public TEColorParameter(TEGradientSource gradientSource, String label, int color) {
    super(label, color);
    this.gradientSource = gradientSource;

    // Modify defaults of sat/bright
    this.saturation.reset(100);
    this.brightness.reset(100);

    offset.addListener(offsetListener);

    addSubparameter("solidSource", this.colorSource);
    addSubparameter("blendMode", this.blendMode);
    addSubparameter("offset", this.offset);
  }

  @Override
  public TEColorParameter setDescription(String description) {
    return (TEColorParameter) super.setDescription(description);
  }

  @Override
  public LXListenableNormalizedParameter getRemoteControl() {
    return this.offset;
  }

  public double getOffset() {
    return this.offset.getValue();
  }

  public final float getOffsetf() {
    return (float) getOffset();
  }

  // SOLID-COLOR METHODS

  /**
   * ** Solid-Color patterns should use this method **
   *
   * <p>Returns the real-time value of the color, which may be different from what getColor()
   * returns if there are LFOs/etc being applied. Offset has been applied to this color.
   */
  public int calcColor() {
    switch (this.colorSource.getEnum()) {
      case NORMAL:
        // TODO: scale brightness here
        return getGradientColorFixed(getOffsetf(), TEGradient.NORMAL);
      case DARK:
        // TODO: scale brightness here
        return getGradientColorFixed(getOffsetf(), TEGradient.DARK);
      case STATIC:
      default:
        return LXColor.hsb(
            this.hue.getValue(), this.saturation.getValue(), this.brightness.getValue());
    }
  }

  /**
   * Solid-Color patterns that use two colors can get the second color here.
   *
   * @return LXColor
   */
  public int calcColor2() {
    // TODO: This needs to maintain the hue offset used by ColorPaletteManager

    // TODO: Below is is a quick fix to get color2 to work -- if I understand
    // TODO: correctly, an offset of 0.5 will get us to the second color in a two color
    // TODO: palette, and to a still-reasonable color in a three color palette.
    // TODO: Not sure what we want here.  Would be nice to have a controllable
    // TODO offset for color2.
    float colorPaletteManagerOffset = 0.5f;

    switch (this.colorSource.getEnum()) {
      case NORMAL:
        // TODO: scale brightness here
        return getGradientColorFixed(getOffsetf() + colorPaletteManagerOffset);
      case DARK:
        // TODO: scale brightness here
        return LXColor.BLACK;
      case STATIC:
      default:
        return LXColor.hsb(
            this.hue.getValue(), this.saturation.getValue(), this.brightness.getValue());
    }
  }

  /**
   * Returns a base color pre-modulators and pre-offset. Patterns are encouraged to use calcColor()
   * instead.
   */
  @Override
  public int getColor() {
    switch (this.colorSource.getEnum()) {
      case NORMAL:
        return getGradientColorFixed(0, TEGradient.NORMAL);
      case DARK:
        return getGradientColorFixed(0, TEGradient.DARK);
      default:
      case STATIC:
        return super.getColor();
    }
  }

  // GRADIENT METHODS

  /**
   * ** Gradient patterns should use this method **
   *
   * <p>Given a value in 0..1 (and wrapped back outside that range) Return a color within the
   * selected gradient. Offset is added to lerp to create a user-shiftable gradient.
   *
   * @param lerp as a frac
   * @return LXColor
   */
  public int getGradientColor(float lerp) {
    return getGradientColorFixed(lerp + getOffsetf());
  }

  /** Returns absolute position within current gradient. */
  public int getGradientColorFixed(float lerp) {
    switch (this.colorSource.getEnum()) {
      case STATIC:
        return super.getColor();
      case DARK:
        return getGradientColorFixed(lerp, TEGradient.DARK);
      case NORMAL:
      default:
        return getGradientColorFixed(lerp, TEGradient.NORMAL);
    }
  }

  private int getGradientColorFixed(float lerp, TEGradient gradient) {
    lerp = (float) LXUtils.wrapnf(lerp);

    LXGradientUtils.BlendFunction bf;
    switch (this.blendMode.getEnum()) {
      case HSVCCW:
        bf = LXGradientUtils.BlendMode.HSVCCW.function;
        break;
      case HSVCW:
        bf = LXGradientUtils.BlendMode.HSVCW.function;
        break;
      case OKLAB:
        bf = LXGradientUtils.BlendMode.OKLAB.function;
        break;
      case HSVM:
      default:
        bf = LXGradientUtils.BlendMode.HSVM.function;
    }

    return getGradientStops(gradient).getColor(lerp, bf);
  }

  /** Internal helper method. Maps gradient enum to ColorStops. */
  private LXGradientUtils.ColorStops getGradientStops(TEGradient gradient) {
    switch (gradient) {
      case DARK:
        return this.gradientSource.darkGradient;
      case NORMAL:
      default:
        return this.gradientSource.normalGradient;
    }
  }

  @Override
  protected void onSubparameterUpdate(LXParameter p) {
    // TODO: some fixing up here
    if (this.colorSource.getEnum() == ColorSource.NORMAL) {
      setColor(getGradientColor(0));
    } else {
      super.onSubparameterUpdate(p);
    }
  }

  @Override
  public void dispose() {
    this.offset.removeListener(offsetListener);
    super.dispose();
  }
}
