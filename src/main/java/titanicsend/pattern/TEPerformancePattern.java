package titanicsend.pattern;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.util.TEMath;

import java.nio.FloatBuffer;
import java.util.HashMap;

public abstract class TEPerformancePattern extends TEAudioPattern {

    public class TECommonControls {

        // accessible control objects for each standard control
        public LinkedColorParameter color;

        _CommonControlGetter defaultGetFn = new _CommonControlGetter() {
            @Override public float getValue(LXParameter ctl) { return ctl.getValuef(); }
        }
;

        public interface _CommonControlGetter {
            float getValue(LXParameter ctl);
        }

        protected class _CommonControl {
            _CommonControl(LXParameter ctl, _CommonControlGetter getFn) {
                this.control = ctl;
                this.get = getFn;
            }
            LXParameter control;
            _CommonControlGetter get;
        }

        public HashMap<TEControlTag, _CommonControl> controlList = new HashMap<TEControlTag, _CommonControl>();

        /**
         * Retrieve control object for given tag
         * @param tag
         */
        public LXParameter getControl(TEControlTag tag) {
            return controlList.get(tag).control;
        }

        /**
         * Get current value of control specified by tag by calling
         * the tag's configured getter function (and NOT by directly calling
         * the control's getValue() function)
         *
         * @param tag
         */
        float getValue(TEControlTag tag) {
            _CommonControl ctl = controlList.get(tag);
            return ctl.get.getValue(ctl.control);
        }

        public void setControl(TEControlTag tag,LXParameter ctl,_CommonControlGetter getFn) {
            _CommonControl newControl = new _CommonControl(ctl,getFn);
            if (controlList.get(tag) != null) {
                removeParameter(tag.getPath());
            }
            controlList.put(tag,newControl);
            addParameter(tag.getPath(), ctl);
        }

        /**
         * Sets a new getter function (an object implementing the _CommonControlGetter
         * interface) for specified tag's control.
         * @param tag
         * @param getFn
         */
        void setGetterFunction(TEControlTag tag, _CommonControlGetter getFn) {
            controlList.get(tag).get = getFn;
        }

        public void registerCommonControls() {
            for (TEControlTag tag : controlList.keySet()) {
                addParameter(tag.getPath(), controlList.get(tag).control);
            }
        }

        public void unregisterCommonControls(){
            for (TEControlTag tag : controlList.keySet()) {
                removeParameter(tag.getPath());
            }
            controlList.clear();
        }

        public void buildDefaultControlList() {
            LXParameter p;

            p = new CompoundParameter("Speed", 0.1f, -1.0, 1.0)
                    .setExponent(3.0)
                    .setDescription("Speed");
            setControl(TEControlTag.SPEED,p,defaultGetFn);

            p = new CompoundParameter("xPos1", 0f, -1.0, 1.0)
                    .setDescription("X Position");
            setControl(TEControlTag.XPOS,p,defaultGetFn);


            p = new CompoundParameter("yPos1", 0f, -1.0, 1.0)
                            .setDescription("Y Position");
            setControl(TEControlTag.YPOS,p,defaultGetFn);

            p = new CompoundParameter("Size", 0f, -1.0, 1.0)
                            .setDescription("Size");
            setControl(TEControlTag.SIZE,p,defaultGetFn);

            p = new CompoundParameter("Quantity", 0.5f, 0, 1.0)
                            .setDescription("Quantity");
            setControl(TEControlTag.QUANTITY,p,defaultGetFn);

            p = (CompoundParameter)
                    new CompoundParameter("Spin", 0f, -1.0, 1.0)
                            .setExponent(3)
                            .setDescription("Spin");
            setControl(TEControlTag.SPIN,p,defaultGetFn);

            p = new CompoundParameter("Brightness", 1.0f, 0.0, 1.0)
                            .setDescription("Brightness");
            setControl(TEControlTag.BRIGHTNESS,p,defaultGetFn);

            p = new CompoundParameter("Wow1", 0f, 0f, 1.0)
                            .setDescription("Wow 1");
            setControl(TEControlTag.WOW1,p,defaultGetFn);

            p = new CompoundParameter("Wow2", 0f, 0f, 1.0)
                            .setDescription("Wow 2");
            setControl(TEControlTag.WOW2,p,defaultGetFn);

            p =  new BooleanParameter("WowTrigger", false)
                    .setMode(BooleanParameter.Mode.MOMENTARY)
                    .setDescription("Trigger WoW effects");
            setControl(TEControlTag.WOWTRIGGER,p,defaultGetFn);

        }

        void registerColorControl() {
            color = registerColor("Color", "te_color", ColorType.PRIMARY,
                    "Panel Color");
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

    protected float maxRotationsPerSecond;
    protected float maxRotationsPerBeat;

    protected float maxTimeMultiplier;
    public VariableSpeedTimer iTime;

    protected VariableSpeedTimer spinTimer;
    protected TECommonControls controls;

    FloatBuffer palette =  Buffers.newDirectFloatBuffer(15);

    protected TEPerformancePattern(LX lx,boolean doColorSetup) {
        super(lx);
        maxRotationsPerSecond = 4.0f;
        maxRotationsPerBeat = 4.0f;
        maxTimeMultiplier = 8.0f;

        iTime = new VariableSpeedTimer();
        spinTimer = new VariableSpeedTimer();
        controls = new TECommonControls(doColorSetup);;
    }

    protected TEPerformancePattern(LX lx) {
        this(lx,true);
    }

    public void registerColorControl() {
        this.controls.registerColorControl();
    }

    public FloatBuffer getCurrentPalette() {
        int col;
        float r,g,b;

        if (palette != null ) {
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

    public void setMaxRotationsPerSecond(float maxRotationsPerSecond) {
        this.maxRotationsPerSecond = maxRotationsPerSecond;
    }

    public void setMaxRotationsPerBeat(float maxRotationsPerBeat) {
        this.maxRotationsPerBeat = maxRotationsPerBeat;
    }

    public void setMaxTimeMultiplier(float m) {
        this.maxTimeMultiplier = m;
    }

    /**
     * @return Returns the current rotation angle in radians, derived from a real-time LFO, the setting
     * of the "spin" control, and the constant MAX_ROTATIONS_PER_SECOND
     */
    public float getRotationAngle() {
        return (float) (TEMath.TAU * (spinTimer.getTime() % 1));
    }

    /**
     * @return Returns the current rotation angle in radians, derived from the sawtooth wave provided
     * by getTempo().basis(), the setting of the "spin" control, and a preset maximum rotations
     * per beat.
     */
    public float getRotationAngleOverBeat() {
        return (float) (TEMath.TAU * this.getTempo().basis() * (controls.getValue(TEControlTag.SPIN) * maxRotationsPerBeat));
    }

    public int getCurrentColor() {
        return controls.color.calcColor();
    }

    public float getSpeed() {
        return controls.getValue(TEControlTag.SPEED);
    }

    public float getXPos() {
        return controls.getValue(TEControlTag.XPOS);
    }

    public float getYPos() {
        return controls.getValue(TEControlTag.YPOS);
    }

    public float getSize() {
        return controls.getValue(TEControlTag.SIZE);
    }

    public float getQuantity() { return controls.getValue(TEControlTag.QUANTITY); }

    /**
     *    For most uses, getRotationAngle() is recommended, but if you
     *    need direct acces to the spin control value, here it is.
     */
    public float getSpin() {
        return controls.getValue(TEControlTag.SPIN);
    }

    public float getBrightness() {
        return controls.getValue(TEControlTag.BRIGHTNESS);
    }

    public float getWow1() {
        return controls.getValue(TEControlTag.WOW1);
    }

    public float getWow2() {
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
