package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.Tempo;
import heronarts.lx.modulator.LXWaveshape;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.*;
import heronarts.lx.utils.LXUtils;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderEffect;

@LXCategory("Titanics End")
public class ExplodeEffect extends GLShaderEffect {
  double effectDepth;
  private double lastBasis;
  private boolean triggerRequested = false;
  boolean isRunning = false;

  public final ObjectParameter<LXWaveshape> waveshape =
      new ObjectParameter<LXWaveshape>(
          "Shape",
          new LXWaveshape[] {
            LXWaveshape.DOWN, LXWaveshape.UP, LXWaveshape.SIN, LXWaveshape.TRI, LXWaveshape.SQUARE
          });

  public final CompoundParameter speed =
      new CompoundParameter("Speed", 0.05)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setExponent(2)
          .setDescription("Speed of the effect");

  public final CompoundParameter depth =
      new CompoundParameter("Depth", 0.50)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Depth of the effect");

  public final BoundedParameter slope =
      new BoundedParameter("Slope", 5, 1, 15).setDescription("Steepness of effect/time curve");

  public final BooleanParameter tempoSync =
      new BooleanParameter("Sync", true).setDescription("Sync the effect to the engine tempo");

  public final BooleanParameter manualTrigger =
      new BooleanParameter("Manual", false)
          .setDescription("Enable manual triggering w/trigger button");

  public final EnumParameter<Tempo.Division> tempoDivision =
      new EnumParameter<Tempo.Division>("Division", Tempo.Division.WHOLE)
          .setDescription("Tempo division to use in sync mode");

  public final BoundedParameter tempoPhaseOffset =
      new BoundedParameter("Phase Offset", 0)
          .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
          .setDescription("Shifts timing of explosion relative to beat.");

  public final BoundedParameter size =
      new BoundedParameter("Size", 0, 0, 10).setDescription("Explosion block size");

  public final BooleanParameter trigger =
      new BooleanParameter("Trigger", false)
          .setMode(BooleanParameter.Mode.MOMENTARY)
          .setDescription("Explode NOW!!! (manual sync mode only");

  // sawtooth wave that can range in frequency between .1 and 10hz
  private final SawLFO basis =
      startModulator(
          new SawLFO(
              0,
              1,
              new FunctionalParameter() {
                @Override
                public double getValue() {
                  return 1000 / LXUtils.lerp(0.1, 10, speed.getValue());
                }
              }));

  private final LXParameterListener triggerListener =
      (p) -> {
        if (trigger.isOn()) {
          if (manualTrigger.isOn()) {
            isRunning = true;
            // in tempo sync mode, the trigger schedules an event on
            // the next cycle start
            triggerRequested = tempoSync.isOn();
            lastBasis = 0;

            // if tempo sync, we wait 'till the next cycle start to trigger
            // if free running, reset sawtooth and trigger immediately
            if (!tempoSync.isOn()) basis.setBasis(0.0);
          }
        }
      };

  private double getBasis() {
    double r;
    double gate = 1.0;

    if (tempoSync.isOn()) {
      r = this.lx.engine.tempo.getBasis(this.tempoDivision.getEnum());
      r = (r + this.tempoPhaseOffset.getValue()) % 1.;
    } else {
      r = basis.getBasis();
    }

    if (manualTrigger.isOn()) {
      gate = 0;
      if (isRunning) {
        gate = (triggerRequested) ? 0 : 1;
        if (r < lastBasis) {
          // If explosion was requested but not yet run, we
          // start it on the next cycle.
          // Otherwise, we've successfully completed an explosion.
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

    return gate * waveshape.getObject().compute(r);
  }

  public ExplodeEffect(LX lx) {
    super(lx);

    trigger.addListener(triggerListener);

    addParameter("speed", this.speed);
    addParameter("depth", this.depth);
    addParameter("waveshape", this.waveshape);
    addParameter("slope", this.slope);

    addParameter("tempoSync", this.tempoSync);
    addParameter("tempoDivision", this.tempoDivision);
    addParameter("tempoPhaseOffset", this.tempoPhaseOffset);
    addParameter("size", this.size);

    addParameter("manualTrigger", this.manualTrigger);
    addParameter("trigger", this.trigger);

    addShader(
        GLShader.config(lx).withFilename("explode_effect.fs").withUniformSource(this::setUniforms));
  }

  private void setUniforms(GLShader shader) {
    double basis;

    basis = getBasis();
    double exp = slope.getValue();
    basis = (exp != 1) ? Math.pow(basis, exp) : basis;

    shader.setUniform("basis", (float) (basis * effectDepth));

    float granularity = size.getValuef();
    granularity = (granularity > 0) ? 10 * (11 - granularity) : 0;
    shader.setUniform("size", granularity);
  }

  @Override
  public void run(double deltaMs, double enabledAmount) {
    effectDepth = enabledAmount * depth.getValue();
    super.run(deltaMs, enabledAmount);
  }

  @Override
  public void dispose() {
    trigger.removeListener(triggerListener);
    super.dispose();
  }
}
