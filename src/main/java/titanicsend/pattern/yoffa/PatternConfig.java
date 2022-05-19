package titanicsend.pattern.yoffa;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.yoffa.effect.PulseEffect;
import titanicsend.pattern.yoffa.effect.ShimmeringEffect;
import titanicsend.pattern.yoffa.effect.AlternatingDotsEffect;
import titanicsend.pattern.yoffa.effect.BreathingDotsEffect;
import titanicsend.pattern.yoffa.effect.shaders.WaterShader;
import titanicsend.pattern.yoffa.effect.shaders.WavyShader;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.effect.shaders.OutrunGridShader;
import titanicsend.pattern.yoffa.effect.shaders.NeonHeartShader;

import java.util.List;

@SuppressWarnings("unused")
public class PatternConfig {

    public static Class[] getPatterns() {
        return PatternConfig.class.getDeclaredClasses();
    }

    @LXCategory("Yoffa Panel Combo")
    public static class StarryHeart extends ConstructedPattern {
        public StarryHeart(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> getEffects() {
            return List.of(
                    new NeonHeartShader(PatternTarget.splitPanelSections(this)),
                    new BreathingDotsEffect(PatternTarget.splitPanelSections(this))
                            .setShouldBlend(true)
            );
        }
    }

    @LXCategory("Yoffa Panel Combo")
    public static class StarryOutrun extends ConstructedPattern {
        public StarryOutrun(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> getEffects() {
            return List.of(
                    new OutrunGridShader(PatternTarget.splitPanelSections(this)),
                    new AlternatingDotsEffect(PatternTarget.splitPanelSections(this))
                            .setHorizon(OutrunGridShader.HORIZON_Y)
                            .setShouldBlend(true)
            );
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class NeonHeart extends ConstructedPattern {
        public NeonHeart(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> getEffects() {
            return List.of(new NeonHeartShader(PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class Outrun extends ConstructedPattern {
        public Outrun(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> getEffects() {
            return List.of(new OutrunGridShader(PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class WaterPanels extends ConstructedPattern {
        public WaterPanels(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> getEffects() {
            return List.of(new WaterShader(PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Yoffa Edge Shader")
    public static class WaterEdges extends ConstructedPattern {
        public WaterEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> getEffects() {
            return List.of(new WaterShader(PatternTarget.allEdgesAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Panel Shader")
    public static class WavyPanels extends ConstructedPattern {
        public WavyPanels(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> getEffects() {
            return List.of(new WavyShader(PatternTarget.splitPanelSections(this)));
        }
    }

    @LXCategory("Yoffa Edge Shader")
    public static class WavyEdges extends ConstructedPattern {
        public WavyEdges(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> getEffects() {
            return List.of(new WavyShader(PatternTarget.allEdgesAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Panel Organic")
    public static class PulseCenter extends ConstructedPattern {
        public PulseCenter(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> getEffects() {
            return List.of(new PulseEffect(PatternTarget.allPanelsAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Panel Organic")
    public static class PulseSide extends ConstructedPattern {
        public PulseSide(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> getEffects() {
            return List.of(new PulseEffect(PatternTarget.allPanelsAsCanvas(this)).setOrigin(0, 0, 0));
        }
    }

    @LXCategory("Yoffa Panel Organic")
    public static class AlternatingDots extends ConstructedPattern {
        public AlternatingDots(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> getEffects() {
            return List.of(new AlternatingDotsEffect(PatternTarget.allPanelsAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Panel Organic")
    public static class BreathingDots extends ConstructedPattern {
        public BreathingDots(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> getEffects() {
            return List.of(new BreathingDotsEffect(PatternTarget.allPanelsAsCanvas(this)));
        }
    }

    @LXCategory("Yoffa Edge Organic")
    public static class PowerGrid extends ConstructedPattern {
        protected PowerGrid(LX lx) {
            super(lx);
        }
        @Override
        protected List<PatternEffect> getEffects() {
            return List.of(new ShimmeringEffect(PatternTarget.allEdgesAsCanvas(this)));
        }
    }

}
