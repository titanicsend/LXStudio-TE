package titanicsend.pattern;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.*;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.jon.VariableSpeedTimer;

import java.nio.FloatBuffer;
import java.util.HashMap;

public abstract class TEPerformancePattern extends TEAudioPattern {

    public class TECommonControls {

        // accessible control objects for each standard control
        public LinkedColorParameter color;

        _CommonControlGetter defaultGetFn = new _CommonControlGetter() {
            @Override
            public double getValue(TEControl cc) {
                return cc.getValue();
            }
        };

        public interface _CommonControlGetter {
            double getValue(TEControl cc);
        }

        public class TEControl {
            TEControl(LXListenableParameter ctl, _CommonControlGetter getFn) {
                this.control = ctl;
                this.getFn = getFn;
            }

            LXListenableParameter control;
            _CommonControlGetter getFn;

            public double getValue() {
                return control.getValue();
            }

        }

        public HashMap<TEControlTag, TEControl> controlList = new HashMap<TEControlTag, TEControl>();

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
        double getValue(TEControlTag tag) {
            TEControl ctl = controlList.get(tag);
            return ctl.getFn.getValue(ctl);
        }

        public void setControl(TEControlTag tag, LXListenableParameter ctl, _CommonControlGetter getFn) {
            TEControl newControl = new TEControl(ctl, getFn);
            if (controlList.get(tag) != null) {
                removeParameter(tag.getPath());
            }
            controlList.put(tag, newControl);
            addParameter(tag.getPath(), ctl);
        }

        /**
         * Sets a new getter function (an object implementing the _CommonControlGetter
         * interface) for specified tag's control.
         *
         * @param tag
         * @param getFn
         */
        public void setGetterFunction(TEControlTag tag, _CommonControlGetter getFn) {
            controlList.get(tag).getFn = getFn;
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
                    .setExponent(3.0)
                    .setDescription("Speed");
            setControl(TEControlTag.SPEED, p, defaultGetFn);

            p = new CompoundParameter("xPos1", 0, -1.0, 1.0)
                    .setPolarity(LXParameter.Polarity.BIPOLAR)
                    .setDescription("X Position");
            setControl(TEControlTag.XPOS, p, defaultGetFn);


            p = new CompoundParameter("yPos1", 0, -1.0, 1.0)
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
                            .setExponent(3)
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

        void registerColorControl() {
            color = registerColor("Color", "te_color", ColorType.PRIMARY,
                    "Panel Color");
        }

        /**
         * Sets current value for a common control
         *
         * @param tag
         * @param val - the value to set
         */
        public void setValue(TEControlTag tag, double val) {
            getLXControl(tag).setValue(val);
        }

        TECommonControls(boolean setupColor) {
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
        return controls.getValue(TEControlTag.WOWTRIGGER) > 0.5f;
    }

    protected void run(double deltaMs) {

        iTime.setScale(getSpeed() * maxTimeMultiplier);
        iTime.tick();
        spinTimer.setScale(getSpin() * maxRotationsPerSecond);
        spinTimer.tick();

        super.run(deltaMs);
    }
}
