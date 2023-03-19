package titanicsend.pattern;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.*;
import titanicsend.pattern.jon.TEControl;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.pattern.jon._CommonControlGetter;

import java.nio.FloatBuffer;
import java.util.HashMap;

public abstract class TEPerformancePattern extends TEAudioPattern {

    public class TECommonControls {

        // color control is accessible, in case the pattern needs something
        // other than the current color.
        public LinkedColorParameter color;


        _CommonControlGetter defaultGetFn = new _CommonControlGetter() {
            @Override
            public double getValue(TEControl cc) {
                return cc.getValue();
            }
        };

        private HashMap<TEControlTag, TEControl> controlList = new HashMap<TEControlTag, TEControl>();

        /**
         * Retrieve control object for given tag
         *
         * @param tag
         */
        public LXListenableParameter getLXControl(TEControlTag tag) {
            return controlList.get(tag).control;
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

        public TECommonControls setControl(TEControlTag tag, LXListenableParameter lxp, _CommonControlGetter getFn) {
            TEControl newControl = new TEControl(lxp, getFn);
            controlList.put(tag, newControl);
            return this;
        }

        public TECommonControls setControl(TEControlTag tag, LXListenableParameter lxp) {
            TEControl newControl = new TEControl(lxp, defaultGetFn);
            controlList.put(tag, newControl);
            return this;
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

        public TECommonControls setRange(TEControlTag tag, double value, double v0, double v1) {
            // copy data from previous tag
            CompoundParameter oldControl = (CompoundParameter) getLXControl(tag);
            CompoundParameter newControl = (CompoundParameter) new CompoundParameter(oldControl.getLabel(), value, v0, v1)
                    .setDescription(oldControl.getDescription())
                    .setPolarity(oldControl.getPolarity())
                    .setExponent(oldControl.getExponent())
                    .setUnits(oldControl.getUnits());

            setControl(tag, newControl);
            return this;
        }

        public TECommonControls setExponent(TEControlTag tag, double exp) {
            CompoundParameter p = (CompoundParameter) getLXControl(tag);
            p.setExponent(exp);
            return this;
        }

        public TECommonControls setNormalizationCurve(TEControlTag tag, BoundedParameter.NormalizationCurve curve) {
            CompoundParameter p = (CompoundParameter) getLXControl(tag);
            p.setNormalizationCurve(curve);
            return this;
        }

        public TECommonControls setUnits(TEControlTag tag, LXParameter.Units units) {
            CompoundParameter p = (CompoundParameter) getLXControl(tag);
            p.setUnits(units);
            return this;
        }

        /**
         * To use the common controls, call this function from the constructor
         * of TEPerformancePattern-derived classes after configuring the default
         * controls for your pattern.
         *
         * If your pattern adds its own controls in addition to the common
         * controls, you must call addParameter() for them after callling
         * this function so the UI stays consistent across pattterns.
         */
        public void addCommonControls() {
            registerColorControl();

            // controls will be added in the order their tags appear in the
            // TEControlTag enum
            for (TEControlTag tag : TEControlTag.values()) {
                addParameter(tag.getPath(), controlList.get(tag).control);
            }
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

        public void buildDefaultControlList() {
            LXListenableParameter p;

            p = new CompoundParameter("Speed", 0.1, -1.0, 1.0)
                    .setPolarity(LXParameter.Polarity.BIPOLAR)
                    .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
                    .setExponent(2.0)
                    .setDescription("Speed");
            setControl(TEControlTag.SPEED, p);

            p = new CompoundParameter("xPos", 0, -1.0, 1.0)
                    .setPolarity(LXParameter.Polarity.BIPOLAR)
                    .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
                    .setDescription("X Position");
            setControl(TEControlTag.XPOS, p);

            p = new CompoundParameter("yPos", 0, -1.0, 1.0)
                    .setPolarity(LXParameter.Polarity.BIPOLAR)
                    .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
                    .setDescription("Y Position");
            setControl(TEControlTag.YPOS, p);

            p = new CompoundParameter("Size", 1, 0.01, 5.0)
                    .setDescription("Size");
            setControl(TEControlTag.SIZE, p);

            p = new CompoundParameter("Quantity", 0.5, 0, 1.0)
                    .setDescription("Quantity");
            setControl(TEControlTag.QUANTITY, p);

            p = (CompoundParameter)
                    new CompoundParameter("Spin", 0, -1.0, 1.0)
                            .setPolarity(LXParameter.Polarity.BIPOLAR)
                            .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
                            .setExponent(2)
                            .setDescription("Spin");
            setControl(TEControlTag.SPIN, p);

            p = new CompoundParameter("Brightness", 1.0, 0.0, 1.0)
                    .setDescription("Brightness");
            setControl(TEControlTag.BRIGHTNESS, p);

            p = new CompoundParameter("Wow1", 0, 0, 1.0)
                    .setDescription("Wow 1");
            setControl(TEControlTag.WOW1, p);

            p = new CompoundParameter("Wow2", 0, 0, 1.0)
                    .setDescription("Wow 2");
            setControl(TEControlTag.WOW2, p);

            p = new BooleanParameter("WowTrigger", false)
                    .setMode(BooleanParameter.Mode.MOMENTARY)
                    .setDescription("Trigger WoW effects");
            setControl(TEControlTag.WOWTRIGGER, p);
        }

        protected void registerColorControl() {
            color = registerColor("Color", "te_color", ColorType.PRIMARY,
                    "Panel Color");
        }

        /**
         * Sets current value for a common control
         *
         * @param tag
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
        }
    }
    protected double timeMultiplier;

    public VariableSpeedTimer iTime;

    protected VariableSpeedTimer spinTimer;
    protected TECommonControls controls;

    FloatBuffer palette = Buffers.newDirectFloatBuffer(15);

    protected TEPerformancePattern(LX lx, boolean doColorSetup) {
        super(lx);
        timeMultiplier = 4.0;

        iTime = new VariableSpeedTimer();
        spinTimer = new VariableSpeedTimer();
        controls = new TECommonControls();
    }

    protected TEPerformancePattern(LX lx) {
        this(lx, true);
    }

    public void addCommonControls() { this.controls.addCommonControls(); }

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

    public void setTimeMultiplier(double m) {
        this.timeMultiplier = m;
    }

    /**
     * @return Returns a loosely beat-linked rotation angle in radians.  Overall speed
     * is determined by the "Speed" control, but will automatically speed up and slow down
     * as the LX engine's beat speed changes.
     * If current speed is zero, returned angle will also be zero, to allow easy reset of patterns.
     */
    public double getRotationAngleFromSpeed() {
        // Loosely beat linked speed.  What this thinks it's doing is moving at one complete rotation
        // per beat, based on the elapsed time and the engine's bpm rate.
        // But since we're using variable time, we can speed it up and slow it down smoothly, and still
        // have it moving more-or-less in sync with the beat.
        return (getSpeed() != 0) ? (LX.TWO_PI*(lx.engine.tempo.bpm()/60) * iTime.getTime()) % LX.TWO_PI : 0;
    }

    /**
     * @return Returns a loosely beat-linked rotation angle in radians.  Overall speed
     * is determined by the "Spin" control, but will automatically speed up and slow down
     * as the LX engine's beat speed changes.
     * If current speed is zero, returned angle will also be zero, to allow easy reset of patterns.
     */
    public double getRotationAngleFromSpin() {
        // See comments in getRotationAngleFromSpeed() above.
        return (getSpin() != 0) ? (LX.TWO_PI*(lx.engine.tempo.bpm()/60) * spinTimer.getTime()) % LX.TWO_PI : 0;
    }

    public int getCurrentColor() {
        return controls.color.calcColor();
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

    protected void run(double deltaMs) {
        spinTimer.setScale(getSpin());
        spinTimer.tick();
        iTime.setScale(getSpeed() * timeMultiplier);
        iTime.tick();


        super.run(deltaMs);
    }
}
