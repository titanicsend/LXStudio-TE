package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.effect.LXModelEffect;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEPattern;

@LXCategory("Titanics End")
public class NoGapEffect extends LXModelEffect<TEWholeModel> {

  public final CompoundParameter logFrequency = 
    new CompoundParameter("LogSec", 60, 1, 300)
    .setDescription("Frequency of log entry in seconds")
    .setExponent(2);
  
  public NoGapEffect(LX lx) {
    super(lx);
    addParameter("LogSec", this.logFrequency);
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
	boolean modified = colors[this.model.gapPoint.index] != TEPattern.GAP_PIXEL_COLOR;
	if (modified) {
      // Fix it
      colors[this.model.gapPoint.index] = TEPattern.GAP_PIXEL_COLOR;
	}
  }

}
