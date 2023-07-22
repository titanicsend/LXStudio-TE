package titanicsend.pattern.mf64;

import heronarts.lx.color.LXColor;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;

public abstract class TEMidiFighter64Subpattern {
  protected final TEMidiFighter64DriverPattern driver;
  protected TEWholeModel modelTE;
  protected static final int[] overlayColors = {
      LXColor.rgb(255, 0, 0),
      LXColor.rgb(255, 170, 0),
      LXColor.rgb(255, 255, 0),
      LXColor.rgb(0, 255, 0),
      LXColor.rgb(0, 170, 170),
      LXColor.rgb(0, 0, 255),
      LXColor.rgb(255, 0, 255),
      LXColor.rgb(255, 255, 255),
  };

  protected ButtonColorMgr buttons;

  protected TEMidiFighter64Subpattern(TEMidiFighter64DriverPattern driver) {
    this.driver = driver;
    this.modelTE = this.driver.getModelTE();
    this.buttons = new ButtonColorMgr();
  }

  public abstract void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping);
  public abstract void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping);

  public abstract void run(double deltaMsec, int[] colors);
}
