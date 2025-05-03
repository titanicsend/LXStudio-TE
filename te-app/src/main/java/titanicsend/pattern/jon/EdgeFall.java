package titanicsend.pattern.jon;

import static heronarts.lx.utils.Noise.stb_perlin_noise3;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import java.nio.FloatBuffer;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Combo FG")
public class EdgeFall extends GLShaderPattern {
  VariableSpeedTimer realTime;
  double eventStartTime;
  double elapsedTime;
  int fallingCycleBeats;
  int beatCounter;
  static final double burstDuration = 0.2;
  boolean isFalling;

  static final int LINE_COUNT = 52;
  FloatBuffer gl_segments;
  float[][] saved_lines;
  float[][] working_lines;
  float[][] line_velocity;

  // Work to be done per frame
  private void setUniforms(GLShader s) {
    float glowLevel;

    realTime.tick();

    double t = realTime.getTime();
    elapsedTime = Math.abs(t - eventStartTime);

    beatCounter += lx.engine.tempo.beat() ? 1 : 0;

    fallingCycleBeats = (int) Math.floor(getWow1());

    glowLevel = (float) getSize();

    // tiny state machine for falling vs. resting states
    if (isFalling) {
      // simulate short explosive burst (by greatly increasing line
      // width/glow) when event is first triggered
      if (elapsedTime < burstDuration) {
        glowLevel *= (float) (elapsedTime / burstDuration);
      }
      // reset automatically at end of cycle
      else if (beatCounter > fallingCycleBeats) {
        reset(0);
        isFalling = false;
      }
    } else {
      reset(t);
    }

    moveLines(saved_lines, working_lines);

    glowLevel -= 80f * (float) getBassLevel() * (float) getLevelReactivity();

    // set line brightness.  We need to override the default behavior because
    // when a fall is triggered, we make everything very, very bright for a short time
    // to simulate an explosive burst.
    s.setUniform("iScale", glowLevel);

    // override the default variable timer, since this shader needs actual 1:1 seconds.
    s.setUniform("iTime", realTime.getTimef());

    // send current line segment position data
    for (int i = 0; i < LINE_COUNT; i++) {
      setUniformLine(
          i, working_lines[i][0], working_lines[i][1], working_lines[i][2], working_lines[i][3]);
    }
    s.setUniform("lines", gl_segments, 4);
  }

  // Constructor
  public EdgeFall(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // set up timer for fall cycle length.
    realTime = new VariableSpeedTimer();

    // set default speed to 1:1 - line motion looks good that way
    controls.setRange(TEControlTag.SPEED, 1, -4, 4);

    // Size controls line width/glow
    controls.setRange(TEControlTag.SIZE, 80, 200, 37);
    controls.setExponent(TEControlTag.SIZE, 0.3);

    // wow1 - falling phase duration before auto reset (in beats at current tempo)
    controls
        .setRange(TEControlTag.WOW1, 4.0, 1, 16)
        .setUnits(TEControlTag.WOW1, LXParameter.Units.INTEGER);

    // wow2 controls palette color mix

    addCommonControls();

    addShader("edgefall.fs", this::setUniforms);

    // create an n x 4 array, so we can pass line segment descriptors
    // to GLSL shaders.
    // NOTE: This buffer needs to be *exactly* large enough to contain
    // the number of line segments you're using.  No smaller, no bigger.
    this.gl_segments = Buffers.newDirectFloatBuffer(LINE_COUNT * 4 * 4);

    // buffer to hold line descriptors taken from the vehicle
    saved_lines = new float[LINE_COUNT][4];

    // working storage so we can move those lines around
    working_lines = new float[LINE_COUNT][4];

    // per-line x and y velocity components
    line_velocity = new float[LINE_COUNT][2];

    // Select the edges we want to draw. NOTE: To add more edges, you need
    // to change LINE_COUNT so the segment buffer will be the right size. OpenGL
    // is very picky about this!
    CarGeometryPatternTools.getPanelConnectedEdges(getModelTE(), "^S.*$", saved_lines, LINE_COUNT);

    // initialize in the at-rest state
    reset(-99);
  }

  void reset(double time) {
    eventStartTime = time;
    randomizeLineVelocities();
    elapsedTime = 0;
    beatCounter = 0;
  }

  // store segment descriptors in our GL line segment buffer.
  void setUniformLine(int segNo, float x1, float y1, float x2, float y2) {
    // TE.log("setLine %d : %.4f %.4f, %.4f %.4f",segNo,x1,y1,x2,y2);
    gl_segments.position(segNo * 4);
    gl_segments.put(-x1);
    gl_segments.put(y1);
    gl_segments.put(-x2);
    gl_segments.put(y2);
    gl_segments.rewind();
  }

  // generate a random value between a and b with minimum absolute value of c
  float randomBetween(float a, float b, float c) {
    float r = (float) (Math.random() * (b - a) + a);
    if (Math.abs(r) < c) {
      r = (r < 0) ? -c : c;
    }
    return r;
  }

  // set speed and direction of falling lines
  void randomizeLineVelocities() {
    for (int i = 0; i < LINE_COUNT; i++) {
      line_velocity[i][0] = randomBetween(-8, 8, 1);
      line_velocity[i][1] = randomBetween(-5, 5, 1);
    }
  }

  void moveLines(float[][] src, float[][] dst) {

    // if in falling mode, send the lines flying out along their current vectors
    if (isFalling) {
      float d = (float) (-0.5 * elapsedTime / fallingCycleBeats);

      for (int i = 0; i < LINE_COUNT; i++) {
        dst[i][0] = src[i][0] + d * line_velocity[i][0]; // x1
        dst[i][1] = src[i][1] + d * line_velocity[i][1]; // y1
        dst[i][2] = src[i][2] + d * line_velocity[i][0]; // x2
        dst[i][3] = src[i][3] + d * line_velocity[i][1]; // y2
      }
    } else {
      // use structured noise to move the lines around with the beat
      float d = 0.075f * (float) getTrebleLevel() * (float) getFrequencyReactivity();
      for (int i = 0; i < LINE_COUNT; i++) {
        float n = (float) getTime() + (float) i / 2f;
        dst[i][0] = src[i][0] + d * stb_perlin_noise3(n, 0.5f, 0.5f, 10, 10, 10);
        dst[i][1] = src[i][1] + d * stb_perlin_noise3(0.5f, n, 0.5f, 10, 10, 10);
        dst[i][2] = src[i][2] + d * stb_perlin_noise3(0.5f, n, n, 10, 10, 10);
        dst[i][3] = src[i][3] + d * stb_perlin_noise3(n, n, 0.5f, 10, 10, 10);
      }
    }
  }

  @Override
  protected void onWowTrigger(boolean on) {
    // when the wow trigger button is pressed...
    if (on) {
      isFalling = !isFalling;
    }
  }
}
