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

    public SigmoidDanceAveraged(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        controls.setRange(TEControlTag.QUANTITY, 2.0, 0.0, 4.0);

        addCommonControls();

        effect = new NativeShaderPatternEffect("sigmoid_dance.fs",
                new PatternTarget(this));
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        float volumeRatio = getVolumeRatiof();
        double bassLevel = getBassLevel();
        double trebleLevel = getTrebleLevel();
        double bassRatio = getBassRatio();
        double trebleRatio = getTrebleRatio();

//        System.out.printf("%s, %s, %s, %s, %s\n", volumeRatio, bassLevel, trebleLevel, bassRatio, trebleRatio);

        shader.setUniform("iScaledLo", (float) bassLevel);
        shader.setUniform("iScaledHi", (float) trebleLevel);

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
