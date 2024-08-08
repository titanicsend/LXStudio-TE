package titanicsend.pattern.jon;

import static titanicsend.util.TEColor.setBrightness;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TEMath;

@LXCategory("Edge FG")
public class FxXWave extends TEPerformancePattern {
  boolean active = false;
  boolean stopRequest = false;
  double time;
  double startTime;

  public FxXWave(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // Size controls the width of the waves
    controls.setRange(TEControlTag.SIZE, 0.05, 0.01, 0.24);

    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));

    addCommonControls();
  }

  @Override
  public void runTEAudioPattern(double deltaMs) {
    if (!this.active) return;

    // generate a sawtooth wave that goes from 0 to 1 over the interval of a measure
    // (which will, of course, be variable in this context)
    time = getTime() * 4000.0;
    float cycle = (float) (time - startTime) / 4000f;

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

    // create a two peak sawtooth so we can run our wave twice per cycle,
    // and shape it for a more organic look
    float movement = (2f * cycle) % 1;
    movement = movement * movement;

    float lightWave = movement;

    int color = calcColor();
    float width = (float) getSize();

    // precalculate trig for point rotation so we don't have to do it for every point
    double theta = (float) -getRotationAngleFromSpin();
    float cosT = (float) Math.cos(theta);
    float sinT = (float) Math.sin(theta);

    // do one wave on the panels
    for (LXPoint point : this.modelTE.getPanelPoints()) {
      // rotate point at x, y using precalculated trig values
      float x = point.xn - 0.5f;
      float y = (-0.5f + point.yn);
      x = 0.5f + (x * cosT - y * sinT);

      float dist = Math.abs(x - lightWave);

      if (dist <= width) {
        //color = setBrightness(color, (float) TEMath.clamp(dist, 0f, 1f));
        colors[point.index] = color;
      }
    }

    lightWave = 1 - movement;

    // and another on the edges
    for (LXPoint point : modelTE.getEdgePoints()) {

      // rotate point at x, y using precalculated trig values
      float x = point.xn - 0.5f;
      float y = (-0.5f + point.yn);
      x = 0.5f + (x * cosT - y * sinT);

      float dist = Math.abs(x - lightWave);

      if (dist <= width) {
        //color = setBrightness(color, (float) TEMath.clamp(dist, 0f, 1f));
        colors[point.index] = color;
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
      this.stopRequest = false;
      this.startTime = getTime() * 1000.0;
    } else {
      this.stopRequest = true;
    }
  }
}
