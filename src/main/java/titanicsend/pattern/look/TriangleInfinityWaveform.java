package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

import java.util.List;

@LXCategory("Look Shader Patterns")
public class TriangleInfinityWaveform extends ConstructedPattern {
    public TriangleInfinityWaveform(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected List<PatternEffect> createEffects() {
        controls.setValue(TEControlTag.YPOS, -0.17);
        controls.setRange(TEControlTag.SIZE, 1.35, 0.2, 2.0);
        controls.setRange(TEControlTag.SPEED, 0.01, 0.00, 0.5);
        controls.setRange(TEControlTag.QUANTITY, 6.0, 2.0, 12.0);
        controls.setRange(TEControlTag.WOW1, 0.09, 0.0, 0.5);
        controls.setRange(TEControlTag.WOW2, 0.04, 0.0, 0.5);

        return List.of(new NativeShaderPatternEffect("triangle_infinity_waveform.fs",
                new PatternTarget(this)));
    }
}
