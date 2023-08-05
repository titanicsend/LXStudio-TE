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
public class TriangleInfinityLevels extends TriangleInfinity {
    AudioLevelsTracker levelsTracker;

    public TriangleInfinityLevels(LX lx) {
        super(lx);

        int fullNBands = eq.getNumBands();
        int halfNBands = fullNBands / 2;
        levelsTracker = new AudioLevelsTracker(eq);
        levelsTracker.addBandRange(0, halfNBands);
        levelsTracker.addBandRange(halfNBands, fullNBands - halfNBands);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        float iScaledLo = levelsTracker.sample(0, deltaMs);
        float iScaledHi = levelsTracker.sample(1, deltaMs);
        shader.setUniform("iQuantity", 1.0f + 5.0f * Math.abs(iScaledLo));
        shader.setUniform("iWow1", 0.5f + Math.abs(iScaledLo));

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
