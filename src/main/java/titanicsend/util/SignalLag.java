package titanicsend.util;

/**
 * Provides a simple signal lag functionality with separate attack and release controls.
 */
public class SignalLag {
  private double laggedSignal = 0.0f;
  private double attackSmoothness = 0.9f;
  private double releaseSmoothness = 0.3f;

  /**
   * Constructor to initialize the SignalLag object.
   *
   * @param attackSmoothness Controls the speed of the lag when the signal increases (0.0 - 1.0)
   * @param releaseSmoothness Controls the speed of the lag when the signal decreases (0.0 - 1.0)
   */
  public SignalLag(double attackSmoothness, double releaseSmoothness) {
    this.attackSmoothness = attackSmoothness;
    this.releaseSmoothness = releaseSmoothness;
  }

  /**
   * Applies the lag effect to a signal value, using attack and release parameters.
   *
   * @param currentSignal The current input signal value.
   * @return The lagged (smoothed) signal value.
   */
  public double applyLag(double currentSignal) {
    if (currentSignal > laggedSignal) {
      laggedSignal = attackSmoothness * laggedSignal + (1.0f - attackSmoothness) * currentSignal;
    } else {
      laggedSignal = releaseSmoothness * laggedSignal + (1.0f - releaseSmoothness) * currentSignal;
    }
    return laggedSignal;
  }
}