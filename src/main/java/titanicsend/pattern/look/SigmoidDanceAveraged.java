package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.util.TEMath;

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

    private float minLo = Float.POSITIVE_INFINITY;
    private float maxLo = Float.NEGATIVE_INFINITY;
    private float minHi = Float.POSITIVE_INFINITY;
    private float maxHi = Float.NEGATIVE_INFINITY;
    private TEMath.EMA emaLo = new TEMath.EMA(0., .01);
    private TEMath.EMA emaHi = new TEMath.EMA(0., .01);

    @Override
    public void runTEAudioPattern(double deltaMs) {
        int fullNBands = eq.getNumBands();
        int halfNBands = fullNBands / 2;

        // TODO: EMA?
        float curLo = eq.getAveragef(0, halfNBands);
        float curHi = eq.getAveragef(halfNBands, fullNBands - halfNBands);
        emaLo.update(curLo, deltaMs);
        emaHi.update(curHi, deltaMs);
        // TODO: keep max/min on a moving window?
        if (curLo < minLo) {
            minLo = curLo;
        }
        if (curLo > maxLo) {
            maxLo = curLo;
        }
        if (curHi < minHi) {
            minHi = curHi;
        }
        if (curHi > maxHi) {
            maxHi = curHi;
        }

        float runningAvgLo = emaLo.getValuef();
        float runningAvgHi = emaHi.getValuef();

//        System.out.printf("fullNBands = %s, halfNBands = %s\n", fullNBands, halfNBands);
//        System.out.printf("curLo = %s, curHi = %s\n", curLo, curHi);
//        System.out.printf("runningAvgLo = %s, runningAvgHi = %s\n", runningAvgLo, runningAvgHi);

        float normLo = curLo / (maxLo - minLo);
        float normHi = curHi / (maxHi - minHi);
//        System.out.printf("normLo = %s, normHi = %s\n", normLo, normHi);

        float scaledLo = normLo * 2 - 1;
        float scaledHi = normHi * 2 - 1;
//        System.out.printf("scaledLo = %s, scaledHi = %s\n", scaledLo, scaledHi);

//        shader.setUniform("iScaledLo", avgLo);
//        shader.setUniform("iScaledHi", avgHi);
//        shader.setUniform("iScaledLo", normLo);
//        shader.setUniform("iScaledHi", normHi);
        shader.setUniform("iScaledLo", Math.abs(scaledLo)+0.05f);
        shader.setUniform("iScaledHi", Math.abs(scaledHi)+0.05f);

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
