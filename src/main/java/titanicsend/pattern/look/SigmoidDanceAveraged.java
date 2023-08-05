package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.audio.GraphicMeter;
import org.opencv.core.Mat;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.util.TEMath;

import java.util.ArrayList;
import java.util.List;

@LXCategory("Look Shader Patterns")
public class SigmoidDanceAveraged extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    AudioLevelsTracker levelsTracker;

    public SigmoidDanceAveraged(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        controls.setRange(TEControlTag.QUANTITY, 2.0, 0.0, 4.0);

        addCommonControls();

        effect = new NativeShaderPatternEffect("sigmoid_dance.fs",
                new PatternTarget(this));

        int fullNBands = eq.getNumBands();
        int halfNBands = fullNBands / 2;
        System.out.printf("fullNBands = %s, halfNBands = %s\n", fullNBands, halfNBands);

        levelsTracker = new AudioLevelsTracker(eq);
        levelsTracker.addBandRange(0, halfNBands);
        levelsTracker.addBandRange(halfNBands, fullNBands - halfNBands);
    }



    @Override
    public void runTEAudioPattern(double deltaMs) {
        float iScaledLo = levelsTracker.sample(0, deltaMs);
        float iScaledHi = levelsTracker.sample(1, deltaMs);
        shader.setUniform("iScaledLo", iScaledLo);
        shader.setUniform("iScaledHi", iScaledHi);

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
