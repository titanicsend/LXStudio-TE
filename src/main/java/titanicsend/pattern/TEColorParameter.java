package titanicsend.pattern;

import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.GradientUtils;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.*;
import heronarts.lx.utils.LXUtils;
import titanicsend.lx.LXGradientUtils;

public class TEColorParameter extends ColorParameter implements GradientUtils.GradientFunction {

    // SOLID-COLOR SOURCE

    public enum SolidColorSource {
        STATIC("Static"),
        FOREGROUND("Foreground"),
        GRADIENT("Selected Gradient");

        public final String label;

        private SolidColorSource(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    private final SolidColorSource SOLID_SOURCE_DEFAULT = SolidColorSource.FOREGROUND;

    public final EnumParameter<SolidColorSource> solidSource =
            new EnumParameter<SolidColorSource>("SolidSource", SOLID_SOURCE_DEFAULT) {
                @Override
                public LXParameter reset() {
                    // JKB: Don't worry about this, just avoiding a minor bug
                    // in EnumParameter. It'll be fixed soon.
                    setValue(SOLID_SOURCE_DEFAULT);
                    return this;
                }
            }
                    .setDescription("For a solid color: Whether to use global TE palette (preferred), or a static color unique to this pattern");

    public final CompoundParameter color2offset =
            new CompoundParameter("C2Offset", 0.5);

    // GRADIENT

    @SuppressWarnings("unchecked")
    public final EnumParameter<TEPattern.TEGradient> gradient = (EnumParameter<TEPattern.TEGradient>)
            new EnumParameter<TEPattern.TEGradient>("Gradient", TEPattern.TEGradient.FULL_PALETTE)
                    .setDescription("Which TEGradient to use. Full_Palette=entire, Foreground=Primary-Secondary, Primary=Primary-BackgroundPrimary, Secondary=Secondary-BackgroundSecondary")
                    .setWrappable(false);

    // GRADIENT BLEND. Excluding RGB because it does play well with gradients.

    public enum BlendMode {
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
            }
                    .setDescription("Blend mode for the gradient");

    // OFFSET affects both Solid Colors and Gradient

    // This custom wrapper class allows the device UI to render a color
    // picker on just this subparameter.
    public class TEColorOffsetParameter extends CompoundParameter {
        public TEColorOffsetParameter(String label) {
            super(label);
            setWrappable(true);
        }
    }

    private double lastOffset = 0;

    public final TEColorOffsetParameter offset = (TEColorOffsetParameter)
            new TEColorOffsetParameter("Offset") {
                @Override
                public BoundedParameter reset() {
                    super.reset();
                    // As the main user-facing sub-parameter, reset the color picker in STATIC mode.
                    if (solidSource.getEnum() == SolidColorSource.STATIC) {
                        brightness.reset();
                        saturation.reset();
                        hue.reset();
                    }
                    return this;
                }
            }
                    .setDescription("Allows user variation of solid color.  If Static, adjusts hue offset. If Palette, adjusts normalized position within gradient.");

    private final LXParameterListener offsetListener = (p) -> {
        double value = p.getValue();
        // When SolidColorSource is STATIC, turning the offset pushes the
        // hue position so the UI hue indicator stays in sync.
        if (solidSource.getEnum() == SolidColorSource.STATIC) {
            hue.incrementNormalized(value - lastOffset);
        }
        lastOffset = value;
    };

    private final TEPattern parentPattern;

    public TEColorParameter(TEPattern parentPattern, String label) {
        this(parentPattern, label, 0xff000000);
    }

    public TEColorParameter(TEPattern parentPattern, String label, int color) {
        super(label, color);
        this.parentPattern = parentPattern;

        // Modify defaults of sat/bright
        this.saturation.reset(100);
        this.brightness.reset(100);

        offset.addListener(offsetListener);

        addSubparameter("solidSource", this.solidSource);
        addSubparameter("gradient", this.gradient);
        addSubparameter("blendMode", this.blendMode);
        addSubparameter("offset", this.offset);
        addSubparameter("color2offset", this.color2offset);
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
     * <p>
     * Returns the real-time value of the color, which may be different from what
     * getColor() returns if there are LFOs/etc being applied.
     * Offset has been applied to this color.
     */
    public int calcColor() {
        switch (this.solidSource.getEnum()) {
            case GRADIENT:
                // TODO: scale brightness here
                return _getGradientColor(getOffsetf());
            case FOREGROUND:
                // TODO: scale brightness here
                return _getGradientColor(getOffsetf(), TEPattern.TEGradient.FOREGROUND);
            default:
            case STATIC:
                return LXColor.hsb(
                        this.hue.getValue(),
                        this.saturation.getValue(),
                        this.brightness.getValue()
                );
        }
    }

    /**
     * Solid-Color patterns that use two colors can get
     * the second color here.
     *
     * @return LXColor
     */
    public int calcColor2() {
        switch (this.solidSource.getEnum()) {
            case GRADIENT:
                // TODO: scale brightness here
                return _getGradientColor(getOffsetf() + color2offset.getValuef());
            case FOREGROUND:
                // TODO: scale brightness here
                return _getGradientColor(getOffsetf() + color2offset.getValuef(), TEPattern.TEGradient.FOREGROUND);
            default:
            case STATIC:
                return LXColor.hsb(
                        this.hue.getValue() + (color2offset.getValue() * 360.),
                        this.saturation.getValue(),
                        this.brightness.getValue()
                );
        }
    }

    /**
     * Returns a base color pre-modulators and pre-offset.
     * Patterns are encouraged to use calcColor() instead.
     */
    @Override
    public int getColor() {
        switch (this.solidSource.getEnum()) {
            case FOREGROUND:
                return getGradientColor(0);
            default:
            case STATIC:
                return super.getColor();
        }
    }

    // GRADIENT METHODS

    /**
     * ** Gradient patterns should use this method **
     * <p>
     * Given a value in 0..1 (and wrapped back outside that range)
     * Return a color within the selected gradient.
     * Offset is added to lerp to create a user-shiftable gradient.
     *
     * @param lerp as a frac
     * @return LXColor
     */
    public int getGradientColor(float lerp) {
        return _getGradientColor(lerp + getOffsetf());
    }

    /**
     * Returns absolute position within current gradient.
     *
     * @param lerp
     * @return
     */
    private int _getGradientColor(float lerp) {
        return _getGradientColor(lerp, this.gradient.getEnum());
    }

    private int _getGradientColor(float lerp, TEPattern.TEGradient gradient) {
        lerp = (float) LXUtils.wrapnf(lerp);

        LXGradientUtils.BlendFunction bf;
        switch (this.blendMode.getEnum()) {
            case HSVCCW:
                bf = LXGradientUtils.BlendMode.HSVCCW.function;
                break;
            case HSVCW:
                bf = LXGradientUtils.BlendMode.HSVCW.function;
                break;
            case HSVM:
            default:
                bf = LXGradientUtils.BlendMode.HSVM.function;
        }

        return getGradientStops(gradient).getColor(lerp, bf);
    }

    /**
     * Internal helper method. Maps gradient enum to ColorStops.
     */
    private LXGradientUtils.ColorStops getGradientStops(TEPattern.TEGradient gradient) {
        switch (gradient) {
            case FOREGROUND:
                return this.parentPattern.foregroundGradient;
            case PRIMARY:
                return this.parentPattern.primaryGradient;
            case SECONDARY:
                return this.parentPattern.secondaryGradient;
            case FULL_PALETTE:
            default:
                return this.parentPattern.paletteGradient;
        }
    }

    @Override
    protected void onSubparameterUpdate(LXParameter p) {
        // TODO: some fixing up here
        if (this.solidSource.getEnum() == SolidColorSource.FOREGROUND) {
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