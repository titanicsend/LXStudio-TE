package titanicsend.pattern.yoffa.config;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;

import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.AlternatingDotsEffect;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import java.util.List;

@SuppressWarnings("unused")
public class ShaderPanelsPatternConfig {

    /* Removed from UI - see TEApp.java for details.
    @LXCategory("Native Shaders Panels")
    public static class ShaderToyPattern extends ConstructedPattern {
        public ShaderToyPattern(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new ShaderToyPatternEffect(new PatternTarget(this, TEShaderView.SPLIT_PANEL_SECTIONS)));
        }
    }

    */

    //multiple
    @LXCategory("Native Shaders Panels")
    public static class LightBeamsPattern extends ConstructedPattern {
        public LightBeamsPattern(LX lx) {
            super(lx, TEShaderView.ALL_PANELS_INDIVIDUAL);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
            controls.setValue(TEControlTag.SPEED, 0.5);

            return List.of(new NativeShaderPatternEffect("light_beams.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class NeonHeartNative extends ConstructedPattern {
        public NeonHeartNative(LX lx) {
            super(lx, TEShaderView.ALL_PANELS_INDIVIDUAL);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.QUANTITY,.25,.01,.5);  // segment length


            return List.of(new NativeShaderPatternEffect("neon_heart.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Utility")
    public static class PixelScanner extends ConstructedPattern {
        public PixelScanner(LX lx) {
            super(lx, TEShaderView.ALL_POINTS);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.QUANTITY,5,1,10);

            return List.of(new NativeShaderPatternEffect("pixel_scanner.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class Marbling extends ConstructedPattern {
        public Marbling(LX lx) {
            super(lx, TEShaderView.DOUBLE_LARGE);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.SPEED, 0, -4, 4); // overall scale
            controls.setValue(TEControlTag.SPEED, 0.5);
            controls.setRange(TEControlTag.SIZE, 3, 6, 1); // overall scale
            controls.setRange(TEControlTag.QUANTITY,7,1,10);  // iterations
            controls.setRange(TEControlTag.WOW1,2.5,1,5);  // x relative scale
            controls.setRange(TEControlTag.WOW2,1.5,1,5);  // y relative scale

            return List.of(new NativeShaderPatternEffect("marbling.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class NeonRipples extends ConstructedPattern {
        public NeonRipples(LX lx) {
            super(lx, TEShaderView.SPLIT_PANEL_SECTIONS);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.SPEED, 0, -4, 4);
            controls.setValue(TEControlTag.SPEED, 0.5);

            controls.setRange(TEControlTag.SIZE, 2, 6, 0.1); // overall scale
            controls.setRange(TEControlTag.QUANTITY,20,1,50);  // pixelation scale
            controls.setRange(TEControlTag.WOW1,0,0,0.25);  // "wiggle" in rings
            controls.setRange(TEControlTag.WOW2,0,0,3);  // radial rotation distortion

            return List.of(new NativeShaderPatternEffect("neon_ripples.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class NeonTriangles extends ConstructedPattern {
        public NeonTriangles(LX lx) {
            super(lx, TEShaderView.ALL_PANELS_INDIVIDUAL);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.SIZE, 1, 0.2, 10); // overall scale
            controls.setRange(TEControlTag.SPEED, 0, -4, 4);
            controls.setValue(TEControlTag.SPEED, 0.5);
            controls.setRange(TEControlTag.QUANTITY,1,2,0.1);  // triangle density
            controls.setRange(TEControlTag.WOW1,1,0.2,5);  // glow

            return List.of(new NativeShaderPatternEffect("neon_triangles.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class SpaceExplosion extends ConstructedPattern {
        public SpaceExplosion(LX lx) {
            super(lx, TEShaderView.ALL_PANELS);
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

    @LXCategory("Native Shaders Panels")
    public static class SynthWaves extends ConstructedPattern {
        public SynthWaves(LX lx) {
            super(lx, TEShaderView.DOUBLE_LARGE);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("synth_waves.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class PulsingHeart extends ConstructedPattern {
        public PulsingHeart(LX lx) {
            super(lx, TEShaderView.ALL_PANELS_INDIVIDUAL);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.SIZE, 1, 2.5, 0.4); // overall scale

            return List.of(new NativeShaderPatternEffect("pulsing_heart.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class NeonBlocks extends ConstructedPattern {
        public NeonBlocks(LX lx) {
            super(lx, TEShaderView.DOUBLE_LARGE);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("neon_blocks.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class Warp extends ConstructedPattern {
        public Warp(LX lx) {
            super(lx, TEShaderView.DOUBLE_LARGE);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("warp.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class Fire extends ConstructedPattern {
        public Fire(LX lx) {
            super(lx, TEShaderView.SPLIT_PANEL_SECTIONS);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("fire.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class StormScanner extends ConstructedPattern {
        public StormScanner(LX lx) {
            super(lx, TEShaderView.DOUBLE_LARGE);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
            controls.setValue(TEControlTag.SPEED, 0.5);

            controls.setRange(TEControlTag.SIZE, 1, 3, 0.5); // overall scale
            controls.setRange(TEControlTag.WOW1, .35, 0.1, 1);  // Contrast

            return List.of(new NativeShaderPatternEffect("storm_scanner.fs",
                new PatternTarget(this), "gray_noise.png"));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class JetStream extends ConstructedPattern {
        public JetStream(LX lx) {
            super(lx, TEShaderView.DOUBLE_LARGE);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("jet_stream.fs",
                new PatternTarget(this), "color_noise.png"));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class OutrunGrid extends ConstructedPattern {
        public OutrunGrid(LX lx) {
            super(lx, TEShaderView.DOUBLE_LARGE);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("outrun_grid.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Yoffa Panel Combo")
    public static class StarryOutrun extends ConstructedPattern {
        public StarryOutrun(LX lx) {
            super(lx, TEShaderView.DOUBLE_LARGE);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(
                    new NativeShaderPatternEffect("outrun_grid.fs", new PatternTarget(this)),
                    new AlternatingDotsEffect(new PatternTarget(this))
                    .setHorizon(AlternatingDotsEffect.OUTRUN_HORIZON_Y)
                    .setShouldBlend(true));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class Galaxy extends ConstructedPattern {
        public Galaxy(LX lx) {
            super(lx, TEShaderView.ALL_PANELS);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("galaxy.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class AudioTest2 extends ConstructedPattern {
        public AudioTest2(LX lx) {
            super(lx, TEShaderView.ALL_PANELS);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("audio_test2.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class NeonCells extends ConstructedPattern {
        public NeonCells(LX lx) {
            super(lx, TEShaderView.SPLIT_PANEL_SECTIONS);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.SIZE, 1, 2, 0.25); // overall scale

            return List.of(new NativeShaderPatternEffect("neon_cells.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("DREVO Shaders")
    public static class SlitheringSnake extends ConstructedPattern {
        public SlitheringSnake(LX lx) {
            super(lx, TEShaderView.DOUBLE_LARGE);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.SPEED, 0, -4, 4);
            controls.setValue(TEControlTag.SPEED, 0.5);

            controls.setRange(TEControlTag.SIZE, 2, 6, 0.35); // overall scale

            controls.setRange(TEControlTag.WOW1,0.5,0,1.00);  // snake color level
            controls.setRange(TEControlTag.WOW2,0.4,0,1.00);  // background level

            return List.of(new NativeShaderPatternEffect("snake_approaching.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("DREVO Shaders")
    public static class PulsingPetriDish extends ConstructedPattern {
        public PulsingPetriDish(LX lx) {
            super(lx, TEShaderView.ALL_PANELS);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            // set up common controls
            controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
            controls.setValue(TEControlTag.SPEED, 0.5);

            controls.setRange(TEControlTag.SIZE, 1, 0.5,5); // overall scale

            return List.of(new NativeShaderPatternEffect("pulsing_petri_dish.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("DREVO Shaders")
    public static class Mondelbrot extends ConstructedPattern {
        public Mondelbrot(LX lx) {
            super(lx, TEShaderView.DOUBLE_LARGE);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            // set up common controls
            controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
            controls.setValue(TEControlTag.SPEED, 0.5);

            controls.setRange(TEControlTag.SIZE, 1, 4, 0.6); // overall scale
            controls.setRange(TEControlTag.WOW1, 0.5, 0.05, 2.5);  // contrast

            return List.of(new NativeShaderPatternEffect("mandelbrot.fs",
                new PatternTarget(this)));
        }
    }

    @LXCategory("DREVO Shaders")
    public static class MetallicWaves extends ConstructedPattern {
        public MetallicWaves(LX lx) {
            super(lx, TEShaderView.ALL_POINTS);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            // set up common controls
            controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
            controls.setValue(TEControlTag.SPEED, 0.5);

            controls.setRange(TEControlTag.SIZE, 1, 6, 0.1); // overall scale
            controls.setRange(TEControlTag.QUANTITY, 6, 1, 16);  // number of waves
            controls.setRange(TEControlTag.WOW1, 0, 0, 0.25);  // pixelated decomposition

            return List.of(
                new NativeShaderPatternEffect("metallic_wave.fs", new PatternTarget(this))
            );
        }
    }



    @LXCategory("Noise")
    public static class SmokeShader extends ConstructedPattern {
        public SmokeShader(LX lx) {
            super(lx, TEShaderView.ALL_POINTS);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            controls.setRange(TEControlTag.SPEED, 0, -4, 4); // overall scale
            controls.setValue(TEControlTag.SPEED, 0.75);

            controls.setRange(TEControlTag.SIZE, 1, 3, .3);
            controls.setRange(TEControlTag.QUANTITY, 7, 1, 8);
            controls.setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);
            controls.setRange(TEControlTag.WOW1, 0, 0, 1);
            controls.setRange(TEControlTag.WOW2, 1.0, 0.25, 2.0);

            return List.of(new NativeShaderPatternEffect("smoke_shader.fs",
                new PatternTarget(this)));
        }
    }
}
