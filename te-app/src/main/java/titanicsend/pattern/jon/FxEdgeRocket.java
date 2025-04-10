package titanicsend.pattern.jon;

import static titanicsend.util.TEColor.setBrightness;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Edge FG")
public class FxEdgeRocket extends TEPerformancePattern {
  boolean active = false;
  boolean stopRequest = false;
  double time;
  double startTime;
  float rocketSize = 0.06f;
  float trailLength = 0.45f;

  public FxEdgeRocket(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // Size controls the size of the rocket
    controls.setRange(TEControlTag.SIZE, 0.06, 0.02, 0.2);

    // Quantity controls the length of the trail
    controls.setRange(TEControlTag.QUANTITY, 0.45, 0, 1);

    // Speed controls the speed of the rocket. It is limited to twice
    // per beat, and can't run backwards.
    controls.setRange(TEControlTag.SPEED, 1, -2, 2);
    allowBidirectionalTime(false);

    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));

    addCommonControls();
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    if (!this.active) return;

    // generate a sawtooth wave that goes from 0 to 1 over the interval of a second
    // (which will, of course, be variable in this context)
    time = getTime() * 1000.0;
    float cycle = (float) (time - startTime) / 1000f;

    // if we've completed a cycle see if we reset or stop,
    // depending on the state of the WowTrigger button
    if (cycle >= 1f) {
      if (stopRequest) {
        this.active = false;
        this.stopRequest = false;
        return;
      }
      startTime = time;
      cycle = 0;
    }

    // get rocket and trail sizes from controls
    rocketSize = (float) getSize();
    trailLength = (float) getQuantity();

    // precalculate trig for point rotation so we don't have to do it for every point
    double theta = (float) getRotationAngleFromSpin();
    float cosT = (float) Math.cos(theta);
    float sinT = (float) Math.sin(theta);

    // since the wavefront is pushing the trail ahead of it, we need to start it below
    // the actual model, so visible effect will start at the "bottom" edge of the model
    // when the effect is triggered.  Create a scaled and shifted version of cycle to
    // represent the visible wavefront.
    float cc = ((1 + trailLength) * cycle) - (0.5f + trailLength);

    int color = calcColor();

    // Basic approach adapted from Ben Hencke's "Fireworks Nova" pixelblaze pattern.
    // Build a moving sawtooth, coloring the leading edge with a solid color and
    // trailing off into random sparkles.  It winds up looking a lot like a particle
    // system, but it's much cheaper to compute.
    for (LXPoint point : modelTE.getEdgePoints()) {
      float spark;

      // rotate point at x, y using precalculated trig values
      // note that we only calculate the y value b/c that's all we need.
      float x = point.xn - 0.5f;
      float y = (-0.5f + point.yn);
      y = x * sinT + y * cosT;

      // get distance from point to wavefront
      float dist = (float) Math.abs(y - cc);

      // bright rocket at front of wave, with trailing
      // sparkles that fade out behind.
      // Pixels outside the wave are not touched, and so
      // effectively transparent
      if ((y >= cc) && (dist <= trailLength)) {
        spark = dist / trailLength;
        if (1.0f - spark < rocketSize) {
          spark = 1.0f;
        } else {
          spark = (spark > (3f * Math.random())) ? spark * spark * spark : 0f;
        }
        colors[point.index] = setBrightness(color, spark);
      }
    }
  }

  // Button activation logic.  Keep going while button is held,
  // stop at end of cycle when released.
  @Override
  protected void onWowTrigger(boolean on) {
    // trigger pattern when wow button is pressed.
    // restart if retriggered before cycle is complete.
    if (on) {
      this.active = true;
      stopRequest = false;
      startTime = getTime() * 1000.0;
    } else {
      stopRequest = true;
    }
  }
}
