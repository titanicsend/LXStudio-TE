package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import titanicsend.pattern.TEPattern;

@LXCategory("Titanics End")
public class NoGapEffect extends TEEffect {

  public NoGapEffect(LX lx) {
    super(lx);
  }
  
  @Override
  protected void onEnable() {
    // Friendly reminder this will be most effective on master channel
    LXComponent parent = this.getParent();
    if (parent == null || !parent.equals(this.lx.engine.mixer.masterBus)) {
     LX.log("Warning: NoGap Effect is running on a channel that is not master. Gap pixels may not get filtered.");
    }
    super.onEnable();
  }

  @Override
  protected void run(double deltaMs, double enabledAmount) {
    // There is only one LXPoint instance for all gap pixels
    // Is it modified?
    if (colors[this.modelTE.getGapPointIndex()] != TEPattern.GAP_PIXEL_COLOR) {
      // Fix it
      colors[this.modelTE.getGapPointIndex()] = TEPattern.GAP_PIXEL_COLOR;
    }
  }

}
