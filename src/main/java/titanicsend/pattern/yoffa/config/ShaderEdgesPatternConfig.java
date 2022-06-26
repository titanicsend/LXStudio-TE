package titanicsend.pattern.yoffa.config;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.effect.ShaderToyPatternEffect;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;

import java.util.List;

@SuppressWarnings("unused")
public class ShaderEdgesPatternConfig {

    public static Class[] getPatterns() {
        return ShaderEdgesPatternConfig.class.getDeclaredClasses();
    }
    
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
    public static class ElectricEdges extends ConstructedPattern {
        public ElectricEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("electric.fs",
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

    @LXCategory("Native Shaders Edges")
    public static class SynthWavesEdges extends ConstructedPattern {
        public SynthWavesEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("synth_waves.fs",
                    PatternTarget.allEdgesAsCanvas(this)));
        }
    }

    @LXCategory("Native Shaders Edges")
    public static class PulsingHeartEdges extends ConstructedPattern {
        public PulsingHeartEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("pulsing_heart.fs",
                    PatternTarget.allEdgesAsCanvas(this)));
        }
    }

}
