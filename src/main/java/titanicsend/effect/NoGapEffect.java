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
  
  private double lastLogTime = 0;
  private int frameTotal = 0;
  private int frameGap = 0;
  
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
    resetCounters();
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
      this.frameGap++;
	}
    this.frameTotal++;
    
    // Log on regular interval
    double logElapsed = (this.lx.engine.nowMillis - this.lastLogTime) / 1000;
    if (logElapsed > this.logFrequency.getValue()) {
      if (this.frameGap > 0 ) {
        double percentFrames = (double)frameGap / (double)frameTotal * 100;        
        LX.log("Warning: " + 
          String.format("%,.1f", percentFrames) + "% of " + this.frameTotal + " frames in the last " + 
          String.format("%,.1f", logElapsed) + " seconds contained modified gap pixels.");
      } else {
        LX.log("Gap pixels looking good for last " + String.format("%,.1f", logElapsed) + " seconds!");          
      }      
      resetCounters();
    }
  }
  
  private void resetCounters() {
      this.lastLogTime = this.lx.engine.nowMillis;
      this.frameTotal = 0;
      this.frameGap = 0;      
  }
}
