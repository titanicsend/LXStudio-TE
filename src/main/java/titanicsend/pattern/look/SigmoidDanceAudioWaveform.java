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
public class SigmoidDanceAudioWaveform extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    public SigmoidDanceAudioWaveform(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        controls.setValue(TEControlTag.YPOS, -0.04);
        controls.setValue(TEControlTag.SIZE, 0.5);
        controls.setRange(TEControlTag.WOW1, 4.0, 0.0, 8.0);
        controls.setRange(TEControlTag.WOW2, 1.0, 0.0, 4.0);
        addCommonControls();

        effect = new NativeShaderPatternEffect("sigmoid_dance_audio_waveform.fs", new PatternTarget(this));
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        shader.setUniform("avgVolume", avgVolume.getValuef());
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
