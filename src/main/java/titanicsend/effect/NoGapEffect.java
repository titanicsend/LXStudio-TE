package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.effect.LXModelEffect;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEWholeModel;
import titanicsend.util.TEColor;

@LXCategory("Titanics End")
public class NoGapEffect extends LXModelEffect<TEWholeModel> {

  public NoGapEffect(LX lx) {
    super(lx);
  }

  @Override
  protected void run(double deltaMs, double enabledAmount) {
    for (LXPoint p : this.model.points) {
      if (this.model.isGapPoint(p)) {
        colors[p.index] = TEColor.TRANSPARENT;                
      }          
    }
  }
}
