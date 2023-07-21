package titanicsend.pattern.mf64;

import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEMidiFighter64DriverPattern;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64FlashPattern extends TEMidiFighter64Subpattern {
  private int overlayColor = TRANSPARENT;
  private boolean flashPending = true;

  public MF64FlashPattern(TEMidiFighter64DriverPattern driver) {
    super(driver);
  }

  @Override
  public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
    this.overlayColor = overlayColors[mapping.col];
    this.flashPending = true;
  }

  @Override
  public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
    this.overlayColor = TRANSPARENT;
    this.flashPending = true;
  }

  private void paintAll(int[] colors, int color) {
    for (LXPoint point : this.modelTE.panelPoints) {
      colors[point.index] = color;
    }
    for (LXPoint point : this.modelTE.edgePoints) {
      colors[point.index] = color;
    }
  }

  @Override
  public void run(double deltaMsec, int[] colors) {
    if (this.flashPending) {
      paintAll(colors, this.overlayColor);
      this.flashPending = false;
    }
  }
}
