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
 * SimpleSolidEdgePattern is a trivial pattern that accepts input to
 * control the output color of just the edges in the model.
 */
@LXCategory("TE Examples")
public class SolidEdge extends TEPattern {

  public final LinkedColorParameter color =
          new LinkedColorParameter("Color")
                  .setDescription("Edge color");

  public SolidEdge(LX lx) {
    super(lx);
    addParameter("color", this.color);
    color.mode.setValue(LinkedColorParameter.Mode.PALETTE);
    color.index.setValue(4);
    clearPixels();
  }

  @Override
  public void run(double deltaMs) {
    int color = this.color.calcColor();
    for (LXPoint point : this.modelTE.edgePoints) {
      colors[point.index] = color;
    }
  }
}
