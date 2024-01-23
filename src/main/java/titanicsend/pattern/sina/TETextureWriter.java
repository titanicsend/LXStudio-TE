package titanicsend.pattern.sina;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;

@LXCategory(LXCategory.COLOR)
public class TETextureWriter extends TEPerformancePattern {

  public TETextureWriter(LX lx) {
    super(lx);

    addCommonControls();
  }

  @Override
  protected void runTEAudioPattern(double deltaMs) {
    int color1 = calcColor();


    for (LXPoint p : getModel().getPoints()) {
      if (this.modelTE.isGapPoint(p)) {
        continue;
      }

      colors[p.index] = color1;
    }
  }
}
