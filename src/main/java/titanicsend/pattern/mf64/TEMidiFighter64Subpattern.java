package titanicsend.pattern.mf64;

import heronarts.lx.LX;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.TEPattern;

public abstract class TEMidiFighter64Subpattern {
  public final TEMidiFighter64DriverPattern driver;
  protected TEMidiFighter64Subpattern(TEMidiFighter64DriverPattern driver) {
    this.driver = driver;
  }

  public abstract void noteReceived(TEMidiFighter64DriverPattern.Mapping mapping);

  public abstract void run(double deltaMsec, int[] colors);
}
