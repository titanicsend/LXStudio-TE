package titanicsend.app.director;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.model.LXModel;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.CompoundParameter;

public class Director extends LXComponent implements LX.Listener, LXOscComponent {

  private static Director current;

  public static Director get() {
    return current;
  }

  private final List<Filter> mutableFilters = new ArrayList<Filter>();
  public final List<Filter> filters = Collections.unmodifiableList(this.mutableFilters);

  public final CompoundParameter master =
    new CompoundParameter("Master", 1)
      .setUnits(CompoundParameter.Units.PERCENT_NORMALIZED)
      .setDescription("Top fader, dims all components");

  private final CompoundParameter beaconsFader;

  public Director(LX lx) {
    super(lx, "director");
    current = this;

    lx.engine.registerComponent("director", this);

    addFilter(new TagFilter("te", "TE", "te"));
    addFilter(new TagFilter("panels", "Panels", "panel"));
    addFilter(new TagFilter("edges", "Edges", "edge"));
    addFilter(new TagFilter("foh", "FOH", "mothership"));
    addFilter(new Filter("lasers", "Lasers"));

    Filter beaconsFilter = new DmxFilter("beacons", "Beacons", "beacon");
    addFilter(beaconsFilter);
    this.beaconsFader = beaconsFilter.fader;

    addParameter("master", this.master);

    lx.addListener(this);
    onModelChanged(lx.getModel());
  }

  private void addFilter(Filter filter) {
    this.mutableFilters.add(filter);
    addParameter(filter.path, filter.fader);
  }

  @Override
  public void modelGenerationChanged(LX lx, LXModel model) {
    onModelChanged(model);
  }

  private void onModelChanged(LXModel model) {
    for (Filter filter : this.filters) {
      filter.modelChanged(model);
    }
  }

  public double getBeaconsLevel() {
    return this.beaconsFader.getNormalized() * this.master.getNormalized();
  }

  @Override
  public void dispose() {
    lx.removeListener(this);
    super.dispose();
  }
}