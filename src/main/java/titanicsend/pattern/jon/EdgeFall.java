package titanicsend.pattern.jon;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.nio.FloatBuffer;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Combo FG")
public class EdgeFall extends GLShaderPattern {
  double eventStartTime;
  double elapsedTime;
  static final double fallingCycleLength = 2.75;
  static final double burstDuration = 0.2;
  boolean isFalling;
  static final int LINE_COUNT = 52;
  FloatBuffer gl_segments;
  float[][] saved_lines;
  float[][] working_lines;
  float[][] line_velocity;

  // Work to be done per frame
  GLShaderFrameSetup setup = new GLShaderFrameSetup() {
    @Override
    public void OnFrame(GLShader s) {
      float glowLevel;

      double t = getTime();
      elapsedTime = Math.abs(t - eventStartTime);

      glowLevel = (float) getSize();

      // tiny state machine for falling vs. resting states
      if (isFalling) {
        // simulate short explosive burst (by greatly increasing line
        // width/glow) when event is first triggered
        if (elapsedTime < burstDuration) {
          glowLevel *= (float) (elapsedTime / burstDuration);
        }
      } else {
        eventStartTime = t;
        randomizeLineVelocities();
        elapsedTime = 0;
      }

      moveLines(saved_lines, working_lines);

      s.setUniform("iScale", glowLevel);

      // send current line segment position data
      for (int i = 0; i < LINE_COUNT; i++) {
        setUniformLine(i, working_lines[i][0], working_lines[i][1],working_lines[i][2], working_lines[i][3]);
      }
      s.setUniform("lines", gl_segments, 4);
    }
  };

  // Constructor
  public EdgeFall(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // Size controls line width/glow
    controls.setRange(TEControlTag.SIZE, 80, 200, 15);
    controls.setExponent(TEControlTag.SIZE, 0.3);

    // Wow1 - beat reactive pulse
    controls.setRange(TEControlTag.WOW1, 0, 0, 0.65);
    // wow2 - foreground vs gradient color mix

    addCommonControls();

    addShader("edgefall.fs", setup);

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

    randomizeLineVelocities();

    eventStartTime = -99;
    isFalling = false;
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

    float d = (isFalling) ? (float) (-0.5 * elapsedTime / fallingCycleLength) : 0f;

    for (int i = 0; i < LINE_COUNT; i++) {
      dst[i][0] = src[i][0] + d * line_velocity[i][0]; // x1
      dst[i][1] = src[i][1] + d * line_velocity[i][1]; // y1
      dst[i][2] = src[i][2] + d * line_velocity[i][0]; // x2
      dst[i][3] = src[i][3] + d * line_velocity[i][1]; // y2
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