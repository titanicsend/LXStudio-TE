package titanicsend.app.director;

import heronarts.lx.model.LXModel;
import heronarts.lx.parameter.CompoundParameter;

public class Filter {

  public final String path;

  public final CompoundParameter fader;

  public Filter(String path, String label) {
    this.path = path;

    this.fader =
        new CompoundParameter(label, 1).setUnits(CompoundParameter.Units.PERCENT_NORMALIZED);
  }

  /** Apply the filter to the colors array. Subclasses should override and implement. */
  public void run(int[] colors, float master) {}

  /**
   * Called every time the model is changed. Filters should refresh their state so they can run
   * efficiently every frame.
   */
  public void modelChanged(LXModel model) {}
}
