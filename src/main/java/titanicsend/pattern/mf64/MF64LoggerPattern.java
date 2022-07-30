package titanicsend.pattern.mf64;

import heronarts.lx.LX;
import titanicsend.pattern.TEMidiFighter64DriverPattern;

public class MF64LoggerPattern extends TEMidiFighter64Subpattern {
  public MF64LoggerPattern(TEMidiFighter64DriverPattern driver) {
    super(driver);
  }

  @Override
  public void noteReceived(TEMidiFighter64DriverPattern.Mapping mapping) {
    String pageStr = mapping.page == TEMidiFighter64DriverPattern.Mapping.Page.LEFT ? "left" : "right";
    LX.log("MIDI Fighter page=" + pageStr + " row=" + mapping.row + " col=" + mapping.col);
  }

  @Override
  public void run(double deltaMsec) {

  }
}
