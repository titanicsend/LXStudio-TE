package titanicsend.pattern.mf64;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64FlashPattern extends TEMidiFighter64Subpattern {
  private static final double PERIOD_MSEC = 100.0;
  private static final int[] flashColors = {
          LXColor.rgb(255,0,0),
          LXColor.rgb(255,170,0),
          LXColor.rgb(255,255,0),
          LXColor.rgb(0,255,0),
          LXColor.rgb(0,170,170),
          LXColor.rgb(0,0,255),
          LXColor.rgb(255,0,255),
          LXColor.rgb(255,255,255),
  };
  private int flashColor = TRANSPARENT;
  private boolean flashPending = true;
  private TEWholeModel modelTE;

  public MF64FlashPattern(TEMidiFighter64DriverPattern driver) {
    super(driver);
    this.modelTE = this.driver.getModelTE();
  }

  @Override
  public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
    this.flashColor = flashColors[mapping.col];
    this.flashPending = true;
  }

  @Override
  public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
    this.flashColor = TRANSPARENT;
    this.flashPending = true;
  }

  private void paintAll(int colors[], int color) {
    for (LXPoint point : this.modelTE.panelPoints) {
      colors[point.index] = color;
    }
    for (LXPoint point : this.modelTE.edgePoints) {
      colors[point.index] = color;
    }
  }

  @Override
  public void run(double deltaMsec, int colors[]) {
    if (this.flashPending) {
      paintAll(colors, this.flashColor);
      this.flashPending = false;
    }
  }
}
