package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.EnumParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

@LXCategory("Combo FG")
public class SpaceExplosion2 extends TEPerformancePattern {

    public enum TriggerMode {
        NORMAL("Explosion on beat"),
        ONESHOT("One Shot Trigger");

        private final String label;

        private TriggerMode(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    public final EnumParameter<TriggerMode> triggerCtl =
        (EnumParameter<TriggerMode>) new EnumParameter<TriggerMode>("Mode", TriggerMode.NORMAL)
            .setDescription("Trigger Mode")
            .setWrappable(false);


    NativeShaderPatternEffect effect;
    NativeShader shader;
    double eventStartTime;
    double elapsedTime;

    double lastBasis;
    static final double eventDuration = 1.00;  // explosion lasts 1 variable speed second
    boolean inExplosion;
    boolean triggerMode;

    // Constructor
    public SpaceExplosion2(LX lx) {
        super(lx);

        controls.setRange(TEControlTag.SPEED, 0, -1.5, 1.5); // speed
        controls.setExponent(TEControlTag.SPEED, 2.0);
        controls.setValue(TEControlTag.SPEED, 0.5);

        // trigger Mode (in Wow1 control position)
        controls.setControl(TEControlTag.WOW1,triggerCtl);

        addCommonControls();

        effect = new NativeShaderPatternEffect("space_explosion2.fs",
            new PatternTarget(this, TEShaderView.ALL_POINTS));

        eventStartTime = -99;
        lastBasis = 0;
        inExplosion = false;
        triggerMode = false;
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {

        TriggerMode mode = triggerCtl.getEnum();
        boolean trigger = getWowTrigger();
        boolean isBeat = false;

        double t = getTime();
        elapsedTime = Math.abs(t - eventStartTime);

        // determine if we've started a beat. We use this rather than engine.tempo.beat()
        // to give us a dependable single trigger, with some flexibility in timing
        // even if we're running slow and miss the exact moment of basis == 0.
        double basis = lx.engine.tempo.basis();
        if (basis <= lastBasis) {
           isBeat = true;
        }
        lastBasis = basis;

        // state machine to control triggered explosion
        // we trigger explosions on start of beat and hold the trigger
        // through the whole beat.
        if (inExplosion) {
            // do something

        } else {
            eventStartTime = t;
            elapsedTime = 0;
        }

        // send our trigger value, rather than the one from the UI
        // control, to the shader
        shader.setUniform("iWowTrigger",trigger);
        shader.setUniform("triggerMode",triggerMode);

        // run the shader
        effect.run(deltaMs);
    }

    @Override
    protected void onWowTrigger(boolean on) {
        // when the wow trigger button is pressed...
        if (on) {
            // in all cases, when the button is pressed, reset the "Speed" timer
            // so an explosion will start immediately
            retrigger(TEControlTag.SPEED);

            // if we're in one-shot trigger mode, set up to fire off a single
            // explosion.
            if (triggerMode == true) {

            }
            // otherwise, explode at the current beat rate as long as the button's down.
            else {

            }
        }
    }

    @Override
    // THIS IS REQUIRED if you're not using ConstructedPattern!
    // Initialize the NativeShaderPatternEffect and retrieve the native shader object
    // from it when the pattern becomes active
    public void onActive() {
        effect.onActive();
        shader = effect.getNativeShader();
    }

    @Override
    public String getDefaultView() {
        return effect.getDefaultView();
    }
}
