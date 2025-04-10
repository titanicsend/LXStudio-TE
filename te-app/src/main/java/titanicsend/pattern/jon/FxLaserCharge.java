package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/*
 * Mothership "Charging up the Lasers" effect.
 *
 * All is dark (idle) until you press WowTrigger.
 * Pressing WowTrigger starts a one-shot event, followed by a return to darkness.
 */

@LXCategory("Mothership")
public class FxLaserCharge extends GLShaderPattern {
  double eventStartTime;
  boolean running;

  // Constructor
  public FxLaserCharge(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // for once, start with 1:1 unidirectional time
    controls.setValue(TEControlTag.SPEED, 1.00);
    allowBidirectionalTime(false);

    // Wow1 controls the exponential curve shape
    controls.setRange(TEControlTag.WOW1, 0.6, 0.5, 2.0);

    // Wow2 controls the base event duration in (variable speed) seconds
    controls.setRange(TEControlTag.WOW2, 6.0, 1.0, 12.0);

    // Hide unused controls
    controls.markUnused(controls.getLXControl(TEControlTag.SIZE));
    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));

    addCommonControls();

    addShader(new GLShader(lx, "ms_charge_lasers.fs", getControlData(), "color_noise.png"), setup);

    eventStartTime = 0;
    running = false;
  }

  // Work to be done per frame
  GLShaderFrameSetup setup =
      new GLShaderFrameSetup() {
        @Override
        public void OnFrame(GLShader s) {
          // state of explosion visual
          double progress = 0;
          double eventDuration = 2 * getWow2();

          // If the WowTrigger button is pressed and we're not
          // already running, start the event
          if (getWowTrigger()) {
            if (!running) {
              eventStartTime = getTime();
              running = true;
            }
          }

          // if  running, calculate the progress of the event
          // and stop when it's done
          if (running) {
            double elapsedTime = getTime() - eventStartTime;
            // figure where we are in the charging cycle
            // wow1 is `k` in the exponential curve formula
            progress = Math.exp(-getWow1() * (eventDuration - elapsedTime));
            if (progress >= 1) {
              running = false;
            }
          }

          s.setUniform("progress", (float) progress);
        }
      };
}
