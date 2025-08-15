package titanicsend.pattern.yoffa.config;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.ConstructedShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@SuppressWarnings("unused")
public class ShaderEdgesPatternConfig {

  @LXCategory("Native Shaders Edges")
  public static class LightBeamsEdges extends ConstructedShaderPattern {
    public LightBeamsEdges(LX lx) {
      super(lx, TEShaderView.ALL_EDGES);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
      controls.setValue(TEControlTag.SPEED, 0.5);
      controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
      controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));

      addShader("light_beams.fs");
    }
  }

  @LXCategory("Native Shaders Edges")
  public static class NeonRipplesEdges extends ConstructedShaderPattern {
    public NeonRipplesEdges(LX lx) {
      super(lx, TEShaderView.ALL_EDGES);
    }

    @Override
    protected void createShader() {
      // set up parameters for the edge version of this...
      controls.setRange(TEControlTag.SPEED, 0, -4, 4); // overall scale
      controls.setValue(TEControlTag.SPEED, 0.5);

      controls.setRange(TEControlTag.SIZE, 2, 6, 0.1); // overall scale
      controls.setValue(TEControlTag.SIZE, 2.25);

      controls.setRange(TEControlTag.QUANTITY, 20, 1, 50); // pixelation scale
      controls.setValue(TEControlTag.QUANTITY, 8);

      controls.setRange(TEControlTag.WOW1, 0, 0, 0.2); // "wiggle" in rings
      controls.setValue(TEControlTag.WOW1, 0.15);

      controls.setRange(TEControlTag.WOW2, 0, 0, 3); // radial rotation distortion
      controls.setValue(TEControlTag.WOW2, 2.25);

      controls.setValue(TEControlTag.SPIN, 0.05);

      controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
      controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));

      addShader("neon_ripples.fs");
    }
  }

  @LXCategory("Native Shaders Edges")
  public static class SpaceExplosionEdges extends ConstructedShaderPattern {
    public SpaceExplosionEdges(LX lx) {
      super(lx, TEShaderView.ALL_EDGES);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0, -1.5, 1.5); // speed
      controls.setExponent(TEControlTag.SPEED, 2.0);
      controls.setValue(TEControlTag.SPEED, 0.5);
      controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
      controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
      controls.markUnused(controls.getLXControl(TEControlTag.WOW2));

      addShader("space_explosion.fs");
    }
  }
}
