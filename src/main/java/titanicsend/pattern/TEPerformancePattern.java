package titanicsend.pattern;

import heronarts.lx.LX;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.FunctionalParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.util.TEMath;

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

        public final CompoundParameter spin = (CompoundParameter)
                new CompoundParameter("Spin", 0f, -1.0, 1.0)
                        .setExponent(3)
                        .setUnits(LXParameter.Units.HERTZ)
                        .setDescription("Spin");

        public final CompoundParameter brightness =
                new CompoundParameter("Brightness", 0.1f, -1.0, 1.0)
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

        TECommonControls() {
            color = registerColor("Color", "te_color", ColorType.PRIMARY,
                    "Panel Color");
            addParameter("te_speed",speed);
            addParameter("te_xPos",xPos);
            addParameter("te_yPos",yPos);
            addParameter("te_size",size);
            addParameter("te_spin",spin);
            addParameter("te_brightness",brightness);
            addParameter("te_wow1",wow1);
            addParameter("te_wow2",wow2);
            addParameter("te_wowTrigger",wowTrigger);


        }
    }

    protected float maxRotationsPerSecond;
    protected float maxRotationsPerBeat;

    protected VariableSpeedTimer spinTimer;
    protected TECommonControls controls;

    public void setMaxRotationsPerSecond(float maxRotationsPerSecond) {
        this.maxRotationsPerSecond = maxRotationsPerSecond;
    }

    public void setMaxRotationsPerBeat(float maxRotationsPerBeat) {
        this.maxRotationsPerBeat = maxRotationsPerBeat;
    }

    protected TEPerformancePattern(LX lx) {
        super(lx);
        maxRotationsPerSecond = 4.0f;
        maxRotationsPerBeat = 4.0f;

        spinTimer = new VariableSpeedTimer();
        controls = new TECommonControls();;
    }

    /**
     * @return Returns the current rotation angle in radians, derived from a real-time LFO, the setting
     * of the "spin" control, and the constant MAX_ROTATIONS_PER_SECOND
     */
    protected float getRotationAngle() {
        return (float) (TEMath.TAU * (spinTimer.getTime() % 1));
    }

    /**
     * @return Returns the current rotation angle in radians, derived from the sawtooth wave provided
     * by getTempo().basis(), the setting of the "spin" control, and a maximum of 4 complete rotations
     * per beat. (If we need to go faster,
     */
    protected float getRotationAngleOverBeat() {
        return (float) (TEMath.TAU * this.getTempo().basis() * (controls.spin.getValue() * maxRotationsPerBeat));
    }

    protected void run(double deltaMs) {
        spinTimer.setScale(controls.spin.getValuef() * maxRotationsPerSecond);
        spinTimer.tick();
        super.run(deltaMs);
    }



}
