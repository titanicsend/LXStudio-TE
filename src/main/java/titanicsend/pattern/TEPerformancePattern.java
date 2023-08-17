package titanicsend.pattern;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.GradientUtils.GradientFunction;
import heronarts.lx.color.LXColor;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.model.LXModel;
import heronarts.lx.parameter.*;
import heronarts.lx.parameter.BooleanParameter.Mode;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.utils.LXUtils;
import titanicsend.lx.LXGradientUtils;
import titanicsend.lx.LXGradientUtils.BlendFunction;
import titanicsend.model.justin.ColorCentral;
import titanicsend.model.justin.DisposableParameter.DisposeListener;
import titanicsend.model.justin.LXVirtualDiscreteParameter;
import titanicsend.model.justin.ViewCentral;
import titanicsend.model.justin.ViewCentral.ViewCentralListener;
import titanicsend.model.justin.ViewCentral.ViewParameter;
import titanicsend.pattern.jon.TEControl;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.pattern.jon._CommonControlGetter;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.MissingControlsManager;
import titanicsend.util.TE;
import titanicsend.util.TEColor;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public abstract class TEPerformancePattern extends TEAudioPattern {

    public class TEColorParameter extends ColorParameter implements GradientFunction {

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
        public final EnumParameter<TEGradient> gradient = (EnumParameter<TEGradient>)
            new EnumParameter<TEGradient>("Gradient", TEGradient.FULL_PALETTE)
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

        public TEColorParameter(String label) {
            this(label, 0xff000000);
        }

        public TEColorParameter(String label, int color) {
            super(label, color);

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
         *
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
                    return _getGradientColor(getOffsetf(), TEGradient.FOREGROUND);
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
                    return _getGradientColor(getOffsetf() + color2offset.getValuef(), TEGradient.FOREGROUND);
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
         *
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

        private int _getGradientColor(float lerp, TEGradient gradient) {
            lerp = (float) LXUtils.wrapnf(lerp);

            BlendFunction bf;
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
        private LXGradientUtils.ColorStops getGradientStops(TEGradient gradient) {
            switch (gradient) {
                case FOREGROUND:
                    return foregroundGradient;
                case PRIMARY:
                    return primaryGradient;
                case SECONDARY:
                    return secondaryGradient;
                case FULL_PALETTE:
                default:
                    return paletteGradient;
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

    // Explicitly keep this available for now
    @Override
    public int getSwatchColor(ColorType type) {
        return super.getSwatchColor(type);
    }

    // VIEW-PER-CHANNEL  *We have switch to per-pattern. Not deleting this code for sake of easy field edits.

    public class LXVirtualViewParameter extends LXVirtualDiscreteParameter<ViewParameter> implements ViewCentralListener {

        /* Two conditions are needed to link this virtual parameter to ViewCentral:
         *   1. ViewCentral instance must be loaded
         *   2. This pattern needs to be added to a channel.
         *      (during file load, happens after pattern is loaded)
         *      (LXPattern.setChannel() and LXPattern.setParent() are final, so
         *       to make life easier we're linking this on first engine loop.)
         */
        public LXVirtualViewParameter(String label) {
            super(label);

            setIncrementMode(IncrementMode.RELATIVE);
            setWrappable(false);

            if (ViewCentral.isLoaded()) {
                link();
            } else {
                listenForLoad();
            }
        }

        public void listenForLoad() {
            if (getParameter() == null) {
                ViewCentral.listenOnce(this);
            }
        }

        @Override
        public void ViewCentralLoaded() {
            link();
        }

        public void link() {
            if (getParameter() == null) {
                if (ViewCentral.isLoaded()) {
                    LXChannel channel = getChannel();
                    if (channel != null) {
                        setParameter(ViewCentral.get().get(channel).view);
                    }
                }
            }
        }

        // Type-specific pass-through
        public LXVirtualViewParameter setView(String label) {
            ViewParameter p = getParameter();
            if (p != null) {
                p.setView(label);
            }
            return this;
        }

        @Override
        public LXVirtualViewParameter reset() {
            TEShaderView patternDefaultView = getDefaultView();
            if (patternDefaultView != null) {
              return setView(getDefaultView().getParameterKey());
            }
            return (LXVirtualViewParameter)super.reset();
        }
    }

    private TEShaderView defaultView = null;

    /**
     * Subclasses can override to specify a preferred default view.
     * Alternatively, just pass a default to TEPerformancePattern's constructor.
     * 
     * Warning for overrides: 
     *   Called from this constructor prior to child class constructors.
     */
    public TEShaderView getDefaultView() {
        return defaultView;
    }

    // VIEW PER PATTERN

    // Hide LXModelComponent.model, redirect to view-per-pattern
    protected LXModel model;

    @Override
    public LXModel getModel() {
        return this.model;
    }

    public ViewParameter viewPerPattern;

    private LXParameterListener viewPerPatternListener = (p) -> {
        this.model = viewPerPattern.getModel();
        onModelChanged(this.model);
        // TEPattern calls clearPixels() after onModelChanged()
        if (this.lx instanceof LXStudio) {
          ((LXStudio) lx).ui.setMouseoverHelpText("View:  " + viewPerPattern.getObject().toString());
        }
    };

    private DisposeListener viewPerPatternDisposing = (p) -> {
        if (p == this.viewPerPattern) {
            this.viewPerPattern.removeListener(viewPerPatternListener);
            this.viewPerPattern = null;
        }
    };

    // ANGLE PARAMETER

    // Create new class for Angle control so we can override the reset
    // behavior and have reset set the current composite rotation angle
    // to the spin control's current setting.
    class TECommonAngleParameter extends CompoundParameter {

        public TECommonAngleParameter(String label, double value, double v0, double v1) {
            super(label, value, v0, v1);
        }

        @Override
        public LXListenableNormalizedParameter incrementNormalized(double amount) {
            // High resolution
            return super.incrementNormalized(amount / 5.);
        }

        @Override
        public BoundedParameter reset() {
            // if not spinning, resetting angle controls
            // resets both the static angle and the spin angle.
            if (getSpin() == 0) {
                spinRotor.setAngle(0);
            }

            // If spinning, reset static angle to 0, and also
            // add a corresponding offset to spinRotor to avoid a visual glitch.
            else {
                spinRotor.addAngle(-this.getValue());
            }
            return super.reset();
        }
    }

    public class TECommonControls {

        // Color control is accessible, in case the pattern needs something
        // other than the current color.
        public TEColorParameter color;

        // Virtual parameter: it's a pass-through to ViewCentral's current view for this channel
        public final LXVirtualViewParameter viewParameter =
            new LXVirtualViewParameter("View") {
        };

        // Panic control courtesy of JKB's Rubix codebase
        public final BooleanParameter panic = (BooleanParameter)
            new BooleanParameter("PANIC", false)
            .setDescription("Panic! Moves parameters into a visible range")
            .setMode(Mode.MOMENTARY);

        private final LXParameterListener panicListener = (p) -> {
            if (((BooleanParameter)p).getValueb()) {
              onPanic();
            }
        };

        _CommonControlGetter defaultGetFn = new _CommonControlGetter() {
            @Override
            public double getValue(TEControl cc) {
                return cc.getValue();
            }
        };


        private final HashMap<TEControlTag, TEControl> controlList = new HashMap<TEControlTag, TEControl>();
        public final Set<LXNormalizedParameter> unusedParams = new HashSet<>();


        /**
         * Retrieve backing LX control object for given tag
         *
         * @param tag
         */
        public LXListenableNormalizedParameter getLXControl(TEControlTag tag) {
            return controlList.get(tag).control;
        }

        public TEControl getControl(TEControlTag tag) {
            return controlList.get(tag);
        }

        /**
         * Get current value of control specified by tag by calling
         * the tag's configured getter function (and NOT by directly calling
         * the control's getValue() function)
         *
         * @param tag
         */
        protected double getValue(TEControlTag tag) {
            TEControl ctl = controlList.get(tag);
            return ctl.getFn.getValue(ctl);
        }

        public TECommonControls setControl(TEControlTag tag, LXListenableNormalizedParameter lxp, _CommonControlGetter getFn) {
            TEControl newControl = new TEControl(lxp, getFn);
            controlList.put(tag, newControl);
            return this;
        }

        public TECommonControls setControl(TEControlTag tag, LXListenableNormalizedParameter lxp) {
            return setControl(tag, lxp, defaultGetFn);
        }

        /**
         * Sets a new getter function (an object implementing the _CommonControlGetter
         * interface) for specified tag's control.
         *
         * @param tag
         * @param getFn
         */
        public TECommonControls setGetterFunction(TEControlTag tag, _CommonControlGetter getFn) {
            controlList.get(tag).getFn = getFn;
            return this;
        }

        private static LXListenableNormalizedParameter updateParam(LXListenableNormalizedParameter oldControl, String label, double value, double v0, double v1) {
            LXListenableNormalizedParameter newControl;
            if (oldControl instanceof CompoundParameter) {
                newControl = (CompoundParameter) new CompoundParameter(label, value, v0, v1)
                    .setNormalizationCurve(((CompoundParameter)oldControl).getNormalizationCurve())
                    .setExponent(oldControl.getExponent())
                    .setDescription(oldControl.getDescription())
                    .setPolarity(oldControl.getPolarity())
                    .setUnits(oldControl.getUnits());
            } else if (oldControl instanceof BoundedParameter) {
                newControl  = (BoundedParameter) new BoundedParameter(label, value, v0, v1)
                    .setNormalizationCurve(((BoundedParameter)oldControl).getNormalizationCurve())
                    .setExponent(oldControl.getExponent())
                    .setDescription(oldControl.getDescription())
                    .setPolarity(oldControl.getPolarity())
                    .setUnits(oldControl.getUnits());
            } else if (oldControl instanceof BooleanParameter) {
                newControl  = (BooleanParameter) new BooleanParameter(label)
                    .setMode(((BooleanParameter)oldControl).getMode())
                    .setDescription(oldControl.getDescription())
                    .setUnits(oldControl.getUnits());
            } else if (oldControl instanceof DiscreteParameter) {
                newControl  = (DiscreteParameter) new DiscreteParameter(label, ((DiscreteParameter)oldControl).getOptions())
                    .setIncrementMode(((DiscreteParameter)oldControl).getIncrementMode())
                    .setDescription(oldControl.getDescription())
                    .setUnits(oldControl.getUnits());
            } else {
                TE.err("Unrecognized control type in TE Common Control " + oldControl.getClass().getName());
                return oldControl;
            }
            return newControl;
        }

        public TECommonControls setRange(TEControlTag tag, double value, double v0, double v1) {
            LXListenableNormalizedParameter oldControl = getLXControl(tag);
            LXListenableNormalizedParameter newControl = updateParam(oldControl, oldControl.getLabel(), value, v0, v1);
            setControl(tag, newControl);
            return this;
        }

        public TECommonControls setLabel(TEControlTag tag, String newLabel) {
            LXListenableNormalizedParameter oldControl = getLXControl(tag);
            double value = 0d;
            double v0 = 0d;
            double v1 = 0d;
            if (oldControl instanceof CompoundParameter) {
                value = oldControl.getValue();
                v0 = ((CompoundParameter) oldControl).range.v0;
                v1 = ((CompoundParameter) oldControl).range.v1;
            } else if (oldControl instanceof BoundedParameter) {
                value = ((BoundedParameter) oldControl).getValue();
                v0 = ((BoundedParameter) oldControl).range.v0;
                v1 = ((BoundedParameter) oldControl).range.v1;
            }
            LXListenableNormalizedParameter newControl = updateParam(oldControl, newLabel, value, v0, v1);
            setControl(tag, newControl);
            return this;
        }

        public TECommonControls setExponent(TEControlTag tag, double exp) {
            LXListenableNormalizedParameter p = getLXControl(tag);
            p.setExponent(exp);
            return this;
        }

        public TECommonControls setNormalizationCurve(TEControlTag tag, BoundedParameter.NormalizationCurve curve) {
            LXListenableNormalizedParameter p = getLXControl(tag);
            if (p instanceof BoundedParameter) {
                ((BoundedParameter)p).setNormalizationCurve(curve);
            } else {
                TE.log("Warning: setNormalizationCurve() can not be called on parameter " + tag.toString());
            }
            return this;
        }

        public TECommonControls setUnits(TEControlTag tag, LXParameter.Units units) {
            LXListenableNormalizedParameter p = getLXControl(tag);
            p.setUnits(units);
            return this;
        }

        /**
         * To use the common controls, call this function from the constructor
         * of TEPerformancePattern-derived classes after configuring the default
         * controls for your pattern.
         *
         * If your pattern adds its own controls in addition to the common
         * controls, you must call addParameter() for them after calling
         * this function so the UI stays consistent across patterns.
         */
        public void addCommonControls(TEPerformancePattern pat) {
            // load the missing controls file
            MissingControlsManager.MissingControls missingControls = MissingControlsManager.get().findMissingControls(pat.getClass());

            String colorPrefix = "";
            if (missingControls != null && !missingControls.uses_palette) {
                colorPrefix = "[x] ";
            }
            TEColorParameter colorParam = registerColorControl(colorPrefix);
//            if (missingControls != null && !missingControls.uses_palette) {
//                markUnused(colorParam.offset);
//                markUnused(colorParam.gradient);
//                markUnused(swatchParameter);
//            }

            // controls will be added in the order their tags appear in the
            // TEControlTag enum
            for (TEControlTag tag : TEControlTag.values()) {
                LXListenableNormalizedParameter param = controlList.get(tag).control;
                if (missingControls != null) {
                    if (missingControls.missing_control_tags.contains(tag)){
                        markUnused(param);
                    }
                }

                // disable explode control if the pattern isn't a shader
                // (takes advantage of the fact that all shaders have a missingControls file entry,
                // even if they're not missing any controls.)
                // TODO - reminder to implement "explode" as an effect so all patterns can use it.
                else if (param.getLabel().equals(TEControlTag.EXPLODE.getLabel())) {
                    markUnused(param);
                }

                addParameter(tag.getPath(), param);
            }

            addParameter("panic", this.panic);
            addParameter("viewPerPattern", viewPerPattern);
            addParameter("swatchPerChannel", swatchParameter);
        }

        public void markUnused(LXNormalizedParameter param) {
            unusedParams.add(param);
        }

        /**
         * Included for consistency. We may need it later.
         */
        public void removeCommonControls() {
            for (TEControlTag tag : controlList.keySet()) {
                removeParameter(tag.getPath());
            }
            controlList.clear();
        }

        /**
         * Set remote controls in order they will appear on the midi surfaces
         */
        protected void setRemoteControls() {
            setCustomRemoteControls(new LXListenableNormalizedParameter[] {
                this.color.offset,
                this.color.gradient,
                getSwatchRemoteControl(),
                getControl(TEControlTag.SPEED).control,

                getControl(TEControlTag.XPOS).control,
                getControl(TEControlTag.YPOS).control,
                getControl(TEControlTag.QUANTITY).control,
                getControl(TEControlTag.SIZE).control,

                getControl(TEControlTag.ANGLE).control,
                getControl(TEControlTag.SPIN).control,
                this.panic,
                getViewRemoteControl(),

                getControl(TEControlTag.WOW1).control,
                getControl(TEControlTag.WOW2).control,
                getControl(TEControlTag.WOWTRIGGER).control,
                getControl(TEControlTag.EXPLODE).control
                // To be SHIFT, not implemented yet
            });
        }

        protected LXListenableNormalizedParameter getSwatchRemoteControl() {
            if (ColorCentral.ENABLED) {
                return swatchParameter;
            } else {
                return getControl(TEControlTag.BRIGHTNESS).control;
            }
        }

        protected LXListenableNormalizedParameter getViewRemoteControl() {
            if (ViewCentral.ENABLED) {
                return viewPerPattern;
            } else {
                return null;
            }
        }

        public void buildDefaultControlList() {
            LXListenableNormalizedParameter p;

            p = new CompoundParameter(TEControlTag.SPEED.getLabel(), 0.5, -4.0, 4.0)
                .setPolarity(LXParameter.Polarity.BIPOLAR)
                .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
                .setExponent(1.75)
                .setDescription("Speed");
            setControl(TEControlTag.SPEED, p);

            p = new CompoundParameter(TEControlTag.XPOS.getLabel(), 0, -1.0, 1.0)
                .setPolarity(LXParameter.Polarity.BIPOLAR)
                .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
                .setDescription("X Position");
            setControl(TEControlTag.XPOS, p);

            p = new CompoundParameter(TEControlTag.YPOS.getLabel(), 0, -1.0, 1.0)
                .setPolarity(LXParameter.Polarity.BIPOLAR)
                .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
                .setDescription("Y Position");
            setControl(TEControlTag.YPOS, p);

            p = new CompoundParameter(TEControlTag.SIZE.getLabel(), 1, 0.01, 5.0)
                .setDescription("Size");
            setControl(TEControlTag.SIZE, p);

            p = new CompoundParameter(TEControlTag.QUANTITY.getLabel(), 0.5, 0, 1.0)
                .setDescription("Quantity");
            setControl(TEControlTag.QUANTITY, p);

            p = (CompoundParameter)
                new CompoundParameter(TEControlTag.SPIN.getLabel(), 0, -1.0, 1.0)
                    .setPolarity(LXParameter.Polarity.BIPOLAR)
                    .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
                    .setExponent(2)
                    .setDescription("Spin");

            setControl(TEControlTag.SPIN, p);

            p = new CompoundParameter(TEControlTag.BRIGHTNESS.getLabel(), 1.0, 0.0, 1.0)
                .setDescription("Brightness");
            setControl(TEControlTag.BRIGHTNESS, p);

            p = new CompoundParameter(TEControlTag.WOW1.getLabel(), 0, 0, 1.0)
                .setDescription("Wow 1");
            setControl(TEControlTag.WOW1, p);

            p = new CompoundParameter(TEControlTag.WOW2.getLabel(), 0, 0, 1.0)
                .setDescription("Wow 2");
            setControl(TEControlTag.WOW2, p);

            p = new BooleanParameter(TEControlTag.WOWTRIGGER.getLabel(), false)
                .setMode(BooleanParameter.Mode.MOMENTARY)
                .setDescription("Trigger WoW effects");
            setControl(TEControlTag.WOWTRIGGER, p);

            p = new CompoundParameter("Explode", 0, 0, 1.0)
                .setDescription("Randomize the pixels to a certain radius on beat");
            setControl(TEControlTag.EXPLODE, p);

            // in degrees for display 'cause more people think about it that way
            p = (LXListenableNormalizedParameter)new TECommonAngleParameter(TEControlTag.ANGLE.getLabel(), 0, -Math.PI, Math.PI)
                .setDescription("Static Rotation Angle")
                .setPolarity(LXParameter.Polarity.BIPOLAR)
                .setWrappable(true)
                .setFormatter((v) -> {
                    return Double.toString(Math.toDegrees(v));
                });

            setControl(TEControlTag.ANGLE, p);
        }

        protected TEColorParameter registerColorControl(String prefix) {
            color = new TEColorParameter(prefix+"Color")
                .setDescription("TE Color");
            addParameter("te_color", color);
            return color;
        }

        /**
         * Sets current value for a common control
         *
         * @param tag - tag for control to set
         * @param val - the value to set
         */
        public TECommonControls setValue(TEControlTag tag, double val) {
            getLXControl(tag).setValue(val);
            return this;
        }

        TECommonControls() {
            // Create the user replaceable controls
            // derived classes must call addCommonControls() in their
            // constructor to add them to the UI.
            buildDefaultControlList();

            panic.addListener(panicListener);
        }

        /**
         * Called when the momentary PANIC knob is pressed.
         * Parameters can be reset here or just constrained
         * to a visible range.
         */
        protected void onPanic() {
            // For color, reset everything but Hue
            this.color.gradient.reset();
            this.color.blendMode.reset();
            this.color.solidSource.reset();
            this.color.offset.reset();
            this.color.color2offset.reset();
            this.color.saturation.setNormalized(1);
            this.color.brightness.setNormalized(1);

            getControl(TEControlTag.BRIGHTNESS).control.reset();
            getControl(TEControlTag.SPEED).control.reset();

            getControl(TEControlTag.XPOS).control.reset();
            getControl(TEControlTag.YPOS).control.reset();
            getControl(TEControlTag.QUANTITY).control.reset();
            getControl(TEControlTag.SIZE).control.reset();

            getControl(TEControlTag.ANGLE).control.reset();
            getControl(TEControlTag.SPIN).control.reset();

            getControl(TEControlTag.EXPLODE).control.reset();
            getControl(TEControlTag.WOW1).control.reset();
            getControl(TEControlTag.WOW2).control.reset();
            getControl(TEControlTag.WOWTRIGGER).control.reset();

            if (ColorCentral.ENABLED) {
                swatchParameter.reset();
            }

            if (ViewCentral.ENABLED) {
                viewPerPattern.reset();
            }
        }

        public void dispose() {
          panic.removeListener(panicListener);
        }
    }

    /**
     * Class to support incremental rotation over variable-speed time
     *
     * The rate is tied to the engine bpm and the input time value, which is usually
     * controlled by the variable speed timer associated with the speed or spin controls.
     * (but anything with a seconds.millis timer can generate rotational angles this way.)
     */
    protected class Rotor {
        private double maxSpinRate = Math.PI;
        private double angle = 0;
        private double lastTime = 0;

        // Internal: Called on every frame to calculate and memoize the current
        // spin angle so that calls to getAngle() during a frame
        // will always return the same value no matter how long the frame
        // calculations take.
        void updateAngle(double time, double ctlValue) {
            // if this is the first frame, or if the timer was restarted,
            // we skip calculation for a frame.  Otherwise, do
            // the incremental angle calculation...
            if (lastTime != 0) {
                // calculate change in angle since last frame.
                // Note: revised calculation restricts maximum speed while still allowing
                // you to get to maximum speed at slower bpm.
                double et = Math.min(maxSpinRate, maxSpinRate * (time - lastTime));
                angle -= et % LX.TWO_PI;
            }
            lastTime = time;
        }

        /**
         * @return Current rotational angle, either computed, or taken from
         */
        double getAngle() {
            return angle;
        }

        void setAngle(double angle) {
            this.angle = angle;
        }

        void addAngle(double offset) {
            this.angle += offset;
        }

        void reset() {
            angle = 0;
            lastTime = 0;
        }

        /**
         * Sets maximum spin rate for all patterns using this rotor.  Note that a Rotor
         * object is associated with a timer, which can be a VariableSpeedTimer.  So
         * "seconds" may be variable in duration, and can be positive or negative.
         *
         * @param radiansPerSecond
         */
        void setMaxSpinRate(double radiansPerSecond) {
            maxSpinRate = radiansPerSecond;
        }

    }

    private final VariableSpeedTimer iTime = new VariableSpeedTimer();
    private final VariableSpeedTimer spinTimer = new VariableSpeedTimer();
    private final Rotor speedRotor = new Rotor();
    private final Rotor spinRotor = new Rotor();

    protected TECommonControls controls;

    protected FloatBuffer palette = Buffers.newDirectFloatBuffer(15);

    protected TEPerformancePattern(LX lx) {
        this(lx, null);
    }

    protected TEPerformancePattern(LX lx, TEShaderView defaultView) {
        super(lx);
        controls = new TECommonControls();

        this.defaultView = defaultView;

        this.model = super.model;   // That's right!
        this.viewPerPattern = ViewCentral.get().createParameter();
        this.viewPerPattern.addListener(viewPerPatternListener);
        this.viewPerPattern.listenDispose(viewPerPatternDisposing);
        this.viewPerPattern.setDefault(getDefaultView(), true);

        lx.engine.addTask(() -> {
            if (this.controls.color == null) {
                // Instantiation failed. Turn off now to avoid fatal call to null variables.
                return;
            }

            // Patterns are created, then added to channel. Channel should be available on next engine loop.
            linkChannelParameters(lx);

            // Because LXPattern child classes can not override defaultRemoteControls and have to use
            // customRemoteControls, that means a list of our remote controls are saved to file and
            // then on file open the pattern is loaded and THEN LXDeviceComponent.load() restores a
            // list of the old remote controls thinking they're user-custom.  In our case to prevent
            // needing to recreate .lxps every time a pattern parameter is changed, we'll just refresh
            // the remote controls AGAIN after LXDeviceComponent restored the old ones AFTER the
            // pattern loaded from file.  This means runtime user-customized remote controls will not
            // survive a file save/load which is much less inconvenient than this default behavior.
            this.controls.setRemoteControls();
        });
    }

    private void linkChannelParameters(LX lx) {
        // Finally safe to assume a channel has been assigned
        this.controls.viewParameter.link();
    }

    public TECommonControls getControls() {
        return controls;
    }

    public void addCommonControls() {
        this.controls.addCommonControls(this);
        this.controls.setRemoteControls();

        this.controls.getLXControl(TEControlTag.WOWTRIGGER).addListener(wowTriggerListener);
    }

    public FloatBuffer getCurrentPalette() {
        float r, g, b;

        if (palette != null) {
            palette.rewind();
            for (int i = 0; i < 5; i++) {

                int color = getLX().engine.palette.swatch.getColor(i).getColor();

                r = (float) (0xff & LXColor.red(color)) / 255f;
                palette.put(r);
                g = (float) (0xff & LXColor.green(color)) / 255f;
                palette.put(g);
                b = (float) (0xff & LXColor.blue(color)) / 255f;
                palette.put(b);
            }
            palette.rewind();
        }
        return palette;
    }

    /**
     * @return Returns a loosely beat-linked rotation angle in radians.  Overall speed
     * is determined by the "Speed" control, but will automatically speed up and slow down
     * as the LX engine's beat speed changes. The "Angle" controls sets an additional
     * angular offset.
     */
    public double getRotationAngleFromSpeed() {
        // Loosely beat linked speed.  What this thinks it's doing is moving at one complete rotation
        // per beat, based on the elapsed time and the engine's bpm rate.
        // But since we're using variable time, we can speed it up and slow it down smoothly by adjusting
        // the speed of time, and still have keep its speed in sync with the beat.
        return -(speedRotor.getAngle() - getStaticRotationAngle());
    }

    /**
     * @return Returns a loosely beat-linked rotation angle in radians.  Overall speed
     * is determined by the "Spin" control, but will automatically speed up and slow down
     * as the LX engine's beat speed changes. The "Angle" controls sets an additional
     * angular offset.
     */
    public double getRotationAngleFromSpin() {
        // See comments in getRotationAngleFromSpeed() above.
        return spinRotor.getAngle() - getStaticRotationAngle();
    }

    public double getStaticRotationAngle() {
        double t = controls.getValue(TEControlTag.ANGLE);
        return (t >= 0) ? t : t + LX.TWO_PI;
    }

    /**
     * Sets the maximum rotation speed used by both getRotationAngleFromSpin() and
     * getRotationAngleFromSpeed().
     *
     * The default maximum radians per second is PI, which gives one complete rotation
     * every two beats at the current engine BPM.  Do not change this value unless you
     * have a specific reason for doing so.  Too high a rotation speed can cause visuals
     * to become erratic.
     *
     * @param radiansPerSecond
     */
    public void setMaxRotationSpeed(double radiansPerSecond) {

    }


    /*
     * Color safety mechanism: only calculate solid colors once per frame.
     * Child classes are still encouraged to only call the color methods
     * once but this will reduce impact of uncaught cases.
     */
    private boolean isStaleColor = true;
    private boolean isStaleColor2 = true;
    private boolean isStaleColorBase = true;
    private int _getColor, _calcColor, _calcColor2;

    protected void expireColors() {
        isStaleColorBase = isStaleColor = isStaleColor2 = true;
    }

    /**
     * @return Color derived from the current setting of the color and brightness controls
     *
     * NOTE:  The design philosophy here is that palette colors (and the color control)
     * have precedence.
     *
     * Brightness modifies the current color, and is set to 1.0 (100%) by default. So
     * if you don't move the brightness control you get *exactly* the currently
     * selected color.
     *
     * At present, the brightness control lets you dim the current color,
     * but if you want to brighten it, you have to do that with the channel fader or
     * the color control.
     */
    public int calcColor() {
        if (isStaleColor) {
            _calcColor = TEColor.setBrightness(controls.color.calcColor(), (float) getBrightness());
            isStaleColor = false;
        }
        return _calcColor;
    }

    /**
     * ** Instead of these two methods, use getGradientColor(lerp) which defers to TEColorParameter for gradient selection. **
     *
     * Suppress parent TEPattern gradient methods, force child classes
     * to choose solid color or gradient, keeping other choices
     * runtime-adjustable.
     *
     * TODO: remove these two methods from TEPattern to prevent confusion?
     */
    @Deprecated
    @Override
    public int getPrimaryGradientColor(float lerp) {
        LX.error("Use getGradientColor() instead");
        return TEColor.setBrightness(super.getPrimaryGradientColor(lerp), (float) getBrightness());
    }

    @Deprecated
    @Override
    public int getSecondaryGradientColor(float lerp) {
        LX.error("Use getGradientColor() instead");
        return TEColor.setBrightness(super.getSecondaryGradientColor(lerp), (float) getBrightness());
    }

    /**
     * For patterns that consume two solid colors, use this method
     * to retrieve the 2nd color.
     * Returns a color offset in position from the first color.
     *
     * @return
     */
    public int calcColor2() {
        if (isStaleColor2) {
            _calcColor2 = TEColor.setBrightness(controls.color.calcColor2(), (float) getBrightness());
            isStaleColor2 = false;
        }
        return _calcColor2;
    }

    /**
     * Gets the current color as set in the color control, without adjusting
     * for brightness.  This is used by the OpenGL renderer, which has
     * a unified mechanism for handling brightness.
     */
    public int getColor() {
        if (isStaleColorBase) {
            _getColor = controls.color.calcColor();
            isStaleColorBase = false;
        }
        return _getColor;
    }

    public int getGradientColor(float lerp) {
        return TEColor.setBrightness(controls.color.getGradientColor(lerp),(float) getBrightness());
    }

    /**
     * @return Current variable time in seconds.  Note that time
     * can run both forward and backward, so the returned value can be negative.
     */
    public double getTime() {
        return iTime.getTime();
    }

    /**
     * @return Current variable time in milliseconds.  Note that time
     * can run both forward and backward, so the returned value can be negative.
     */
    public double getTimeMs() {
        return iTime.getTimeMs();
    }

    /**
     * Controls whether time is allowed to run both forward and backward,
     * according to the sign of the current scale value.  Bidirectional
     * time is allowed (true) by default.
     * @param val true for bidirectional time, false for forward-moving time only,
     * regardless of time scale setting.
     */
    public void allowBidirectionalTime(boolean val) {
        iTime.allowBidirectionalTime(val);
    }

    /**
     * @return current variable time in milliseconds since last call to this timer's
     * Tick() function (normally called automatically at the start of each frame.)
     * Note that time can run both forward and backward, so the returned value can be negative.
     */
    public double getDeltaMs() {
        return iTime.getDeltaMs();
    }

    public double getSpeed() {
        return controls.getValue(TEControlTag.SPEED);
    }

    public double getXPos() {
        return controls.getValue(TEControlTag.XPOS);
    }

    public double getYPos() {
        return controls.getValue(TEControlTag.YPOS);
    }

    public double getSize() {
        return controls.getValue(TEControlTag.SIZE);
    }

    public double getQuantity() {
        return controls.getValue(TEControlTag.QUANTITY);
    }

    /**
     * For most uses, getRotationAngle() is recommended, but if you
     * need direct access to the spin control value, here it is.
     */
    public double getSpin() {
        return controls.getValue(TEControlTag.SPIN);
    }

    /**
     * <b>NOTE:</b> This control has functional overlap with color and channel fader settings, and
     * could potentially cause confusing brightness behavior.
     *
     * <b>It may be deprecated or removed in the future and should not be used in patterns.</b>
     *
     * @return The current value of the brightness control, by default in the range 0.0 to 1.0
     */
    public double getBrightness() {
        return controls.getValue(TEControlTag.BRIGHTNESS);
    }

    public double getWow1() {
        return controls.getValue(TEControlTag.WOW1);
    }

    public double getWow2() {
        return controls.getValue(TEControlTag.WOW2);
    }

    public boolean getWowTrigger() {
        return controls.getValue(TEControlTag.WOWTRIGGER) > 0.0;
    }

    public double getExplode() {
        return controls.getValue(TEControlTag.EXPLODE);
    }

    /**
     * Restarts the specified timer's elapsed time when called.
     * The timer's rate is not changed.
     *
     * This is useful for syncing a timer precisely to beats,
     * measures and other external events.
     *
     * @param tag - the tag of the control to be retriggered.  Only
     *            works timer-linked controls - SPEED and SPIN at
     *            present.
     */
    public void retrigger(TEControlTag tag) {
        switch (tag) {
            case SPEED:
                iTime.reset();
                break;
            case SPIN:
                spinTimer.reset();
                break;
            default:
                //TE.log("retrigger: Invalid parameter.");
                break;
        }
    }

    private final LXParameterListener wowTriggerListener = (p) -> {
        onWowTrigger(getWowTrigger());
    };

    /**
     * Subclasses can override
     */
    protected void onWowTrigger(boolean on) {  }

    @Override
    protected void run(double deltaMs) {
        // get the current tempo in beats per second
        double bps = lx.engine.tempo.bpm() / 60;

        // Spin control
        double value = getSpin() * bps;
        spinTimer.setScale(value);
        spinTimer.tick();
        spinRotor.updateAngle(spinTimer.getTime(), value);

        // Speed control
        // To calculate timescale for speed, we multiply the control value
        // by the engine bpm (converted to beats/sec).  This makes the core
        // speed clock rate 1 virtual second per beat - quarter note pace in
        // 4/4 time signature, which is then modified by the Speed UI control.
        //
        // This makes tempo syncing in patterns a LOT easier, with no extra parameters,
        // controls or efforts.  You can set speed for specific time divisions as follows:
        // speed = 0.25 is whole notes
        // speed = 0.5 is half notes
        // speed = 1 is quarter notes
        // speed = 2 is eighth notes
        // speed = 3 is eighth note triplets
        // speed = 4 (the default maximum) is 16th notes
        //
        // If you need to go faster than 16ths in a pattern, expand the range with setRange()
        // in your constructor.  Of course, other speeds work too, and can create
        // interesting syncopated visuals.
        value = getSpeed() * bps;
        iTime.setScale(value);
        iTime.tick();
        speedRotor.updateAngle(iTime.getTime(), value);

        // Gradients always need to be up to date for TEColorParameter
        updateGradients();
        expireColors();

        super.run(deltaMs);
    }

    @Override
    public void dispose() {
        if (this.viewPerPattern != null) {
            this.viewPerPattern.removeListener(viewPerPatternListener);
            this.viewPerPattern.unlistenDispose(viewPerPatternDisposing);
        }
        this.controls.getLXControl(TEControlTag.WOWTRIGGER).removeListener(wowTriggerListener);
        this.controls.dispose();
        super.dispose();
    }
}
