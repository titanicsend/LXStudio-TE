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
            new CompoundParameter("Energy", .1, 0, .4)
                    .setDescription("Dance!");

    public Phasers(LX lx) {
        super(lx);

        // create new effect with alpha on and no automatic
        // parameter uniforms
        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        // set parameters for common controls

        // start with beam split 3 ways
        controls.setRange(TEControlTag.QUANTITY, 3, 1, 8)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

        controls.setRange(TEControlTag.SIZE,1,0.5,5);

        controls.setRange(TEControlTag.XPOS, 0, -0.5, 0.5)
        .setRange(TEControlTag.YPOS, 0, -0.5, 0.5);

        // Wow1 breaks the image into slices
        controls.setRange(TEControlTag.WOW1, 0.0,0,0.9);

        // Wow2 is the fog brightness
        controls.setRange(TEControlTag.WOW2, 2, 0, 4);

        // After configuring all the common controls, register them with the UI
        addCommonControls();

        // Add any extra controls used by this pattern
        addParameter("energy", energy);

        // Create the underlying shader pattern
        effect = new NativeShaderPatternEffect("phasers.fs",
                PatternTarget.allPanelsAsCanvas(this), options);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {

        // Sound reactivity - various brightness features are related to energy
        float e = energy.getValuef();
        shader.setUniform("energy", e * e);

        // Overriding a default uniform -- setting it in user code has priority.
        //
        // Here, iRotationAngle will be used to rotate the beam angle, and will be
        // controlled by the speed control, rather than by the default spin control.
        shader.setUniform("iRotationAngle",(float) getRotationAngleFromSpeed());

        // And we'll create a new, separate uniform using the spin control to rotate
        // the entire canvas.
        shader.setUniform("iCanvasAngle",(float) getRotationAngleFromSpin());

        // run the shader
        effect.run(deltaMs);
    }

    @Override
    // THIS IS REQUIRED for shader-based patterns if you're not using ConstructedPattern!
    // Initialize the NativeShaderPatternEffect and retrieve the native shader object
    // from it when the pattern becomes active
    public void onActive() {
        super.onActive();
        effect.onActive();
        shader = effect.getNativeShader();
    }

}
