package titanicsend.pattern.cnk;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControl;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.jon._CommonControlGetter;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.SignalLag;

@LXCategory("Combo FG")
public class DotPolka extends GLShaderPattern {

  private static final double SPEED_MIN = 0.0;
  private static final double SPEED_MAX = 2.0;
  private static final int SPEED_STEPS = 16; // Number of steps between min and max
  private static final double[] SPEED_VALUES = generateSpeedValues();
  private static final double SPEED_DEFAULT = 1.0;

  public static final double MIN_DANCE_DURATION = 0.5;
  public static final double MAX_DANCE_DURATION = 0.99;

  // Last speed we set successfully
  private double lastSpeed = SPEED_DEFAULT;
  // Speed that we're trying to set now
  private double nextSpeed = SPEED_DEFAULT;

  // ADSR envelope for iWowTriggerValue
  private final SignalLag wowTriggerEnvelope = new SignalLag();

  public DotPolka(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);
    controls.setRange(TEControlTag.SIZE, 8.0, 20.0, 1.0);
    controls.setRange(TEControlTag.SPIN, 0.0, -0.125, 0.125); // Much gentler spin range
    controls.setRange(TEControlTag.LEVELREACTIVITY, 0.1, 0.0, 1.0);
    controls.setRange(TEControlTag.FREQREACTIVITY, 0.0, 0.0, 1.0);

    controls
        .setRange(TEControlTag.SPEED, SPEED_DEFAULT, SPEED_MIN, SPEED_MAX)
        .setGetterFunction(
            TEControlTag.SPEED,
            new _CommonControlGetter() {
              private double storedSpeed = 0.2;
              private double lastBeatCount = -1.0;

              @Override
              public double getValue(TEControl cc) {
                double iBeatTime = getIBeatTime(lx);
                double currentBeat = Math.floor(iBeatTime);

                double newValue = cc.getValue();
                nextSpeed = newValue;

                // Only allow changes during dance pause window
                if (isDancePaused()) {
                  if (newValue != storedSpeed) {
                    storedSpeed = newValue;
                    lastSpeed = storedSpeed;
                  }
                }
                if (currentBeat > lastBeatCount) {
                  lastBeatCount = currentBeat;
                }

                return storedSpeed;
              }
            });

    controls
        .setRange(TEControlTag.QUANTITY, 1, 1, 4)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER)
        .setGetterFunction(
            TEControlTag.QUANTITY,
            new _CommonControlGetter() {
              private double storedQuantity = 1.0;
              private double lastBeatCount = -1.0;

              @Override
              public double getValue(TEControl cc) {
                double iBeatTime = getIBeatTime(lx);
                double currentBeat = Math.floor(iBeatTime);

                // Only allow changes during dance pause window
                if (isDancePaused()) {
                  double newValue = Math.floor(cc.getValue());
                  if (newValue != storedQuantity) {
                    storedQuantity = newValue;
                  }
                }
                if (currentBeat > lastBeatCount) {
                  lastBeatCount = currentBeat;
                }

                return storedQuantity;
              }
            });

    addCommonControls();
    addShader(GLShader.config(lx).withFilename("dotpolka.fs").withUniformSource(this::setUniforms));
  }

  private void setUniforms(GLShader s) {
    // iBeatTime acts like iTime but counts beats instead of seconds
    double iBeatTime = getIBeatTime(lx);
    s.setUniform("iBeatTime", (float) iBeatTime);

    // Map continuous control speed to discrete beat-synced uniform speed
    double gotSpeed = getSpeed();
    double discreteSpeed = mapSpeedToDiscrete(gotSpeed);
    s.setUniform("iSpeedDiscrete", (float) discreteSpeed);

    double animationSpeed = gotSpeed * getAnimationSpeed(discreteSpeed) / discreteSpeed;
    s.setUniform("iAnimationSpeed", (float) animationSpeed);

    double smoothedTrigger = wowTriggerEnvelope.applyLag(getWowTrigger() ? 1.0 : 0.0);
    s.setUniform("iWowTriggerValue", (float) smoothedTrigger);
  }

  private static double getAnimationSpeed(double speed) {
    return MIN_DANCE_DURATION + (MAX_DANCE_DURATION - MIN_DANCE_DURATION) * (speed / SPEED_MAX);
  }

  private static double[] generateSpeedValues() {
    double[] values = new double[SPEED_STEPS + 1];
    for (int i = 0; i <= SPEED_STEPS; i++) {
      values[i] = SPEED_MIN + (SPEED_MAX - SPEED_MIN) * i / SPEED_STEPS;
    }
    return values;
  }

  private static double getIBeatTime(LX lx) {
    return lx.engine.tempo.beatCount() + lx.engine.tempo.basis();
  }

  /**
   * Check if we're near the end of the beat, when movement has stopped and it's safe to change
   * control values that affect the movement animation.
   */
  private boolean isDancePaused(double speed) {
    double currentSpeed = mapSpeedToDiscrete(speed);

    // When speed is 0 the entire animation is paused, safe to change controls
    if (currentSpeed == 0) {
      return true;
    }

    double effectiveBeatTime = getIBeatTime(lx) * currentSpeed;
    double effectiveBeatFraction =
        effectiveBeatTime - Math.floor(effectiveBeatTime); // fract(effectiveBeatTime)
    return effectiveBeatFraction >= 0.9 * getAnimationSpeed(currentSpeed);
  }

  private boolean isDancePaused() {
    return isDancePaused(nextSpeed) && isDancePaused(lastSpeed);
  }

  /** Map continuous speed value to discrete beat divisions */
  private double mapSpeedToDiscrete(double speed) {
    double normalizedSpeed =
        Math.max(0, Math.min(1, (speed - SPEED_MIN) / (SPEED_MAX - SPEED_MIN)));
    int index = (int) Math.round(normalizedSpeed * (SPEED_VALUES.length - 1));
    return SPEED_VALUES[index];
  }
}
