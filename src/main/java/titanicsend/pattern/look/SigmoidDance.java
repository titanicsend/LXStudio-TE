package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

@LXCategory("Look Shader Patterns")
public class SigmoidDance extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    public SigmoidDance(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        controls.setRange(TEControlTag.QUANTITY, 4.0, 0.0, 4.0);
        controls.setRange(TEControlTag.SIZE, 0.6, 0.0, 2.0);

        // register common controls with LX
        addCommonControls();

        effect = new NativeShaderPatternEffect("sigmoid_dance.fs", new PatternTarget(this));
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        shader.setUniform("iScaledLo", (float) getWow1());
        shader.setUniform("iScaledHi", (float) getWow2());
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
