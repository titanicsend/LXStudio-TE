package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.Tempo;
import heronarts.lx.modulator.LXWaveshape;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.FunctionalParameter;
import heronarts.lx.parameter.TriggerParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderEffect;

@LXCategory("Titanics End")
public class ShakeEffect extends GLShaderEffect {
  private double effectDepth;
  private double lastBasis;
  private boolean triggerRequested = false;
  private boolean isRunning = false;

  public final CompoundParameter speed =
      new CompoundParameter("Speed", 0.5, 1.0, 0.1)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Speed of the effect");

  public final CompoundParameter depth =
      new CompoundParameter("Depth", 0.50)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Depth of the effect");

  public final BooleanParameter tempoSync =
      new BooleanParameter("Sync", false).setDescription("Sync the effect to the engine tempo");

  public final BooleanParameter manualTrigger =
      new BooleanParameter("Manual", true)
          .setDescription("Enable manual triggering w/trigger button");

  public final EnumParameter<Tempo.Division> tempoDivision =
      new EnumParameter<Tempo.Division>("Division", Tempo.Division.WHOLE)
          .setDescription("Tempo division to use in sync mode");

  public final TriggerParameter trigger =
      new TriggerParameter("Trigger", this::triggerListener)
          .setDescription("Shake it NOW!!! (manual sync mode only");

  // sawtooth wave that can range in frequency between .1 and 10hz
  private final SawLFO basis =
      startModulator(
          new SawLFO(
              0,
              1,
              new FunctionalParameter() {
                @Override
                public double getValue() {
                  return 2000 * speed.getValue();
                }
              }));

  private void triggerListener() {
      isRunning = true;
      // in tempo sync mode, the trigger schedules an event on
      // the next cycle start
      triggerRequested = tempoSync.isOn();
      lastBasis = 0;

      // if tempo sync, we wait 'till the next cycle start to trigger
      // if free running, reset sawtooth and trigger immediately
      if (!tempoSync.isOn()) basis.setBasis(0.0);
  }

  private double getBasis() {
    double r;
    double gate = 1.0;

    if (tempoSync.isOn()) {
      r = this.lx.engine.tempo.getBasis(this.tempoDivision.getEnum());
    } else {
      r = basis.getBasis();
    }

    if (manualTrigger.isOn()) {
      gate = 0;
      if (isRunning) {
        gate = (triggerRequested) ? 0 : 1;
        if (r < lastBasis) {
          // If effect was requested but not yet run, we
          // start it on the next cycle.
          // Otherwise, we've successfully completed a cycle.
          // Now, check the trigger state, and return to the idle state
          // if it's off
          if (triggerRequested) {
            triggerRequested = false;
          } else {
            isRunning = trigger.isOn();
            gate = (isRunning) ? 1 : 0;
          }
        }
      }
      lastBasis = r;
    }

    return gate * LXWaveshape.DOWN.compute(r);
  }

  public ShakeEffect(LX lx) {
    super(lx);

    addParameter("speed", this.speed);
    addParameter("depth", this.depth);

    addParameter("tempoSync", this.tempoSync);
    addParameter("tempoDivision", this.tempoDivision);

    addParameter("manualTrigger", this.manualTrigger);
    addParameter("trigger", this.trigger);

    addShader(
        GLShader.config(lx).withFilename("shake_effect.fs").withUniformSource(this::setUniforms));
  }

  private void setUniforms(GLShader shader) {
    double basis;

    // make curve slightly steeper.
    basis = getBasis();
    basis = Math.pow(basis, 5.0);

    shader.setUniform("basis", (float) (basis * effectDepth));
  }

  @Override
  public void run(double deltaMs, double enabledAmount) {
    effectDepth = enabledAmount * depth.getValue();
    super.run(deltaMs, enabledAmount);
  }

  @Override
  public void dispose() {
    super.dispose();
  }
}
