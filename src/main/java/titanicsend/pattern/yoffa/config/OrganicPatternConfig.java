package titanicsend.pattern.yoffa.config;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.will.shaders.*;
import titanicsend.pattern.yoffa.effect.*;
import titanicsend.pattern.yoffa.effect.shaders.*;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.effect.shaders.OutrunGridShader;
import titanicsend.pattern.yoffa.media.BasicVideoPatternEffect;

import java.util.List;

@SuppressWarnings("unused")
public class OrganicPatternConfig {

    /*  Removed from UI - see TEApp.java for details.
    @LXCategory("Yoffa Panel Combo")
    public static class StarryOutrun extends ConstructedPattern {
        public StarryOutrun(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(
                    new OutrunGridShader(new PatternTarget(this, TEShaderView.SPLIT_PANEL_SECTIONS)),
                    new AlternatingDotsEffect(new PatternTarget(this, TEShaderView.SPLIT_PANEL_SECTIONS))
                            .setHorizon(OutrunGridShader.HORIZON_Y)
                            .setShouldBlend(true)
            );
        }
    }
   */

    @LXCategory("Yoffa Panel Shader")
    public static class RainbowSwirlPanels extends ConstructedPattern {
        public RainbowSwirlPanels(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new RainbowSwirlShader(new PatternTarget(this, TEShaderView.DOUBLE_LARGE)));
        }
    }

    @LXCategory("Yoffa Edge Shader")
    public static class RainbowSwirlEdges extends ConstructedPattern {
        public RainbowSwirlEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new RainbowSwirlShader(new PatternTarget(this, TEShaderView.ALL_EDGES)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class NeonBarsPanels extends ConstructedPattern {
        public NeonBarsPanels(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NeonBarsShader(new PatternTarget(this, TEShaderView.SPLIT_PANEL_SECTIONS)));
        }
    }

    @LXCategory("Yoffa Edge Shader")
    public static class NeonBarsEdges extends ConstructedPattern {
        public NeonBarsEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NeonBarsShader(new PatternTarget(this, TEShaderView.ALL_EDGES)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class NeonCellsLegacy extends ConstructedPattern {
        public NeonCellsLegacy(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NeonCellsShader(new PatternTarget(this, TEShaderView.DOUBLE_LARGE)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class WaterPanels extends ConstructedPattern {
        public WaterPanels(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {

            // set ranges for common controls
            getControls().setRange(TEControlTag.SPEED, 0,-2,2); // Speed
            getControls().setValue(TEControlTag.SPEED, 0.5);

            getControls().setRange(TEControlTag.QUANTITY, 0,1,4); // tiling
            getControls().setRange(TEControlTag.SIZE, 1,0.5,3);     // scale
            getControls().setRange(TEControlTag.WOW1, 5,1,20);    // iterations (intensity 1)
            getControls().setRange(TEControlTag.WOW2, 0.005,0.001,0.01); // intensity 2

            return List.of(new WaterShader(new PatternTarget(this, TEShaderView.ALL_PANELS)));
        }
    }

    @LXCategory("Yoffa Edge Shader")
    public static class WaterEdges extends ConstructedPattern {
        public WaterEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {

            // set ranges for common controls
            getControls().setRange(TEControlTag.SPEED, 0,-4,4); // Speed
            getControls().setValue(TEControlTag.SPEED, 0.5);

            getControls().setRange(TEControlTag.QUANTITY, 0,1,4); // tiling
            getControls().setRange(TEControlTag.SIZE, 1,0.5,3);     // scale
            getControls().setRange(TEControlTag.WOW1, 5,1,20);    // iterations (intensity 1)
            getControls().setRange(TEControlTag.WOW2, 0.005,0.001,0.01); // intensity 2

            return List.of(new WaterShader(new PatternTarget(this, TEShaderView.ALL_EDGES)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class WavyPanels extends ConstructedPattern {
        public WavyPanels(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new WavyShader(new PatternTarget(this, TEShaderView.SPLIT_PANEL_SECTIONS)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class NeonSnake extends ConstructedPattern {
        public NeonSnake(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new NeonSnakeShader(new PatternTarget(this, TEShaderView.ALL_PANELS)));
        }
    }

    @LXCategory("Yoffa Edge Shader")
    public static class WavyEdges extends ConstructedPattern {
        public WavyEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new WavyShader(new PatternTarget(this, TEShaderView.ALL_EDGES)));
        }
    }

    @LXCategory("Yoffa Panel Organic")
    public static class PulseCenter extends ConstructedPattern {
        public PulseCenter(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new PulseEffect(new PatternTarget(this, TEShaderView.ALL_PANELS)));
        }
    }

    @LXCategory("Yoffa Panel Organic")
    public static class AlternatingDots extends ConstructedPattern {
        public AlternatingDots(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new AlternatingDotsEffect(new PatternTarget(this, TEShaderView.ALL_PANELS)));
        }
    }

    @LXCategory("Yoffa Edge Organic")
    public static class PowerGrid extends ConstructedPattern {
        public PowerGrid(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new ShimmeringEffect(new PatternTarget(this, TEShaderView.ALL_EDGES)));
        }
    }

    @LXCategory("Video Patterns")
    public static class BasicVideoPattern extends ConstructedPattern {
        public BasicVideoPattern(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new BasicVideoPatternEffect(new PatternTarget(this, TEShaderView.SPLIT_PANEL_SECTIONS)));
        }
    }

    //below patterns for on the fly testing
    @LXCategory("Video Patterns")
    public static class FullscreenVideoA extends ConstructedPattern {
        public FullscreenVideoA(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new BasicVideoPatternEffect(new PatternTarget(this, TEShaderView.ALL_PANELS),
                    "resources/pattern/test_vid_a.mp4"));
        }
    }

    @LXCategory("Video Patterns")
    public static class FullscreenVideoB extends ConstructedPattern {
        public FullscreenVideoB(LX lx) {
            super(lx);
        }

        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new BasicVideoPatternEffect(new PatternTarget(this, TEShaderView.ALL_PANELS),
                    "resources/pattern/test_vid_b.mp4"));
        }
    }

    @LXCategory("DREVO Shaders")
    public static class RhythmicFlashStatic extends ConstructedPattern {
        public RhythmicFlashStatic(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {
            return List.of(new RhythmicFlashingStatic(new PatternTarget(this, TEShaderView.ALL_PANELS)));
        }
    }

    @LXCategory("DREVO Shaders")
    public static class MatrixScroller extends ConstructedPattern {
        public MatrixScroller(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> createEffects() {

            return List.of(new MatrixScrolling(new PatternTarget(this, TEShaderView.ALL_PANELS)));
        }
    }

}
