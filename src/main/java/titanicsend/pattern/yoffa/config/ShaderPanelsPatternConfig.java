package titanicsend.pattern.yoffa.config;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.effect.ShaderToyPatternEffect;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.ShaderOptions;

import java.util.List;

@SuppressWarnings("unused")
public class ShaderPanelsPatternConfig {

    @LXCategory("Native Shaders Panels")
    public static class ShaderToyPattern extends ConstructedPattern {
        public ShaderToyPattern(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new ShaderToyPatternEffect(PatternTarget.splitPanelSections(this)));
        }
    }

    //multiple
    @LXCategory("Native Shaders Panels")
    public static class LightBeamsPattern extends ConstructedPattern {
        public LightBeamsPattern(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("light_beams.fs",
                PatternTarget.allPanelsAsIndividual(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class NeonHeartNative extends ConstructedPattern {
        public NeonHeartNative(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("neon_heart.fs",
                PatternTarget.allPanelsAsIndividual(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class Marbling extends ConstructedPattern {
        public Marbling(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("marbling.fs",
                PatternTarget.doubleLargeCanvas(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class NeonRipples extends ConstructedPattern {
        public NeonRipples(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("neon_ripples.fs",
                PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class NeonTriangles extends ConstructedPattern {
        public NeonTriangles(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("neon_triangles.fs",
                PatternTarget.allPanelsAsIndividual(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class SpaceExplosion extends ConstructedPattern {
        public SpaceExplosion(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("space_explosion.fs",
                PatternTarget.allPanelsAsIndividual(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class SynthWaves extends ConstructedPattern {
        public SynthWaves(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("synth_waves.fs",
                PatternTarget.doubleLargeCanvas(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class PulsingHeart extends ConstructedPattern {
        public PulsingHeart(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("pulsing_heart.fs",
                PatternTarget.allPanelsAsIndividual(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class NeonBlocks extends ConstructedPattern {
        public NeonBlocks(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("neon_blocks.fs",
                PatternTarget.doubleLargeCanvas(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class Warp extends ConstructedPattern {
        public Warp(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("warp.fs",
                PatternTarget.doubleLargeCanvas(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class Fire extends ConstructedPattern {
        public Fire(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("fire.fs",
                PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class StormScanner extends ConstructedPattern {
        public StormScanner(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("storm_scanner.fs",
                PatternTarget.doubleLargeCanvas(this), "gray_noise.png"));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class JetStream extends ConstructedPattern {
        public JetStream(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("jet_stream.fs",
                PatternTarget.doubleLargeCanvas(this), "color_noise.png"));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class OutrunGrid extends ConstructedPattern {
        public OutrunGrid(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("outrun_grid.fs",
                PatternTarget.doubleLargeCanvas(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class Galaxy extends ConstructedPattern {
        public Galaxy(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("galaxy.fs",
                PatternTarget.allPanelsAsCanvas(this)));
        }
    }

    @LXCategory("Test")
    public static class AudioTest2 extends ConstructedPattern {
        public AudioTest2(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("audio_test2.fs",
                PatternTarget.allPanelsAsCanvas(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class NeonCells extends ConstructedPattern {
        public NeonCells(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("neon_cells.fs",
                PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("DREVO Shaders")
    public static class SlitheringSnake extends ConstructedPattern {
        public SlitheringSnake(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("snake_approaching.fs",
                PatternTarget.doubleLargeCanvas(this)));
        }
    }

    @LXCategory("DREVO Shaders")
    public static class PulsingPetriDish extends ConstructedPattern {
        public PulsingPetriDish(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("pulsing_petri_dish.fs",
                PatternTarget.allPanelsAsCanvas(this)));
        }
    }

    @LXCategory("DREVO Shaders")
    public static class Mondelbrot extends ConstructedPattern {
        public Mondelbrot(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("mandelbrot.fs",
                PatternTarget.doubleLargeCanvas(this)));
        }
    }

    @LXCategory("Noise")
    public static class TriangleNoise extends ConstructedPattern {
        public TriangleNoise(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.SIZE, 1.15, 0.3, 2);
            controls.setRange(TEControlTag.QUANTITY, 3, 1, 5);
            controls.setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);
            controls.setRange(TEControlTag.SPIN, 0, -0.5,0.5 );
            controls.setRange(TEControlTag.WOW1, 4.4, 3, 6.5);

            return List.of(new NativeShaderPatternEffect("triangle_noise.fs",
                PatternTarget.allPointsAsCanvas(this)));
        }
    }

    @LXCategory("Noise")
    public static class SmokeShader extends ConstructedPattern {
        public SmokeShader(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.SIZE, 1, 3, .3);
            controls.setRange(TEControlTag.QUANTITY, 7, 1, 8);
            controls.setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);
            controls.setRange(TEControlTag.WOW2, 1.0, 0.01, 2);

            return List.of(new NativeShaderPatternEffect("smoke_shader.fs",
                PatternTarget.allPointsAsCanvas(this)));
        }
    }

}
