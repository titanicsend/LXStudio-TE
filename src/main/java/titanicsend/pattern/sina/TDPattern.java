package titanicsend.pattern.sina;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEPerformancePattern;

@LXCategory("AAA")
public class TDPattern extends TEPerformancePattern {



  public TDPattern(LX lx) {
    super(lx);

    addCommonControls();
  }

  @Override
  protected void runTEAudioPattern(double deltaMs) {


    ////// COLORS
    int color1 = calcColor();

    for (LXPoint p : getModel().getPoints()) {
      if (this.modelTE.isGapPoint(p)) {
        continue;
      }

      colors[p.index] = color1;
    }
  }
}
