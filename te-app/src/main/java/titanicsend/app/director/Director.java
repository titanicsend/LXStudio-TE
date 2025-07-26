package titanicsend.app.director;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.model.LXModel;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.CompoundParameter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import titanicsend.lasercontrol.TELaserTask;

public class Director extends LXComponent
    implements LX.Listener, LXOscComponent, LX.ProjectListener {

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

  public final TELaserTask laserTask;

  public Director(LX lx) {
    super(lx, "director");
    current = this;

    lx.engine.registerComponent("director", this);

    // LaserTask is a child to ensure proper initialization order and to access the fader parameter
    lx.engine.registerComponent("lasers", this.laserTask = new TELaserTask(lx));

    addFilter(new TagFilter("te", "TE", "te"));
    addFilter(new TagFilter("panels", "Panels", "panel"));
    addFilter(new TagFilter("edges", "Edges", "edge"));
    addFilter(new TagFilter("foh", "FOH", "mothership"));

    Filter beaconsFilter = new DmxFilter("beacons", "Beacons", "beacon");
    addFilter(beaconsFilter);
    this.beaconsFader = beaconsFilter.fader;

    addParameter("master", this.master);

    lx.addListener(this);
    onModelChanged(lx.getModel());
    lx.addProjectListener(this);
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

  @Override
  public void projectChanged(File file, Change change) {
    // Auto-create Director Effect
    if (change == Change.NEW || change == Change.OPEN) {
      DirectorEffect directorEffect = null;

      // Does effect already exist?
      for (LXEffect effect : this.lx.engine.mixer.masterBus.effects) {
        if (effect instanceof DirectorEffect) {
          directorEffect = (DirectorEffect) effect;
          break;
        }
      }

      // Create effect
      if (directorEffect == null) {
        directorEffect = new DirectorEffect(this.lx);
        this.lx.engine.mixer.masterBus.addEffect(directorEffect);
        LX.log("Added DirectorEffect to master channel");
      }

      // Make sure effect is enabled and locked
      if (!directorEffect.enabled.isOn() || !directorEffect.locked.isOn()) {
        // Unlock to toggle enabled state
        directorEffect.locked.setValue(false);
        directorEffect.enabled.setValue(true);
        directorEffect.locked.setValue(true);
      }
    }
  }

  public double getBeaconsLevel() {
    return this.beaconsFader.getNormalized() * this.master.getNormalized();
  }

  @Override
  public void dispose() {
    this.laserTask.dispose();
    lx.removeListener(this);
    super.dispose();
  }
}
