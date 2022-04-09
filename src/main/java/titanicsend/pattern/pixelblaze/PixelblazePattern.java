package titanicsend.pattern.pixelblaze;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEPattern;

import javax.script.Invocable;
import java.io.File;

public class PixelblazePattern extends TEPattern {
  private Wrapper wrapper;
//  private static String PB_CLASS = "firework dust";
  private static String PB_CLASS = "test";

  public PixelblazePattern(LX lx) {
    super(lx);

    try {
      wrapper = Wrapper.fromResource(PB_CLASS, this.model.points.length);
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
      wrapper.beforeRender(deltaMs);

      //TODO scale LXpoints to world units based on boundaryPoints. e.g. boundaryPoints.minXBoundaryPoint.x

      for (LXPoint point : this.model.points) {
        Glue.reset();
        wrapper.render(point);
        colors[point.index] = Glue.color;
      }

    } catch (Exception e) {
      e.printStackTrace();
      LX.error(e);
      return;
    }

  }
}