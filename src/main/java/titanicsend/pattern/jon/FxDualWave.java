package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import java.util.Arrays;

@LXCategory("Edge FG")
public class FxDualWave extends TEPerformancePattern {
  boolean active = false;
  boolean stopRequest = false;
  double time;
  double startTime;

  public enum TriggerMode {
    ONCE("Once"),
    RUN("Run");

    private final String label;

    TriggerMode(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public final EnumParameter<FxDualWave.TriggerMode> triggerMode =
    new EnumParameter<FxDualWave.TriggerMode>("Mode", FxDualWave.TriggerMode.ONCE)
      .setDescription("Trigger Mode");

  public FxDualWave(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);
    
    // Speed controls the transit speed of the waves
    controls.setRange(TEControlTag.SPEED, 0.5, -1.25, 1.25);

    // Size controls the width of the waves
    controls.setRange(TEControlTag.SIZE, 0.05, 0.01, 0.25);

    // Trigger mode (in Wow1 control position)
    controls.setControl(TEControlTag.WOW1, triggerMode);

    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));
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
  // stop at end of cycle when released. If in continuous RUN mode,
  // you can restart the wave at any time with a new button press, and it will
  // continue to run.
  @Override
  protected void onWowTrigger(boolean on) {
    // trigger pattern when wow button is pressed.
    // restart if retriggered before cycle is complete.
    if (on) {
      this.active = true;
      this.stopRequest = false;
      this.startTime = getTime() * 1000.0;
    } else {
      if (this.triggerMode.getEnum() == TriggerMode.ONCE) {
        this.stopRequest = true;
      }
    }
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    super.onParameterChanged(parameter);
    if (parameter == triggerMode) {
      if (triggerMode.getEnum() == TriggerMode.ONCE) {
        this.active = false;
      }
      else {
        this.active = true;
      }
    }
  }


}
