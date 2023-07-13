package titanicsend.pattern.yoffa.config;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import java.util.List;

@SuppressWarnings("unused")
public class ShaderEdgesPatternConfig {

    //multiple
    @LXCategory("Native Shaders Edges")
    public static class LightBeamsEdges extends ConstructedPattern {
        public LightBeamsEdges(LX lx) {
            super(lx, TEShaderView.ALL_EDGES);

            markUnusedControl(TEControlTag.QUANTITY);
            markUnusedControl(TEControlTag.BRIGHTNESS);
            markUnusedControl(TEControlTag.WOW1);
            markUnusedControl(TEControlTag.WOWTRIGGER);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
            controls.setValue(TEControlTag.SPEED, 0.5);

            return List.of(new NativeShaderPatternEffect("light_beams.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Edges")
    public static class NeonRipplesEdges extends ConstructedPattern {
        public NeonRipplesEdges(LX lx) {
            super(lx, TEShaderView.ALL_EDGES);

            markUnusedControl(TEControlTag.BRIGHTNESS);
            markUnusedControl(TEControlTag.WOWTRIGGER);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            // set up parameters for the edge version of this...
            controls.setRange(TEControlTag.SPEED, 0, -4, 4); // overall scale
            controls.setValue(TEControlTag.SPEED, 0.5);

            controls.setRange(TEControlTag.SIZE, 2, 6, 0.1); // overall scale
            controls.setValue(TEControlTag.SIZE, 2.25);

            controls.setRange(TEControlTag.QUANTITY, 20, 1, 50);  // pixelation scale
            controls.setValue(TEControlTag.QUANTITY, 8);

            controls.setRange(TEControlTag.WOW1, 0, 0, 0.2);  // "wiggle" in rings
            controls.setValue(TEControlTag.WOW1, 0.15);

            controls.setRange(TEControlTag.WOW2, 0, 0, 3);  // radial rotation distortion
            controls.setValue(TEControlTag.WOW2, 2.25);

            controls.setValue(TEControlTag.SPIN, 0.05);

            return List.of(new NativeShaderPatternEffect("neon_ripples.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Edges")
    public static class SpaceExplosionEdges extends ConstructedPattern {
        public SpaceExplosionEdges(LX lx) {
            super(lx, TEShaderView.ALL_EDGES);

            markUnusedControl(TEControlTag.SIZE);
            markUnusedControl(TEControlTag.QUANTITY);
            markUnusedControl(TEControlTag.BRIGHTNESS);
            markUnusedControl(TEControlTag.WOW1);
            markUnusedControl(TEControlTag.WOWTRIGGER);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.SPEED, 0, -1.5, 1.5); // speed
            controls.setExponent(TEControlTag.SPEED, 2.0);
            controls.setValue(TEControlTag.SPEED, 0.5);

            return List.of(new NativeShaderPatternEffect("space_explosion.fs",
                new PatternTarget(this)));
        }
    }
}
