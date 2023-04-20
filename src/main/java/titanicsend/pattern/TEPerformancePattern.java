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
import titanicsend.util.TEColor;

import java.nio.FloatBuffer;
import java.util.HashMap;

public abstract class TEPerformancePattern extends TEAudioPattern {

    // Create new class for Angle control so we can override the reset
    // behavior and have reset set the current composite rotation angle
    // to the spin control's current setting.
    class TECommonAngleParameter extends CompoundParameter {

        public TECommonAngleParameter(String label, double value, double v0, double v1) {
            super(label, value, v0, v1);
        }

        @Override
        public LXParameter reset() {
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

        // color control is accessible, in case the pattern needs something
        // other than the current color.
        public LinkedColorParameter color;


        _CommonControlGetter defaultGetFn = new _CommonControlGetter() {
            @Override
            public double getValue(TEControl cc) {
                return cc.getValue();
            }
        };


        private final HashMap<TEControlTag, TEControl> controlList = new HashMap<TEControlTag, TEControl>();

        /**
         * Retrieve backing LX control object for given tag
         *
         * @param tag
         */
        public LXListenableParameter getLXControl(TEControlTag tag) {
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

        public TECommonControls setControl(TEControlTag tag, LXListenableParameter lxp, _CommonControlGetter getFn) {
            TEControl newControl = new TEControl(lxp, getFn);
            controlList.put(tag, newControl);
            return this;
        }

        public TECommonControls setControl(TEControlTag tag, LXListenableParameter lxp) {
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

        public TECommonControls setRange(TEControlTag tag, double value, double v0, double v1) {
            // copy data from previous tag
            CompoundParameter oldControl = (CompoundParameter) getLXControl(tag);
            CompoundParameter newControl = (CompoundParameter) new CompoundParameter(oldControl.getLabel(), value, v0, v1)
                    .setDescription(oldControl.getDescription())
                    .setNormalizationCurve(oldControl.getNormalizationCurve())
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
         * <p>
         * If your pattern adds its own controls in addition to the common
         * controls, you must call addParameter() for them after calling
         * this function so the UI stays consistent across patterns.
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

            p = new CompoundParameter("Speed", 0.5, -4.0, 4.0)
                    .setPolarity(LXParameter.Polarity.BIPOLAR)
                    .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
                    .setExponent(1.75)
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

            // in degrees for display 'cause more people think about it that way
            p = new TECommonAngleParameter("Angle", 0, -Math.PI, Math.PI)
                    .setDescription("Static Rotation Angle")
                    .setPolarity(LXParameter.Polarity.BIPOLAR)
                    .setWrappable(true)
                    .setFormatter((v) -> {
                        return Double.toString(Math.toDegrees(v));
                    });

            setControl(TEControlTag.ANGLE, p);
        }

        protected void registerColorControl() {
            color = registerColor("Color", "te_color", ColorType.PRIMARY,
                    "Panel Color");
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

        }
    }

    /**
     * Class to support incremental rotation over variable-speed time
     * <p>
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
                angle += et % LX.TWO_PI;
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
        super(lx);
        controls = new TECommonControls();
    }

    public void addCommonControls() {
        this.controls.addCommonControls();
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
        return speedRotor.getAngle() - getStaticRotationAngle();
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
        return controls.getValue(TEControlTag.ANGLE);
    }

    /**
     * Sets the maximum rotation speed used by both getRotationAngleFromSpin() and
     * getRotationAngleFromSpeed().
     * <p></p>
     * The default maximum radians per second is PI, which gives one complete rotation
     * every two beats at the current engine BPM.  Do not change this value unless you
     * have a specific reason for doing so.  Too high a rotation speed can cause visuals
     * to become erratic.
     * @param radiansPerSecond
     */
    public void setMaxRotationSpeed(double radiansPerSecond) {

    }


    /**
     * @return Color derived from the current setting of the color and brightness controls
     * <p></p>
     * NOTE:  The design philosophy here is that palette colors (and the color control)
     * have precedence.
     * <p></p>
     * Brightness modifies the current color, and is set to 1.0 (100%) by default. So
     * if you don't move the brightness control you get *exactly* the currently
     * selected color.
     * <p></p>
     * At present, the brightness control lets you dim the current color,
     * but if you want to brighten it, you have to do that with the channel fader or
     * the color control.
     */
    public int getCurrentColor() {
        return TEColor.setBrightness(controls.color.calcColor(), (float) getBrightness());
    }

    @Override
    public int getPrimaryGradientColor(float lerp) {
        return TEColor.setBrightness(super.getPrimaryGradientColor(lerp), (float) getBrightness());
    }

    @Override
    public int getSecondaryGradientColor(float lerp) {
        return TEColor.setBrightness(super.getSecondaryGradientColor(lerp), (float) getBrightness());
    }

    /**
     * Gets the current color as set in the color control, without adjusting
     * for brightness.  This is used by the OpenGL renderer, which has
     * a unified mechanism for handling brightness.
     */
    public int getCurrentColorControlValue() {
        return controls.color.calcColor();
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
     * <p></p>
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

    /**
     * Restarts the specified timer's elapsed time when called.
     * The timer's rate is not changed.
     * <p>
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
        speedRotor.updateAngle(iTime.getTime(), value );

        super.run(deltaMs);
    }
}
