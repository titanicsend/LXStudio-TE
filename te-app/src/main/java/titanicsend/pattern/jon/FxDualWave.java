package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Edge FG")
public class FxDualWave extends GLShaderPattern {
  double eventStartTime;
  double lastBasis;
  boolean stopRequest; // true if the effect should stop at the end of this cycle
  boolean running; // true if effect timing state machine is running
  boolean showVisual; // true if the effect visual should be displayed

  // how long a single instance of the effect lasts, in variable speed seconds
  private static final double eventDuration = 2.0;

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

    // initialize state machine for run control
    eventStartTime = 0;
    lastBasis = 0;
    stopRequest = false;
    running = false;
    showVisual = false;

    // SPEED - transit speed of the waves
    controls.setRange(TEControlTag.SPEED, 0.5, -2, 2);
    // SIZE - width of the waves
    controls.setRange(TEControlTag.SIZE, 0.2, 0.05, 0.5);
    // WOW1 - Trigger mode
    controls.setControl(TEControlTag.WOW1, triggerMode);
    // WOW2 - palette color mix level
    controls.setRange(TEControlTag.WOW2, 0.5, 0.0, 1.0);
    // QUANTITY - number of duplicates of the wave
    controls.setRange(TEControlTag.QUANTITY, 1.0, 1.0, 12.0);

    // disable unused controls
    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));

    addCommonControls();

    addShader(
        GLShader.config(lx).withFilename("dual_wave.fs").withUniformSource(this::setUniforms));
  }

  /**
   * Determine if we've recently started a beat. We use this rather than engine.tempo.beat() to give
   * a dependable single trigger close to the start of a beat, with enough flexibility in timing to
   * catch the event, even if we're running slow and miss the exact moment when tempo.basis == 0.
   *
   * @return true if we're near the start of a beat, false otherwise
   */
  boolean getBeatState() {
    double basis = lx.engine.tempo.basis();
    boolean isBeat = (basis < lastBasis);
    lastBasis = basis;
    return isBeat;
  }

  // Run the effect if the WowTrigger button is pressed or if we're
  // in continuous run mode.
  private void setUniforms(GLShader s) {

    // if the effect is running, check event duration to see if we
    // need to retrigger, or just keep showing the visual
    if (running) {
      // if we're in RUN mode, we always show the effect
      if (triggerMode.getEnum() == TriggerMode.RUN) {
        showVisual = true;
      }
      // If we're triggering from a button press, wait for a beat before
      // starting the effect visual.
      else if (getBeatState() && !showVisual) {
        // reset the pattern's clock to sync to the beat
        retrigger(TEControlTag.SPEED);
        eventStartTime = getTime();
        showVisual = true;
        stopRequest = true;
      }
      // if we're in one-shot mode and have reached the end of the cycle,
      // stop the effect and return to idle state
      else if (Math.abs(getTime() - eventStartTime) > eventDuration) {
        if (stopRequest && showVisual) {
          // if the button was released, stop the effect
          // if it's held down, keep running
          if (!getWowTrigger()) {
            running = false;
            showVisual = false;
          } else {
            eventStartTime = getTime();
          }
        }
        // we're in mid-cycle. Continue running the effect
      } else {
        showVisual = true;
      }
    }

    // Send our visual control flag, rather than the raw value from the
    // WowTrigger control to the shader.
    s.setUniform("runEffect", showVisual);
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
      if (!running) {
        this.running = true;
        this.showVisual = false;
        this.stopRequest = false;
      }
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
        // this.running = false;
        this.stopRequest = true;
      } else {
        this.running = true;
      }
    }
  }
}
