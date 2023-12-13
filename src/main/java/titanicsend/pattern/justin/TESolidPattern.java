package titanicsend.pattern.justin;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;

@LXCategory(LXCategory.COLOR)
public class TESolidPattern extends TEPerformancePattern {

  public TESolidPattern(LX lx) {
    super(lx);

    controls.markUnused(controls.getLXControl(TEControlTag.SPEED));
    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.SIZE));
    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.BRIGHTNESS));
    controls.markUnused(controls.getLXControl(TEControlTag.EXPLODE));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
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
