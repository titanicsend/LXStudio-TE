package titanicsend.util;

/**
 *  Provides a simple signal lag functionality, smoothing out rapid changes.
 */
public class SignalLag {

  private double laggedBassLevel = 0.0f;
  private double smoothness = 0.9f; // Control lag amount (0.0 - 1.0)

  /**
   *  Constructor to initialize the SignalLag object.
   *  @param smoothness Controls the intensity of the lag effect (values between 0.0 and 1.0).
   */
  public SignalLag(double smoothness) {
    this.smoothness = smoothness;
  }

  /**
   *  Applies the lag effect to a signal value.
   *  @param currentBassLevel The current input signal value.
   *  @return The lagged (smoothed) signal value.
   */
  public double applyLag(double currentBassLevel) {
    laggedBassLevel = smoothness * laggedBassLevel + (1.0f - smoothness) * currentBassLevel;
    return laggedBassLevel;
  }
}