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
public class TriangleInfinityRadialWaveform extends ConstructedPattern {
    public TriangleInfinityRadialWaveform(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected List<PatternEffect> createEffects() {
        controls.setValue(TEControlTag.YPOS, -0.17);
        controls.setRange(TEControlTag.SIZE, 1.35, 0.2, 2.0);
        controls.setRange(TEControlTag.SPEED, 0.25, 0.05, 2.0);
//        controls.setRange(TEControlTag.QUANTITY, 8.0, 1.0, 24.0);
        controls.setRange(TEControlTag.QUANTITY, 6.0, 2.0, 12.0);
        controls.setRange(TEControlTag.WOW1, 0.09, 0.0, 0.5);
        controls.setRange(TEControlTag.WOW2, 0.04, 0.0, 0.5);

        return List.of(new NativeShaderPatternEffect("triangle_infinity_radial_waveform.fs",
                new PatternTarget(this)));
    }
}
