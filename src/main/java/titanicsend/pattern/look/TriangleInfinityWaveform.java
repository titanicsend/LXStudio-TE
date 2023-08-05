package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

@LXCategory("Look Shader Patterns")
public class TriangleInfinityWaveform extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    public TriangleInfinityWaveform(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        controls.setRange(TEControlTag.SPEED, 0.25, 0.05, 2.0);
        // number of "layers" of triangles to apply
        controls.setRange(TEControlTag.QUANTITY, 3.0, 1.0, 9.0);
        // Distortion/offset scaling the space between layers
        controls.setRange(TEControlTag.WOW1, 0.9, 0.5, 2.0);
//        // "Neon-Ness" (how crisp the lines are)
//        controls.setRange(TEControlTag.WOW2, 1.2, 1.0, 3.0);
        // Wave Multiplier
        controls.setRange(TEControlTag.WOW2, 0.1, 0.0, 1.0);

        // register common controls with LX
        addCommonControls();

        effect = new NativeShaderPatternEffect("triangle_infinity_waveform.fs",
                new PatternTarget(this));
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        // run the shader
        effect.run(deltaMs);
    }

    @Override
    // THIS IS REQUIRED if you're not using ConstructedPattern!
    // Initialize the NativeShaderPatternEffect and retrieve the native shader object
    // from it when the pattern becomes active
    public void onActive() {
        super.onActive();
        effect.onActive();
        shader = effect.getNativeShader();
    }

}
