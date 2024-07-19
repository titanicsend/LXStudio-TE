package titanicsend.app.director;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.CompoundParameter;

public class Director extends LXComponent implements LX.Listener, LXOscComponent {

  public static class Filter {

    private final String path;

    public final CompoundParameter fader;

    public Filter(String path, String label) {
      this.path = path;

      this.fader = new CompoundParameter(label, 1)
        .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED);
    }

    /**
     * Apply the filter to the colors array. Subclasses should override and implement.
     */
    public void run(int[] colors, float main) { }

    /**
     * Called every time the model is changed. Filters should refresh their
     * state so they can run efficiently every frame.
     */
    public void modelChanged(LXModel model) { }
  }

  public static class TagFilter extends Filter {

    private final String tag;

    private final List<LXPoint> points = new ArrayList<LXPoint>();

    public TagFilter(String path, String label, String tag) {
      super(path, label);
      this.tag = tag;
    }

    @Override
    public void run(int[] colors, float main) {
      float fader = this.fader.getValuef() * main;
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

  public static class DmxFilter extends Filter {

    private final String tag;

    public DmxFilter(String path, String label, String tag) {
      super(path, label);
      this.tag = tag;
    }

    public void run(int[] colors) {
      // TODO: scale the dimmer for DMX fixtures
    }

    public void modelChanged(LXModel model) {
      // TODO: find matching DMX fixtures
    }
  }

  public static Director current;

  private final List<Filter> mutableFilters = new ArrayList<Filter>();
  public final List<Filter> filters = Collections.unmodifiableList(this.mutableFilters);

  public final CompoundParameter main =
    new CompoundParameter("Main", 1)
      .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
      .setDescription("Top fader, dims all components");

  public Director(LX lx) {
    super(lx, "director");
    current = this;

    lx.engine.registerComponent("director", this);

    addFilter(new TagFilter("te", "TE", "te"));
    addFilter(new TagFilter("panels", "Panels", "panel"));
    addFilter(new TagFilter("edges", "Edges", "edge"));
    addFilter(new TagFilter("foh", "FOH", "mothership"));
    addFilter(new Filter("lasers", "Lasers"));
    addFilter(new DmxFilter("beacons", "Beacons", "beacon"));

    addParameter("main", this.main);

    lx.addListener(this);
    onModelChanged(lx.getModel());
  }

  private void addFilter(Filter filter) {
    this.mutableFilters.add(filter);
    addParameter(filter.path, filter.fader);
  }

  public void modelGenerationChanged(LX lx, LXModel model) {
    onModelChanged(model);
  }

  private void onModelChanged(LXModel model) {
    for (Filter filter : this.filters) {
      filter.modelChanged(model);
    }
  }

  @Override
  public void dispose() {
    lx.removeListener(this);
    super.dispose();
  }
}