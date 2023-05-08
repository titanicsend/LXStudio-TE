package titanicsend.pattern.yoffa.config;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.effect.ShaderToyPatternEffect;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.util.List;

@SuppressWarnings("unused")
public class ShaderEdgesPatternConfig {

    //multiple
    @LXCategory("Native Shaders Edges")
    public static class LightBeamsEdges extends ConstructedPattern {
        public LightBeamsEdges(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("light_beams.fs",
                PatternTarget.allEdgesAsCanvas(this)));
        }
    }

    @LXCategory("Native Shaders Edges")
    public static class NeonRipplesEdges extends ConstructedPattern {
        public NeonRipplesEdges(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            // set up parameters for the edge version of this...
            controls.setRange(TEControlTag.SIZE, 2, 0.1, 6); // overall scale
            controls.setValue(TEControlTag.SIZE, 2.25);

            controls.setRange(TEControlTag.QUANTITY, 20, 1, 50);  // pixelation scale
            controls.setValue(TEControlTag.QUANTITY, 8);

            controls.setRange(TEControlTag.WOW1, 0, 0, 0.25);  // "wiggle" in rings
            controls.setValue(TEControlTag.WOW1, 0.15);

            controls.setRange(TEControlTag.WOW2, 0, 0, 3);  // radial rotation distortion
            controls.setValue(TEControlTag.WOW2, 2.25);

            controls.setValue(TEControlTag.SPIN, 0.05);

            return List.of(new NativeShaderPatternEffect("neon_ripples.fs",
                PatternTarget.allEdgesAsCanvas(this)));
        }
    }

    @LXCategory("Native Shaders Edges")
    public static class SpaceExplosionEdges extends ConstructedPattern {
        public SpaceExplosionEdges(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("space_explosion.fs",
                PatternTarget.allEdgesAsCanvas(this)));
        }
    }

    @LXCategory("DREVO Shaders")
    public static class MetallicWaves extends ConstructedPattern {
        public MetallicWaves(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(
                new NativeShaderPatternEffect("metallic_wave.fs", PatternTarget.allPanelsAsCanvas(this)),
                new NativeShaderPatternEffect("metallic_wave.fs", PatternTarget.allEdgesAsCanvas(this))
            );
        }
    }
}
