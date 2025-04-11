package titanicsend.pattern.yoffa.config;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.util.List;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.*;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.media.BasicVideoPatternEffect;

@SuppressWarnings("unused")
public class OrganicPatternConfig {

  @LXCategory("Yoffa Panel Organic")
  public static class PulseCenter extends ConstructedPattern {
    public PulseCenter(LX lx) {
      super(lx, TEShaderView.ALL_PANELS);
    }

    @Override
    protected List<PatternEffect> createEffects() {
      return List.of(new PulseEffect(new PatternTarget(this)));
    }
  }

  @LXCategory("Yoffa Panel Organic")
  public static class AlternatingDots extends ConstructedPattern {
    public AlternatingDots(LX lx) {
      super(lx, TEShaderView.ALL_PANELS);
    }

    @Override
    protected List<PatternEffect> createEffects() {
      return List.of(new AlternatingDotsEffect(new PatternTarget(this)));
    }
  }

  @LXCategory("Yoffa Edge Organic")
  public static class PowerGrid extends ConstructedPattern {
    public PowerGrid(LX lx) {
      super(lx, TEShaderView.ALL_EDGES);
    }

    @Override
    protected List<PatternEffect> createEffects() {
      return List.of(new ShimmeringEffect(new PatternTarget(this)));
    }
  }

  @LXCategory("Yoffa Panel Organic")
  public static class BreathingDots extends ConstructedPattern {
    public BreathingDots(LX lx) {
      super(lx, TEShaderView.ALL_PANELS);
    }

    @Override
    protected List<PatternEffect> createEffects() {
      // set ranges for common controls
      getControls().setRange(TEControlTag.SPEED, 0, -2, 2); // Speed
      getControls().setValue(TEControlTag.SPEED, 0.5);

      return List.of(new BreathingDotsEffect(new PatternTarget(this)));
    }
  }

  @LXCategory("Video Patterns")
  public static class BasicVideoPattern extends ConstructedPattern {
    public BasicVideoPattern(LX lx) {
      super(lx, TEShaderView.SPLIT_PANEL_SECTIONS);
    }

    @Override
    protected List<PatternEffect> createEffects() {
      return List.of(new BasicVideoPatternEffect(new PatternTarget(this)));
    }
  }

  // below patterns for on the fly testing
  @LXCategory("Video Patterns")
  public static class FullscreenVideoA extends ConstructedPattern {
    public FullscreenVideoA(LX lx) {
      super(lx, TEShaderView.ALL_PANELS);
    }

    @Override
    protected List<PatternEffect> createEffects() {
      return List.of(
          new BasicVideoPatternEffect(new PatternTarget(this), "resources/pattern/test_vid_a.mp4"));
    }
  }

  @LXCategory("Video Patterns")
  public static class FullscreenVideoB extends ConstructedPattern {
    public FullscreenVideoB(LX lx) {
      super(lx, TEShaderView.ALL_PANELS);
    }

    @Override
    protected List<PatternEffect> createEffects() {
      return List.of(
          new BasicVideoPatternEffect(new PatternTarget(this), "resources/pattern/test_vid_b.mp4"));
    }
  }
}
