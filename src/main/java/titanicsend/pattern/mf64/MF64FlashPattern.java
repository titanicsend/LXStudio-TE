package titanicsend.pattern.mf64;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.util.TEColor;

public class MF64FlashPattern extends TEMidiFighter64Subpattern {
  private static final double PERIOD_MSEC = 100.0;
  private static final int[] flashColors = {
          LXColor.rgb(255,0,0),
          LXColor.rgb(255,170,0),
          LXColor.rgb(255,255,0),
          LXColor.rgb(0,255,0),
          LXColor.rgb(0,170,170),
          LXColor.rgb(0,0,255),
          LXColor.rgb(255,0,255)
  };
  private int flashColor = TEColor.TRANSPARENT;
  private double flashCountdown = 0.0;
  private TEWholeModel model;

  public MF64FlashPattern(TEMidiFighter64DriverPattern driver) {
    super(driver);
    this.model = this.driver.getModel();
  }

  @Override
  public void noteReceived(TEMidiFighter64DriverPattern.Mapping mapping) {
    LX.log("note");
    this.flashColor = flashColors[mapping.col];
    this.flashCountdown = PERIOD_MSEC;
  }

  private void paintAll(int colors[], int color) {
    for (LXPoint point : this.model.panelPoints) {
      colors[point.index] = color;
    }
    for (LXPoint point : this.model.edgePoints) {
      colors[point.index] = color;
    }
  }

  @Override
  public void run(double deltaMsec, int colors[]) {
    // If this is the first run since the note was received, paint the color
    if (this.flashColor != TEColor.TRANSPARENT) {
      paintAll(colors, this.flashColor);
      this.flashColor = TEColor.TRANSPARENT;
      return;
    }

    if (this.flashCountdown < 0.0) return;

    this.flashCountdown -= deltaMsec;

    // If the clock just expired, turn off the color
    if (this.flashCountdown <= 0) {
      paintAll(colors, TEColor.TRANSPARENT);
    }
  }
}
