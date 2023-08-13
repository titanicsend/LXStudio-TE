package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

@LXCategory("Native Shaders Panels")
public class Kaleidosonic extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    public Kaleidosonic(LX lx) {
        super(lx, TEShaderView.DOUBLE_LARGE);

        controls.setRange(TEControlTag.WOW1, 0.04, 0, 0.1);   // bass response
        controls.setRange(TEControlTag.WOW2, 1., 1, 3);    // overall audio level adjustment
        controls.setRange(TEControlTag.QUANTITY, 7, 1, 13);  // number of kaleidoscope slices
        controls.setValue(TEControlTag.SPIN, 0.125);

        // register common controls with LX
        addCommonControls();

        effect = new NativeShaderPatternEffect("kaleidosonic.fs",
                new PatternTarget(this));
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {

        shader.setUniform("avgVolume", avgVolume.getValuef());

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
