package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.pattern.yoffa.shader_engine.ShaderOptions;

@LXCategory("Native Shaders Panels")
public class Phasers extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    // Pattern-specific controls

    // energy pulses brightness to the beat
    public final CompoundParameter energy =
            new CompoundParameter("Energy", .15, 0, 1)
                    .setDescription("Oh boy...");

    /* not used at the moment
    protected final CompoundParameter beatScale = (CompoundParameter)
            new CompoundParameter("Speed", 60, 120, 1)
                    .setExponent(1)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Overall movement speed");

     */

    public Phasers(LX lx) {
        super(lx);

        addParameter("energy", energy);

        // create new effect with alpha on and no automatic
        // parameter uniforms
        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        // set parameters for common controls

        // start with beam split 3 ways
        controls.setRange(TEControlTag.QUANTITY, 3, 1, 8)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

        controls.setRange(TEControlTag.XPOS, 0, -0.5, 0.5)
        .setRange(TEControlTag.YPOS, 0, -0.5, 0.5);

        // start with a little spin
        controls.setValue(TEControlTag.SPIN,-0.08);

        // Wow1 makes the "phaser" dance to the beat a little
        controls.setValue(TEControlTag.WOW1, 0.0);

        // Wow2 is the fog brightness
        controls.setRange(TEControlTag.WOW2, 2, 0, 4);

        // Create the underlying shader pattern
        effect = new NativeShaderPatternEffect("phasers.fs",
                PatternTarget.allPanelsAsCanvas(this), options);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        // Sound reactivity - various brightness features are related to energy
        float e = energy.getValuef();
        shader.setUniform("energy", e * e);

        // Overriding a default uniform -- setting it in user code has priority
        shader.setUniform("iRotationAngle",(float) getRotationAngleOverBeat());

        // movement speed is beat divided by the current time division
        //shader.setUniform("basis",tempoDivisionClick.getBasisf());

        // run the shader
        effect.run(deltaMs);
    }

    @Override
    // THIS IS REQUIRED if you're not using ConstructedPattern!
    // Initialize the NativeShaderPatternEffect and retrieve the native shader object
    // from it when the pattern becomes active
    public void onActive() {
        effect.onActive();
        shader = effect.getNativeShader();
    }

}
