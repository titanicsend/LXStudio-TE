package titanicsend.pattern;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.FunctionalParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.util.TEMath;

import java.nio.FloatBuffer;

public abstract class TEPerformancePattern extends TEAudioPattern {
    public class TECommonControls {

        public LinkedColorParameter color;

        public final CompoundParameter speed =
                new CompoundParameter("Speed", 0.1f, -1.0, 1.0)
                        .setExponent(3.0)
                        .setDescription("Speed");

        public final CompoundParameter xPos =
                new CompoundParameter("xPos1", 0f, -1.0, 1.0)
                        .setDescription("X Position");
        public final CompoundParameter yPos =
                new CompoundParameter("yPos1", 0f, -1.0, 1.0)
                        .setDescription("Y Position");

        public final CompoundParameter size =
                new CompoundParameter("Size", 0f, -1.0, 1.0)
                        .setDescription("Size");

        public final CompoundParameter quantity =
                new CompoundParameter("Quantity", 0.5f, 0, 1.0)
                        .setDescription("Quantity");

        public final CompoundParameter spin = (CompoundParameter)
                new CompoundParameter("Spin", 0f, -1.0, 1.0)
                        .setExponent(3)
                        .setDescription("Spin");

        public final CompoundParameter brightness =
                new CompoundParameter("Brightness", 1.0f, 0.0, 1.0)
                        .setDescription("Brightness");

        public final CompoundParameter wow1 =
                new CompoundParameter("Wow1", 0f, 0f, 1.0)
                        .setDescription("Wow 1");

        public final CompoundParameter wow2 =
                new CompoundParameter("Wow2", 0f, 0f, 1.0)
                        .setDescription("Wow 2");

        public final BooleanParameter wowTrigger =
                new BooleanParameter("WowTrigger", false)
                        .setMode(BooleanParameter.Mode.MOMENTARY)
                        .setDescription("Trigger WoW effects");

        void registerCommonColorControl() {
            color = registerColor("Color", "te_color", ColorType.PRIMARY,
                    "Panel Color");
        }

        TECommonControls(boolean setupColor) {
            if (setupColor) {
                registerCommonColorControl();
            }
            addParameter("te_speed",speed);
            addParameter("te_xPos",xPos);
            addParameter("te_yPos",yPos);
            addParameter("te_size",size);
            addParameter("te_quantity",quantity);
            addParameter("te_spin",spin);
            addParameter("te_brightness",brightness);
            addParameter("te_wow1",wow1);
            addParameter("te_wow2",wow2);
            addParameter("te_wowTrigger",wowTrigger);
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

    public void registerCommonColorControl() {
        this.controls.registerCommonColorControl();
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
        return (float) (TEMath.TAU * this.getTempo().basis() * (controls.spin.getValue() * maxRotationsPerBeat));
    }

    public int getColorControl() {
        return controls.color.calcColor();
    }

    public float getSpeedControl() {
        return controls.speed.getValuef();
    }

    public float getXPosControl() {
        return controls.xPos.getValuef();
    }

    public float getYPosControl() {
        return controls.yPos.getValuef();
    }

    public float getSizeControl() {
        return controls.size.getValuef();
    }

    public float getQuantityControl() { return controls.quantity.getValuef(); }

    /**
     *    For most uses, getRotationAngle() is recommended, but if you
     *    need direct acces to the spin control value, here it is.
     */
    public float getSpinControl() {
        return controls.spin.getValuef();
    }

    public float getBrightnessControl() {
        return controls.brightness.getValuef();
    }

    public float getWow1Control() {
        return controls.wow1.getValuef();
    }

    public float getWow2Control() {
        return controls.wow2.getValuef();
    }

    public boolean getWowTriggerControl() {
        return controls.wowTrigger.isOn();
    }

    protected void run(double deltaMs) {

        iTime.setScale(controls.speed.getValuef() * maxTimeMultiplier);
        iTime.tick();
        spinTimer.setScale(controls.spin.getValuef() * maxRotationsPerSecond);
        spinTimer.tick();

        super.run(deltaMs);
    }
}
