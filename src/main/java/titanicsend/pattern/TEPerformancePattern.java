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
            if (controlList.get(tag) != null) {
                removeParameter(tag.getPath());
            }
            controlList.put(tag, newControl);
            addParameter(tag.getPath(), lxp);
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

            setControl(tag, newControl, defaultGetFn);
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

        public void registerCommonControls() {
            for (TEControlTag tag : controlList.keySet()) {
                addParameter(tag.getPath(), controlList.get(tag).control);
            }
        }

        public void unregisterCommonControls() {
            for (TEControlTag tag : controlList.keySet()) {
                removeParameter(tag.getPath());
            }
            controlList.clear();
        }

        public void buildDefaultControlList() {
            LXListenableParameter p;

            p = new CompoundParameter("Speed", 0.1, -1.0, 1.0)
                    .setPolarity(LXParameter.Polarity.BIPOLAR)
                    .setExponent(2.0)
                    .setDescription("Speed");
            setControl(TEControlTag.SPEED, p, defaultGetFn);

            p = new CompoundParameter("xPos", 0, -1.0, 1.0)
                    .setPolarity(LXParameter.Polarity.BIPOLAR)
                    .setDescription("X Position");
            setControl(TEControlTag.XPOS, p, defaultGetFn);

            p = new CompoundParameter("yPos", 0, -1.0, 1.0)
                    .setPolarity(LXParameter.Polarity.BIPOLAR)
                    .setDescription("Y Position");
            setControl(TEControlTag.YPOS, p, defaultGetFn);

            p = new CompoundParameter("Size", 1, 0.01, 5.0)
                    .setDescription("Size");
            setControl(TEControlTag.SIZE, p, defaultGetFn);

            p = new CompoundParameter("Quantity", 0.5, 0, 1.0)
                    .setDescription("Quantity");
            setControl(TEControlTag.QUANTITY, p, defaultGetFn);

            p = (CompoundParameter)
                    new CompoundParameter("Spin", 0, -1.0, 1.0)
                            .setPolarity(LXParameter.Polarity.BIPOLAR)
                            .setExponent(2)
                            .setDescription("Spin");
            setControl(TEControlTag.SPIN, p, defaultGetFn);

            p = new CompoundParameter("Brightness", 1.0, 0.0, 1.0)
                    .setDescription("Brightness");
            setControl(TEControlTag.BRIGHTNESS, p, defaultGetFn);

            p = new CompoundParameter("Wow1", 0, 0, 1.0)
                    .setDescription("Wow 1");
            setControl(TEControlTag.WOW1, p, defaultGetFn);

            p = new CompoundParameter("Wow2", 0, 0, 1.0)
                    .setDescription("Wow 2");
            setControl(TEControlTag.WOW2, p, defaultGetFn);

            p = new BooleanParameter("WowTrigger", false)
                    .setMode(BooleanParameter.Mode.MOMENTARY)
                    .setDescription("Trigger WoW effects");
            setControl(TEControlTag.WOWTRIGGER, p, defaultGetFn);
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

        TECommonControls(boolean setupColor) {
            // Some derived classes, notably ConstructedPattern, will crash if we
            // don't defer registering the color control until the pattern
            // hierarchy is fully created. TECommonControls(false) allows us
            // to put this off in this constructor.  The derived class can
            // then call registerColorControl() when ready.
            if (setupColor) {
                registerColorControl();
            }

            // Add the user replaceable controls
            buildDefaultControlList();
            //registerCommonControls();
        }
    }

    protected double maxRotationsPerSecond;
    protected double maxRotationsPerBeat;
    protected double maxTimeMultiplier;
    public VariableSpeedTimer iTime;

    protected VariableSpeedTimer spinTimer;
    protected TECommonControls controls;

    FloatBuffer palette = Buffers.newDirectFloatBuffer(15);

    protected TEPerformancePattern(LX lx, boolean doColorSetup) {
        super(lx);
        maxRotationsPerSecond = 4.0;
        maxRotationsPerBeat = 4.0;
        maxTimeMultiplier = 8.0;

        iTime = new VariableSpeedTimer();
        spinTimer = new VariableSpeedTimer();
        controls = new TECommonControls(doColorSetup);
        ;
    }

    protected TEPerformancePattern(LX lx) {
        this(lx, true);
    }

    public void registerColorControl() {
        this.controls.registerColorControl();
    }

    public FloatBuffer getCurrentPalette() {
        int col;
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

    public void setMaxRotationsPerSecond(double maxRotationsPerSecond) {
        this.maxRotationsPerSecond = maxRotationsPerSecond;
    }

    public void setMaxRotationsPerBeat(double maxRotationsPerBeat) {
        this.maxRotationsPerBeat = maxRotationsPerBeat;
    }

    public void setMaxTimeMultiplier(double m) {
        this.maxTimeMultiplier = m;
    }

    /**
     * @return Returns the current rotation angle in radians, derived from a real-time LFO, the setting
     * of the "spin" control, and the constant MAX_ROTATIONS_PER_SECOND. If current spin rate is 0,
     * returned angle will also be zero, to allow easy reset of patterns.
     */
    public double getRotationAngle() {
        return (getSpin() != 0) ? LX.TWO_PI * (spinTimer.getTime() % 1) : 0;
    }

    /**
     * @return Returns the current rotation angle in radians, derived from the sawtooth wave provided
     * by getTempo().basis(), the setting of the "spin" control, and a preset maximum rotations
     * per beat. If current spin rate is 0, returned angle will also be zero, to allow easy reset of patterns.
     */
    public double getRotationAngleOverBeat() {
        return (getSpin() != 0) ?
                LX.TWO_PI * this.getTempo().basis() * (controls.getValue(TEControlTag.SPIN) * maxRotationsPerBeat) :
                0;
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
     * need direct acces to the spin control value, here it is.
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

        iTime.setScale(getSpeed() * maxTimeMultiplier);
        iTime.tick();
        spinTimer.setScale(getSpin() * maxRotationsPerSecond);
        spinTimer.tick();

        super.run(deltaMs);
    }
}
