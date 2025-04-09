package titanicsend.app.director;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import java.util.ArrayList;
import java.util.List;

public class TagFilter extends Filter {

  private final String tag;

  private final List<LXPoint> points = new ArrayList<LXPoint>();

  public TagFilter(String path, String label, String tag) {
    super(path, label);
    this.tag = tag;
  }

  @Override
  public void run(int[] colors, float master) {
    float fader = this.fader.getValuef() * master;
    if (fader == 1f) {
      return;
    }

    for (LXPoint point : this.points) {
      colors[point.index] = LXColor.scaleBrightness(colors[point.index], fader);
    }
  }

  @Override
  public void modelChanged(LXModel model) {
    this.points.clear();
    for (LXModel sub : model.sub(tag)) {
      this.points.addAll(sub.getPoints());
    }
  }
}
