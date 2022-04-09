package titanicsend.pattern.pixelblaze;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEPattern;

public class PixelblazePattern extends TEPattern {
  private Wrapper wrapper;
//  private static String PB_CLASS = "firework dust";
  private static String PB_CLASS = "test";

  public PixelblazePattern(LX lx) {
    super(lx);

    try {
      wrapper = Wrapper.fromResource(PB_CLASS, model.points, colors);
    } catch (Exception e) {
      e.printStackTrace();
      LX.error(e);
    }
  }

  public void run(double deltaMs) {
    if (wrapper == null)
      return;
    try {
      wrapper.reloadIfNecessary();
      wrapper.render(deltaMs);
//      wrapper.beforeRender(deltaMs);
//
//      //TODO scale LXpoints to world units based on boundaryPoints. e.g. boundaryPoints.minXBoundaryPoint.x
//
//      for (LXPoint point : this.model.points) {
//        wrapper.render(point, colors);
//      }

    } catch (Exception e) {
      e.printStackTrace();
      LX.error(e);
      return;
    }

  }
}