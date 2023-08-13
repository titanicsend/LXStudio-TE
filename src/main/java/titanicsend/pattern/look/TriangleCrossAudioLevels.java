package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import java.util.List;

@LXCategory("Look Shader Patterns")
public class TriangleCrossAudioLevels extends ConstructedPattern {

    public TriangleCrossAudioLevels(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected List<PatternEffect> createEffects() {
        controls.setRange(TEControlTag.SIZE, 0.6, 0.3, 1.2);
        controls.setRange(TEControlTag.XPOS, 0.08, -0.5, 0.5);
        controls.setRange(TEControlTag.YPOS, -0.04, -0.5, 0.5);
        controls.setRange(TEControlTag.QUANTITY, 12.0, 1.0, 16.0);
        controls.setRange(TEControlTag.WOW1, 0.3, 0.0, 1.0);
        controls.setRange(TEControlTag.WOW2, 1.0, 0.9, 1.2);

        return List.of(new NativeShaderPatternEffect("triangle_cross.fs",
                new PatternTarget(this)));
    }
}
