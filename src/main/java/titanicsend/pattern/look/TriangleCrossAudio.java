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
public class TriangleCrossAudio extends ConstructedPattern {
    public TriangleCrossAudio(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected List<PatternEffect> createEffects() {
        controls.setValue(TEControlTag.SIZE, 0.46);
        controls.setValue(TEControlTag.WOW1, 0.11);
        controls.setValue(TEControlTag.XPOS, 0.08);
        controls.setValue(TEControlTag.YPOS, -0.04);

        return List.of(new NativeShaderPatternEffect("triangle_cross_audio.fs",
                new PatternTarget(this)));
    }
}
