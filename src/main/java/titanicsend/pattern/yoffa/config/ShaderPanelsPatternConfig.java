package titanicsend.pattern.yoffa.config;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.effect.ShaderToyPatternEffect;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.ShaderOptions;
import titanicsend.util.TE;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class ShaderPanelsPatternConfig {

    public static Class[] getPatterns() {
        return ShaderPanelsPatternConfig.class.getDeclaredClasses();
    }

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
    public static class Electric extends ConstructedPattern {
        public Electric(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("electric.fs",
                    PatternTarget.splitPanelSections(this)));
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
                    PatternTarget.splitPanelSections(this)));
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
                    PatternTarget.splitPanelSections(this)));
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
                    PatternTarget.allPanelsAsIndividual(this),"gray_noise.png"));
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
                    PatternTarget.doubleLargeCanvas(this),"color_noise.png"));
        }
    }

    @LXCategory("Native Shaders Panels")
    public static class OutrunGrid extends ConstructedPattern {

        public final LinkedColorParameter color = new LinkedColorParameter("Color");
        public OutrunGrid(LX lx) {
            super(lx);
            addParameter("color", color);
            color.mode.setValue(LinkedColorParameter.Mode.PALETTE);
            color.index.setValue(3);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NativeShaderPatternEffect("outrun_grid.fs",
                    PatternTarget.doubleLargeCanvas(this)));
        }

        @Override
        public void onParameterChanged(LXParameter parameter) {
            if (parameter.getPath().equals("color")) {
                int rgb = color.calcColor();
                TE.log("Parameter %s changed: %s (%f) (rgb=%d)", parameter.getPath(), parameter, parameter.getValue(), rgb);
//                TE.log("r=%d, g=%d, b=%d", LXColor.red(rgb), LXColor.green(rgb), LXColor.blue(rgb));
                TE.log("h=%f, s=%f, b=%f", LXColor.h(rgb), LXColor.s(rgb), LXColor.b(rgb));

                for (Map.Entry<String, LXParameter> e : this.parameters.entrySet()) {
                    TE.log("%s -> %s", e.getKey(), e.getValue());
                }
            }
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

    @LXCategory("Native Shaders Panels")
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
                    PatternTarget.doubleLargeCanvas(this)));
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

}
