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

    public Phasers(LX lx) {
        super(lx);

        // create new effect with alpha on and no automatic
        // parameter uniforms
        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        // set parameters for common controls

        // start with beam split 5 ways and spinning slowly
        controls.setRange(TEControlTag.QUANTITY, 5, 1, 8)
                .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

        // Speed controls background movement speed and direction
        controls.setRange(TEControlTag.SPEED, 0.25, -1.0, 1.0);

        // Spin controls spin rate
        controls.setRange(TEControlTag.SPIN, 0.25, -1, 1);

        // Size controls beam width and dispersion
        controls.setRange(TEControlTag.SIZE, 21, 40, 2);

        // Wow1 controls beat reactivity
        controls.setRange(TEControlTag.WOW1, 0.0, 0.0, 0.6);

        // Wow2 is background fog brightness
        controls.setRange(TEControlTag.WOW2, 2, 0, 4);

        // After configuring all the common controls, register them with the UI
        addCommonControls();

        // Create the underlying shader pattern
        effect = new NativeShaderPatternEffect("phasers.fs",
                PatternTarget.allPanelsAsCanvas(this), options);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {

        // use the size control to control both the laser's beam size and surrounding glow
        CompoundParameter scaleCtl = (CompoundParameter) controls.getLXControl(TEControlTag.SIZE);
        double beamWidth = 0.005 + 0.0125 * scaleCtl.getNormalized();
        shader.setUniform("beamWidth", (float) beamWidth);

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
