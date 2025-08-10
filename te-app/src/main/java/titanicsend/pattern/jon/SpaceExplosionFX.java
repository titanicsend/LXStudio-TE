package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/*
 * SpaceExplosionFX Control Behavior
 *
 * Darkness reigns until you press Wow Trigger.
 *
 * Pressing WowTrigger starts an explosion, which runs at the rate determined
 * by the speed control.
 *
 * If you press and release WowTrigger quickly, you will get a one-shot explosion that
 * will run to completion followed by a return to the idle (darkness) state.
 *
 * If you hold down WowTrigger, you will get multiple beat synced explosions, each one
 * starting on the next beat after the previous explosion completes, and running at
 * the set speed.
 *
 * This means that, by changing speed, you can set up explosions that take multiple
 * beats, but still stay synced to the beat.  And if you want an explosion on every beat,
 * just set speed really high!
 */

@LXCategory("Combo FG")
public class SpaceExplosionFX extends GLShaderPattern {
  double eventStartTime;
  double lastBasis;
  boolean running;

  // how long an explosion lasts, in variable speed seconds
  private static final double eventDuration = 1.15;
  private float explosionSeed = 0.0f;

  // Constructor
  public SpaceExplosionFX(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.setRange(TEControlTag.SPEED, 0.5, -2, 2); // speed
    controls.setExponent(TEControlTag.SPEED, 2.0);

    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));

    addCommonControls();

    addShader(
        GLShader.config(lx)
            .withFilename("space_explosionfx.fs")
            .withUniformSource(this::setUniforms));

    eventStartTime = 0;
    lastBasis = 0;
    running = false;
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
    boolean isBeat = (basis <= lastBasis);
    lastBasis = basis;
    return isBeat;
  }

  // Work to be done per frame
  private void setUniforms(GLShader s) {
    // state of explosion visual
    boolean explode = false;

    // If the WowTrigger button is pressed and we're not
    // already exploding, start the explosions.
    if (getWowTrigger()) {
      if (!running) {
        // reset the pattern's clock to sync to button press
        retrigger(TEControlTag.SPEED);
        eventStartTime = getTime(); // current time, since we just reset the clock
        explosionSeed = (float) Math.random(); // random seed for explosion visuals

        // start explosion state machine and turn on visuals
        running = true;
        explode = true;
      }
    }

    // if explosions are running, check event duration to see if we
    // need to retrigger, or just keep showing the explosion visual
    if (running) {
      if (Math.abs(getTime() - eventStartTime) > eventDuration) {

        if (getWowTrigger()) {
          // wait for a beat to start before retriggering
          if (getBeatState()) {
            // reset the pattern's clock to sync to this beat, and start
            // another explosion
            retrigger(TEControlTag.SPEED);
            eventStartTime = 0;
            explosionSeed = (float) Math.random();
            explode = true;
          }
        } else {
          // button is up, and explosion complete.
          // return to idle state
          running = false;
        }
      } else {
        // continue current explosion
        explode = true;
      }
    }

    // Use iWowTrigger to control display of the explosion visuals.
    // Send our visual flag, rather than the simple button value from the
    // control, to the shader.
    s.setUniform("trigger", explode);
    s.setUniform("explosionSeed", explosionSeed);
  }
}
