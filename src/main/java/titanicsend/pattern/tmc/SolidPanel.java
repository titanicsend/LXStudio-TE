/**
 * @author Travis Cline <travis.cline@gmail.com>
 */

package titanicsend.pattern.tmc;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEPattern;

/**
 * SolidPanel is a trivial pattern that accepts input to
 * control the output color of just the panels in the model.
 */
@LXCategory("TE Examples")
public class SolidPanel extends TEPattern {

  public final LinkedColorParameter color =
          new LinkedColorParameter("Color")
                  .setDescription("Panel color");

  public SolidPanel(LX lx) {
    super(lx);
    addParameter("color", this.color);
    color.mode.setValue(LinkedColorParameter.Mode.PALETTE);
    color.index.setValue(5);
    clearPixels();
  }

  @Override
  public void run(double deltaMs) {
    int color = this.color.calcColor();
    for (LXPoint point : this.model.panelPoints) {
      colors[point.index] = color;
    }
  }
}
