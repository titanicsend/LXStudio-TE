package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BooleanParameter;
import titanicsend.pattern.TEPattern;

@LXCategory("Titanics End")
public class NoGapEffect extends TEEffect {

  public final BooleanParameter showGapPixels =
      new BooleanParameter("Show Gaps").setDescription("Colors gap pixels red so they are visible");

  public NoGapEffect(LX lx) {
    super(lx);
    addParameter("Show", showGapPixels);
  }

  @Override
  protected void onEnable() {
    // Friendly reminder this will be most effective on master channel
    LXComponent parent = this.getParent();
    if (parent == null || !parent.equals(this.lx.engine.mixer.masterBus)) {
      LX.warning(
          "NoGap Effect is running on a channel that is not master. Gap pixels may not get"
              + " filtered.");
    }
    super.onEnable();
  }

  @Override
  protected void run(double deltaMs, double enabledAmount) {
    // There is only one LXPoint instance for all gap pixels
    colors[this.modelTE.getGapPointIndex()] =
        showGapPixels.isOn() ? LXColor.RED : TEPattern.GAP_PIXEL_COLOR;
  }
}
