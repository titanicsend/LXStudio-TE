package titanicsend.pattern.yoffa.config;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import java.util.Arrays;
import java.util.List;
import titanicsend.pattern.glengine.ConstructedShaderPattern;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.GaussianFilter;
import titanicsend.util.SignalLogger;
import titanicsend.util.TE;

@SuppressWarnings("unused")
public class ShaderPanelsPatternConfig {

  @LXCategory("Native Shaders Panels")
  public static class LightBeamsPattern extends ConstructedShaderPattern {
    // Internally keeps track of what rotation should be applied to the shader texture.
    private float degrees = 0;

    // Kept internally to keep track of the time difference applied to the speed of the shader
    // based on the trebleLevel.
    private float timeDiff = 0;

    // Gaussian filter used to smoothen out the bassLevel
    GaussianFilter degreesFilter = new GaussianFilter(10);

    // Gaussian filter used to smoothen out the trebleLevel
    GaussianFilter timeDiffFilter = new GaussianFilter(20);
    float levelReactivityControl = 0;
    float freqReactivityControl = 0;

    public LightBeamsPattern(LX lx) {
      super(lx, TEShaderView.ALL_POINTS);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SPEED, 0.02, -2, 2);
      controls.setRange(TEControlTag.SPIN, 0.05, -1.0, 1.0);
      controls.setRange(TEControlTag.LEVELREACTIVITY, 1.2, 0, 2);
      controls.setRange(TEControlTag.FREQREACTIVITY, 0.01, 0, 1);

      addShader(
          "light_beams.fs",
          new GLShaderFrameSetup() {
            @Override
            public void OnFrame(GLShader s) {
              // Update the pattern local control values based on the UI values.
              levelReactivityControl =
                  (float) getControls().getControl(TEControlTag.LEVELREACTIVITY).getValue();
              freqReactivityControl =
                  (float) getControls().getControl(TEControlTag.FREQREACTIVITY).getValue();

              // Get the rotation angle from the spin. This is the default value for the
              // iRotationAngle and we're going to apply a diff on this value based on the
              // bassLevel.
              double radians = getRotationAngleFromSpin();
              int spinControlSign =
                  Float.compare(
                      (float) getControls().getLXControl(TEControlTag.SPIN).getValue(), 0.0f);
              radians += getRotationDiff() * spinControlSign;
              s.setUniform("iRotationAngle", (float) radians);

              // Similar to the rotation, but for speed instead.
              double currentTime = getTime();
              currentTime += getTimeDiff();
              s.setUniform("iTime", (float) currentTime);
            }
          });
    }

    /***
     * This function updates the internal degree value based on the audio signals.
     * @return
     */
    private float getRotationDiff() {
      // Use the GaussianFilter to smppthen the bassLevel signal
      double filteredBassLevel =
          degreesFilter.applyGaussianFilter((float) bassLevel * levelReactivityControl);
      // Amplify the filtered level by 5 so it is more visible.
      float degreeStep = (float) (filteredBassLevel * 5);

      // Update the internal degree value.
      degrees -= degreeStep;
      degrees = normalizeDegree(degrees);

      return degreesToRadians(degrees);
    }

    /**
     * Utility function to convert degrees to radians.
     *
     * @param degrees
     * @return degree converted to radian.
     */
    private float degreesToRadians(float degrees) {
      return (float) (degrees * Math.PI / 180.0);
    }

    /**
     * Function used to normalize the degree values. This normalization keeps the value between [0,
     * 360] and is not affecting the visuals. Users can call this to keep the degree values sane and
     * avoid potential overflow problems.
     *
     * @param degree The angel in degrees
     * @return the degree brought back to 0-360
     */
    private float normalizeDegree(double degree) {
      return (degrees % 360 + 360) % 360;
    }

    private float getTimeDiff() {
      float filtered =
          (float) timeDiffFilter.applyGaussianFilter(trebleLevel * freqReactivityControl);
      timeDiff += filtered;

      return timeDiff;
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
    public NeonCells(LX lx) {
      super(lx, TEShaderView.SPLIT_PANEL_SECTIONS);
    }

    @Override
    protected void createShader() {
      controls.setRange(TEControlTag.SIZE, 1, 2, 0.25); // overall scale

      addShader("neon_cells.fs");
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

      controls.setRange(TEControlTag.SIZE, 1, 6, 0.1); // overall scale
      controls.setRange(TEControlTag.QUANTITY, 6, 1, 16); // number of waves
      controls.setRange(TEControlTag.WOW1, 0, 0, 0.25); // pixelated decomposition

      addShader("metallic_wave.fs");
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
}
