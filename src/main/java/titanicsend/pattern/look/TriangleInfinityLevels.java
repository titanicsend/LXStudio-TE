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
public class TriangleInfinityLevels extends ConstructedPattern {
    public TriangleInfinityLevels(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected List<PatternEffect> createEffects() {
        controls.setValue(TEControlTag.YPOS, -0.17);
        controls.setRange(TEControlTag.SPEED, 0.25, 0.05, 2.0);
        controls.setRange(TEControlTag.QUANTITY, 6.0, 2.0, 12.0);
        controls.setRange(TEControlTag.WOW1, 0.09, 0.0, 0.5);
        controls.setRange(TEControlTag.WOW2, 1.9, 1.0, 3.0);

        return List.of(new NativeShaderPatternEffect("triangle_infinity.fs",
                new PatternTarget(this)));
    }
}
