package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

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
public class SpaceExplosionFX extends TEPerformancePattern {

    NativeShaderPatternEffect effect;
    NativeShader shader;
    double eventStartTime;
    double elapsedTime;
    double lastBasis;
    boolean running;

    // how long an explosion lasts, in variable speed seconds
    static final double eventDuration = 0.75;

    // Constructor
    public SpaceExplosionFX(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        markUnusedControl(TEControlTag.SIZE);
        markUnusedControl(TEControlTag.QUANTITY);
        markUnusedControl(TEControlTag.WOW1);


        controls.setRange(TEControlTag.SPEED, 0, -2, 2); // speed
        controls.setExponent(TEControlTag.SPEED, 2.0);
        controls.setValue(TEControlTag.SPEED, 0.5);

        addCommonControls();

        effect = new NativeShaderPatternEffect("space_explosionfx.fs",
            new PatternTarget(this));

        eventStartTime = 0;
        lastBasis = 0;
        running = false;
    }

    /**
      Determine if we've recently started a beat.
      We use this rather than engine.tempo.beat() to give a dependable single
      trigger close to the start of a beat, with enough flexibility in timing
      to catch the event, even if we're running slow and miss the exact moment
      when tempo.basis == 0.
      @return true if we're near the start of a beat, false otherwise
     */
    boolean getBeatState() {
      double basis = lx.engine.tempo.basis();
      boolean isBeat = (basis <= lastBasis);
      lastBasis = basis;
      return isBeat;
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        // state of explosion visual
        boolean explode = false;

        // If the WowTrigger button is pressed and we're not
        // already exploding, start the explosions.
        if (getWowTrigger()) {
            if (!running) {
                // reset the pattern's clock to sync to button press
                retrigger(TEControlTag.SPEED);
                eventStartTime = 0;  // current time, since we just reset the clock

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
                       explode = true;
                   }
               }
               else {
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
        shader.setUniform("iWowTrigger", explode);

        // run the shader
        effect.run(deltaMs);
    }

    @Override
    protected void onWowTrigger(boolean on) {

    }

    @Override
    // THIS IS REQUIRED if you're not using ConstructedPattern!
    // Initialize the NativeShaderPatternEffect and retrieve the native shader object
    // from it when the pattern becomes active
    public void onActive() {
        effect.onActive();
        shader = effect.getNativeShader();
    }

}
