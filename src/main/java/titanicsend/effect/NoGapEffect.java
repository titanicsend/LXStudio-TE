package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.BooleanParameter;
import titanicsend.pattern.TEPattern;

@LXCategory("Titanics End")
public class NoGapEffect extends TEEffect {

  private static long FLASH_DURATION_MILLIS = 1000;
  private static long TIMESTAMP = System.currentTimeMillis();

  public final BooleanParameter showGapPixels = new BooleanParameter("Flash Gaps")
                  .setDescription("Blinks gap pixels red so they are visible");

  public NoGapEffect(LX lx) {
    super(lx);
    addParameter("Flash Gaps", showGapPixels);
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
    colors[this.modelTE.getGapPointIndex()] = showGapPixels.isOn() && flashOn() ? LXColor.RED
            : TEPattern.GAP_PIXEL_COLOR;
  }

  private boolean flashOn() {
    return (System.currentTimeMillis() - TIMESTAMP) / FLASH_DURATION_MILLIS % 2 == 0;
  }

}
