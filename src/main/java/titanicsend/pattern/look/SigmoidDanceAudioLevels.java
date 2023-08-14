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
public class SigmoidDanceAudioLevels extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    public SigmoidDanceAudioLevels(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        controls.setRange(TEControlTag.QUANTITY, 4.0, 0.0, 4.0);
        controls.setRange(TEControlTag.WOW1, 0.5, 0.0, 1.0);
        controls.setRange(TEControlTag.SIZE, 0.6, 0.0, 2.0);

        addCommonControls();

        effect = new NativeShaderPatternEffect("sigmoid_dance.fs", new PatternTarget(this));
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        double bassLevel = getBassLevel();
        double trebleLevel = getTrebleLevel();
//        float volumeRatio = getVolumeRatiof();
//        double bassRatio = getBassRatio();
//        double trebleRatio = getTrebleRatio();

        shader.setUniform("iScaledLo", Math.abs((float) getWow1() * (float) bassLevel + 0.05f));
        shader.setUniform("iScaledHi", Math.abs((float) getWow1() * (float) trebleLevel + 0.05f));

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
