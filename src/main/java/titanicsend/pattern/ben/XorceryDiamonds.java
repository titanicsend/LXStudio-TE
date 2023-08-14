package titanicsend.pattern.ben;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import java.util.List;

@LXCategory("Native Shaders Panels")
public class XorceryDiamonds extends ConstructedPattern {
    public XorceryDiamonds(LX lx) {
        super(lx, TEShaderView.ALL_PANELS);
    }

    @Override
    protected List<PatternEffect> createEffects() {
        return List.of(new NativeShaderPatternEffect("xorcery_diamonds.fs",
                new PatternTarget(this)));
    }
}
