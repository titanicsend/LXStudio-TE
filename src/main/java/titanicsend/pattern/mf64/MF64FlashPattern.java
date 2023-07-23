package titanicsend.pattern.mf64;

import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEMidiFighter64DriverPattern;

import static titanicsend.util.TEColor.TRANSPARENT;
import static titanicsend.util.TEColor.reAlpha;

public class MF64FlashPattern extends TEMidiFighter64Subpattern {
  private boolean active;
  private int refCount;

  public MF64FlashPattern(TEMidiFighter64DriverPattern driver) {
    super(driver);
    active = false;
    refCount = 0;
  }

  @Override
  public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
    buttons.addButton(mapping.col, overlayColors[mapping.col]);
    refCount++;
    this.active = true;
  }

  @Override
  public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
    buttons.removeButton(mapping.col);
    refCount--;
    if (refCount == 0) this.active = false;
  }

  private void paintAll(int color) {
    // for flash, we're going to reduce alpha just slightly to
    // 1 - keep the car from ever going 100% white and
    // 2 - make it possible for other patterns to draw over the flash
    // while the button is held down, which gives them the ability
    // to roughly control the background color.
    color = reAlpha(color, 240);

    for (LXPoint point : this.modelTE.getPoints()) {
      setColor(point.index, color);
    }
  }

  @Override
  public void run(double deltaMsec) {
    if (active) {
      paintAll(buttons.getCurrentColor());
    }
  }
}
