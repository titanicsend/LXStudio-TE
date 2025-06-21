package titanicsend.pattern.yoffa.config;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.ConstructedShaderPattern;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.GaussianFilter;

@SuppressWarnings("unused")
public class ShaderPanelsPatternConfig {

  @LXCategory("Native Shaders Panels")
  public static class LightBeamsPattern extends ConstructedShaderPattern {
    public LightBeamsPattern(LX lx) {
      super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
      controls.setValue(TEControlTag.SPEED, 0.5);

      addShader("light_beams.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class NeonHeartNative extends ConstructedShaderPattern {
    public NeonHeartNative(LX lx) {
      super(lx, TEShaderView.ALL_PANELS_INDIVIDUAL);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.QUANTITY, .25, .01, .5); // segment length

      addShader("neon_heart.fs");
    }
  }

  @LXCategory("Utility")
  public static class PixelScanner extends ConstructedShaderPattern {
    public PixelScanner(LX lx) {
      super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.QUANTITY, 5, 1, 10);

      addShader("pixel_scanner.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class RhythmicStatic extends ConstructedShaderPattern {
    public RhythmicStatic(LX lx) {
      super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0.2, -4, 4);
      controls
          .setRange(TEControlTag.SIZE, 0, 0, 5) // overall scale
          .setUnits(TEControlTag.SIZE, LXParameter.Units.INTEGER);
      controls.setRange(TEControlTag.QUANTITY, 0.25, 0.02, 1); // number of layers

      addShader("rhythm_static.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class Marbling extends ConstructedShaderPattern {
    public Marbling(LX lx) {
      super(lx, TEShaderView.DOUBLE_LARGE);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0, -4, 4); // overall scale
      controls.setValue(TEControlTag.SPEED, 0.5);
      controls.setRange(TEControlTag.SIZE, 3, 6, 1); // overall scale
      controls.setRange(TEControlTag.QUANTITY, 7, 1, 10); // iterations
      controls.setRange(TEControlTag.WOW1, 2.5, 1, 5); // x relative scale
      controls.setRange(TEControlTag.WOW2, 1.5, 1, 5); // y relative scale

      addShader("marbling.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class NeonRipples extends ConstructedShaderPattern {
    public NeonRipples(LX lx) {
      super(lx, TEShaderView.SPLIT_PANEL_SECTIONS);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0, -4, 4);
      controls.setValue(TEControlTag.SPEED, 0.5);

      controls.setRange(TEControlTag.SIZE, 2, 6, 0.1); // overall scale
      controls.setRange(TEControlTag.QUANTITY, 20, 1, 50); // pixelation scale
      controls.setRange(TEControlTag.WOW1, 0, 0, 0.25); // "wiggle" in rings
      controls.setRange(TEControlTag.WOW2, 0, 0, 3); // radial rotation distortion

      addShader("neon_ripples.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class NeonTriangles extends ConstructedShaderPattern {
    public NeonTriangles(LX lx) {
      super(lx, TEShaderView.ALL_PANELS_INDIVIDUAL);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SIZE, 1, 0.2, 10); // overall scale
      controls.setRange(TEControlTag.SPEED, 0, -4, 4);
      controls.setValue(TEControlTag.SPEED, 0.5);
      controls.setRange(TEControlTag.QUANTITY, 1, 2, 0.1); // triangle density
      controls.setRange(TEControlTag.WOW1, 1, 0.2, 5); // glow

      addShader("neon_triangles.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class SpaceExplosion extends ConstructedShaderPattern {
    public SpaceExplosion(LX lx) {
      super(lx, TEShaderView.ALL_PANELS);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0, -1.5, 1.5); // speed
      controls.setExponent(TEControlTag.SPEED, 2.0);
      controls.setValue(TEControlTag.SPEED, 0.5);

      addShader("space_explosion.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class SynthWaves extends ConstructedShaderPattern {
    public SynthWaves(LX lx) {
      super(lx, TEShaderView.DOUBLE_LARGE);
    }

    @Override
    protected void createShader() {
      addShader("synth_waves.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class PulsingHeart extends ConstructedShaderPattern {
    public PulsingHeart(LX lx) {
      super(lx, TEShaderView.ALL_PANELS_INDIVIDUAL);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SIZE, 1, 2.5, 0.4); // overall scale

      addShader("pulsing_heart.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class NeonBlocks extends ConstructedShaderPattern {
    public NeonBlocks(LX lx) {
      super(lx, TEShaderView.DOUBLE_LARGE);
    }

    @Override
    protected void createShader() {
      addShader("neon_blocks.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class Warp extends ConstructedShaderPattern {
    public Warp(LX lx) {
      super(lx, TEShaderView.DOUBLE_LARGE);
    }

    @Override
    protected void createShader() {
      addShader("warp.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class Fire extends ConstructedShaderPattern {
    public Fire(LX lx) {
      super(lx, TEShaderView.SPLIT_PANEL_SECTIONS);
    }

    @Override
    protected void createShader() {
      addShader("fire.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class StormScanner extends ConstructedShaderPattern {
    public StormScanner(LX lx) {
      super(lx, TEShaderView.DOUBLE_LARGE);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
      controls.setValue(TEControlTag.SPEED, 0.5);

      controls.setRange(TEControlTag.SIZE, 1, 3, 0.5); // overall scale
      controls.setRange(TEControlTag.WOW1, .35, 0.1, 1); // Contrast

      addShader("storm_scanner.fs", "gray_noise.png");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class JetStream extends ConstructedShaderPattern {
    public JetStream(LX lx) {
      super(lx, TEShaderView.DOUBLE_LARGE);
    }

    @Override
    protected void createShader() {
      addShader("jet_stream.fs", "color_noise.png");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class OutrunGrid extends ConstructedShaderPattern {
    public OutrunGrid(LX lx) {
      super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.YPOS, -.35, -1.0, 1.0);

      addShader("outrun_grid.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class Bubbles extends ConstructedShaderPattern {
    public Bubbles(LX lx) {
      super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0, -4, 4).setValue(TEControlTag.SPEED, 0.5);

      controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
      controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
      controls.setRange(TEControlTag.SIZE, 1, 1.25, 0.25); // bubble size
      controls.setRange(TEControlTag.QUANTITY, 1.0, 1.95, 0.0); // bubble density
      controls.setRange(TEControlTag.WOW1, 0, 0, .09); // beat reactivity
      controls.setRange(TEControlTag.WOW2, 0.15, 0, 1); // background level

      controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

      addShader("bubbles.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class DotVortex extends ConstructedShaderPattern {
    public DotVortex(LX lx) {
      super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0, -4, 4).setValue(TEControlTag.SPEED, 0.5);

      controls.setRange(TEControlTag.SIZE, 1.3, 6., 0.75); // vortex size
      controls.setRange(TEControlTag.SPIN, 0, -0.5, 0.5); // rotate about x
      controls.setRange(TEControlTag.WOW1, 1.0, 0.1, 1.0); // dot shape/mode

      controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
      controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
      controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
      controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
      controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

      addShader("dot_vortex.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class Galaxy extends ConstructedShaderPattern {
    public Galaxy(LX lx) {
      super(lx, TEShaderView.ALL_PANELS);
    }

    @Override
    protected void createShader() {
      addShader("galaxy.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class AudioTest2 extends ConstructedShaderPattern {
    public AudioTest2(LX lx) {
      super(lx, TEShaderView.ALL_PANELS);
    }

    @Override
    protected void createShader() {
      addShader("audio_test2.fs");
    }
  }

  @LXCategory("Native Shaders Panels")
  public static class NeonCells extends ConstructedShaderPattern {

    // Kept internally to keep track of the time difference applied to the speed of the shader
    // based on the trebleLevel.  private float timeDiff = 0;
    private float sumTimeDiff = 0;

    private final GaussianFilter rmsFilter = new GaussianFilter(5);

    public NeonCells(LX lx) {
      super(lx, TEShaderView.SPLIT_PANEL_SECTIONS);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SIZE, 1, 4, 0.25);
      controls.setRange(TEControlTag.SPIN, 0.0, -.4f, .4f);
      controls.setRange(TEControlTag.WOW1, 0.1, 0, 1);

      controls.setRange(TEControlTag.LEVELREACTIVITY, 0.2, 0, 3);
      controls.setRange(TEControlTag.FREQREACTIVITY, 0.8, 0, 2);

      addShader(
          "neon_cells.fs",
          new GLShaderFrameSetup() {
            @Override
            public void OnFrame(GLShader s) {
              float levelReactivityControl =
                  (float) getControls().getControl(TEControlTag.LEVELREACTIVITY).getValue();

              // Similar to the rotation, but for speed instead.
              double currentTime = getTime();
              currentTime += getTimeDiff(levelReactivityControl);
              s.setUniform("iTime", (float) currentTime);
            }
          });
    }

    private float getTimeDiff(float controlLevel) {
      float rms = eq.getRaw();
      float filteredRms = (float) rmsFilter.applyGaussianFilter(rms);
      float controlledRms = rms * controlLevel;

      sumTimeDiff += controlledRms;
      return sumTimeDiff;
    }
  }

  @LXCategory("DREVO Shaders")
  public static class SlitheringSnake extends ConstructedShaderPattern {
    public SlitheringSnake(LX lx) {
      super(lx, TEShaderView.DOUBLE_LARGE);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0, -4, 4);
      controls.setValue(TEControlTag.SPEED, 0.5);

      controls.setRange(TEControlTag.SIZE, 2, 6, 0.35); // overall scale

      controls.setRange(TEControlTag.WOW1, 0.5, 0, 1.00); // snake color level
      controls.setRange(TEControlTag.WOW2, 0.4, 0, 1.00); // background level

      addShader("snake_approaching.fs");
    }
  }

  @LXCategory("DREVO Shaders")
  public static class PulsingPetriDish extends ConstructedShaderPattern {
    public PulsingPetriDish(LX lx) {
      super(lx, TEShaderView.ALL_PANELS);
    }

    @Override
    protected void createShader() {
      // set up common controls
      controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
      controls.setValue(TEControlTag.SPEED, 0.5);

      controls.setRange(TEControlTag.SIZE, 1, 0.5, 5); // overall scale

      addShader("pulsing_petri_dish.fs");
    }
  }

  @LXCategory("DREVO Shaders")
  public static class Mondelbrot extends ConstructedShaderPattern {
    public Mondelbrot(LX lx) {
      super(lx, TEShaderView.DOUBLE_LARGE);
    }

    @Override
    protected void createShader() {
      // set up common controls
      controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
      controls.setValue(TEControlTag.SPEED, 0.5);

      controls.setRange(TEControlTag.SIZE, 1, 4, 0.6); // overall scale
      controls.setRange(TEControlTag.WOW1, 0.5, 0.05, 2.5); // contrast

      addShader("mandelbrot.fs");
    }
  }

  @LXCategory("DREVO Shaders")
  public static class MetallicWaves extends ConstructedShaderPattern {
    public MetallicWaves(LX lx) {
      super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected void createShader() {
      // set up common controls
      controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
      controls.setValue(TEControlTag.SPEED, 0.5);
      controls.setRange(TEControlTag.WOW1, 0, 0, 0.25); // complexity
      controls.setRange(TEControlTag.WOW2, 0, 0, 1); // contrast
      controls.setRange(TEControlTag.QUANTITY, 1, 1, 16); // number of kaleidoscope slices
      controls.setValue(TEControlTag.SPIN, 0);
      controls.setRange(TEControlTag.LEVELREACTIVITY, 0.33, 0, 1);
      controls.setRange(TEControlTag.FREQREACTIVITY, 0.33, 0, 1);

      addShader("metallic_wave.fs");
    }
  }

  @LXCategory("DREVO Shaders")
  public static class MetallicBitWaves extends ConstructedShaderPattern {
    public MetallicBitWaves(LX lx) {
      super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected void createShader() {
      controls.setValue(TEControlTag.LEVELREACTIVITY, 0.05);
      controls.setValue(TEControlTag.FREQREACTIVITY, 0.08);
      // set up common controls
      controls.setRange(TEControlTag.SPEED, 0, -4, 4); // speed
      controls.setValue(TEControlTag.SPEED, 0.5);
      controls.setValue(TEControlTag.SPIN, 0.08);

      controls.setRange(TEControlTag.SIZE, 1, 6, 0.1); // overall scale
      controls.setRange(TEControlTag.QUANTITY, 6, 1, 16); // number of waves
      controls.setRange(TEControlTag.WOW1, 0, 0, 0.25); // pixelated decomposition

      addShader("metallic_bit_wave.fs");
    }
  }

  @LXCategory("DREVO Shaders")
  public static class MatrixScroller extends ConstructedShaderPattern {
    public MatrixScroller(LX lx) {
      super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected void createShader() {

      controls.setRange(TEControlTag.SPEED, 0, -2, 2).setValue(TEControlTag.SPEED, 0.3);

      controls.setRange(TEControlTag.SIZE, 0.01, 0.02, 0.005); // grid size
      controls.setRange(TEControlTag.QUANTITY, 0.7, .1, 0.95); // grid density
      controls.setRange(TEControlTag.WOW1, 0.0, 0, 1);
      controls.setRange(TEControlTag.WOW2, 0.0, 0, 1); // beat reactivity

      controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
      controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
      controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

      addShader("matrixscroll.fs");
    }
  }

  @LXCategory("Noise")
  public static class SmokeShader extends ConstructedShaderPattern {
    public SmokeShader(LX lx) {
      super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0, -4, 4); // overall scale
      controls.setValue(TEControlTag.SPEED, 0.75);

      controls.setRange(TEControlTag.SIZE, 1, 3, .3);
      controls.setRange(TEControlTag.QUANTITY, 7, 1, 8);
      controls.setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);
      controls.setRange(TEControlTag.WOW1, 0, 0, 1);
      controls.setRange(TEControlTag.WOW2, 1.0, 0.25, 2.0);

      addShader("smoke_shader.fs");
    }
  }

  @LXCategory("Mothership")
  public static class RingOfFire extends ConstructedShaderPattern {
    public RingOfFire(LX lx) {
      super(lx, TEShaderView.DOUBLE_LARGE);
    }

    @Override
    protected void createShader() {
      // set up common controls

      controls.setRange(TEControlTag.SIZE, 0.91, 1.15, 0.8);
      controls.setValue(TEControlTag.SPEED, 0.64);

      controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
      controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));
      controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
      controls.markUnused(controls.getLXControl(TEControlTag.WOW2));

      addShader("ringofire.fs");
    }
  }

  @LXCategory("Mothership")
  public static class DarkRadiance extends ConstructedShaderPattern {
    public DarkRadiance(LX lx) {
      super(lx, TEShaderView.DOUBLE_LARGE);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SIZE, 0.64, 0.25, 1); // overall scale

      controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
      controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));
      controls.markUnused(controls.getLXControl(TEControlTag.WOW2));

      addShader("darkradiance.fs");
    }
  }

  @LXCategory("piemonte shaders")
  public static class IceGlintPattern extends ConstructedShaderPattern {
    public IceGlintPattern(LX lx) {
      super(lx, TEShaderView.DOUBLE_LARGE);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0, -4, 4);
      controls.setValue(TEControlTag.SPEED, 0.5);
      controls.setRange(TEControlTag.SIZE, 0.1, 3, 1);
      controls.setRange(TEControlTag.QUANTITY, 1, 20, 10);
      controls.setRange(TEControlTag.WOW1, 0, 2, 1);
      controls.setRange(TEControlTag.WOW2, 0, 1, 0.5);

      addShader("ice_glint.fs");
    }
  }
}
