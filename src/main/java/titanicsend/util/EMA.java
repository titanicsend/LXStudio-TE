package titanicsend.util;

/**
 * Exponential Moving Average with adjustable smoothing time
 */
public class EMA {

  private double periodMs;
  private double ema;

  public EMA(double periodMs) {
    this(periodMs, 0);
  }

  public EMA(double periodMs, double initialValue) {
    this.periodMs = periodMs;
    this.ema = initialValue;
  }

  public EMA setPeriod(double periodMs) {
    this.periodMs = periodMs;
    return this;
  }

  public double update(double value, double elapsedMs) {
    if (this.periodMs == 0) {
      this.ema = value;
      return this.ema;
    }

    double alpha = elapsedMs / (periodMs + elapsedMs);
    this.ema = alpha * value + (1 - alpha) * ema;
    return this.ema;
  }

}