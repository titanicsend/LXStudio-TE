package titanicsend.pattern;

public class TimeAccumulator {
  private double accumulatedMsec;
  private final double threshold;

  public TimeAccumulator(double threshold) {
    this.accumulatedMsec = 0.0;
    this.threshold = threshold;
  }

  public void add(double msec) {
    this.accumulatedMsec += msec;
  }

  public boolean timeToRun() {
    if (this.accumulatedMsec < this.threshold) {
      return false;
    } else {
      this.accumulatedMsec -= this.threshold;
      return true;
    }
  }
}