package titanicsend.ndi;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.ObjectParameter;
import java.util.ArrayList;
import java.util.List;
import me.walkerknapp.devolay.*;

public class NDIEngine extends LXComponent implements LXLoopTask {

  public static final String PATH = "ndi";
  private static final String NO_SOURCES = "<NONE FOUND>";
  private static final int SOURCE_REFRESH_INTERVAL = 1000;

  private static NDIEngine current;

  public static NDIEngine get() {
    return current;
  }

  public class Selector extends ObjectParameter<String> {

    public Selector() {
      this("NDI Source");
      selectors.add(this);
    }

    public Selector(String label) {
      super(label, selectorObjects, selectorOptions);
    }

    @Override
    public void dispose() {
      selectors.remove(this);
      super.dispose();
    }
  }

  public final BooleanParameter receiveActive =
      new BooleanParameter("RX Active", true)
          .setMappable(false)
          .setDescription("Whether NDI engine listens for incoming streams");

  private final DevolayFinder finder;
  private double elapsed = SOURCE_REFRESH_INTERVAL;

  // Known sources
  private final List<String> sources = new ArrayList<>();

  // Source Selector parameters that live on other components
  private final List<Selector> selectors = new ArrayList<>();

  // Object and String arrays for the Source Selector parameters
  private String[] selectorObjects = {null};
  private String[] selectorOptions = {NO_SOURCES};

  public NDIEngine(LX lx) {
    current = this;

    // load the NDI libraries and start a finder thread.  Note that this
    // may take a little time to complete, so it must be done before
    // we fire up the loop task.
    Devolay.loadLibraries();
    this.finder = new DevolayFinder();

    lx.engine.registerComponent(PATH, this);
    lx.engine.addLoopTask(this);
  }

  public Selector newSourceSelector() {
    return new Selector();
  }

  /**
   * Connect to the first source found with the given name.
   *
   * @param name
   * @param receiver
   * @return True if the source is available and connection attempt was successful. False if the
   *     source is not available.
   */
  public boolean connectByName(String name, DevolayReceiver receiver) {
    for (DevolaySource source : this.finder.getCurrentSources()) {
      String sourceName = source.getSourceName();
      if (sourceName != null && sourceName.contains(name)) {
        receiver.connect(source);
        return true;
      }
    }
    return false;
  }

  public void loop(double deltaMs) {
    // Refresh list of sources periodically
    this.elapsed += deltaMs;
    if (this.elapsed >= SOURCE_REFRESH_INTERVAL && this.receiveActive.isOn()) {
      this.elapsed = 0;
      updateSources();
    }
  }

  private void updateSources() {
    // Every finder query returns new DevolaySource objects, so we'll just use the strings.
    DevolaySource[] newSources = finder.getCurrentSources();
    boolean changed = false;

    // Add new sources and remove ones that no longer exist
    List<String> toRemove = new ArrayList<>(this.sources);
    for (DevolaySource s : newSources) {
      String sourceName = s.getSourceName();
      if (!toRemove.remove(sourceName)) {
        this.sources.add(sourceName);
        changed = true;
      }
    }
    if (!toRemove.isEmpty()) {
      this.sources.removeAll(toRemove);
      changed = true;
    }

    // Push changes to parameters that live on other components
    if (changed) {
      updateSourceSelectors();
    }
  }

  private void updateSourceSelectors() {
    int numOptions = this.sources.size();
    if (numOptions > 0) {
      this.selectorObjects = new String[numOptions];
      this.selectorOptions = new String[numOptions];
    } else {
      this.selectorObjects = new String[] {null};
      this.selectorOptions = new String[] {NO_SOURCES};
    }

    int i = 0;
    for (String s : this.sources) {
      this.selectorObjects[i] = s;
      this.selectorOptions[i] = s;
      i++;
    }

    // Update all selectors to use the new list of options
    for (Selector selector : this.selectors) {
      // Remember the selected item...
      final String selected = selector.getObject();

      // Update the parameter's list
      selector.setObjects(this.selectorObjects, this.selectorOptions);

      // Restore the selected item
      if (selected != null
          && !selected.equals(selector.getObject())
          && this.sources.contains(selected)) {
        selector.setValue(selected);
      }
    }
  }

  @Override
  public void dispose() {
    // stop the finder thread
    finder.close();

    super.dispose();
  }
}
