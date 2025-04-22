package titanicsend.ndi;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import java.util.ArrayList;
import me.walkerknapp.devolay.*;
import titanicsend.util.TE;

public class NDIEngine extends LXComponent implements LXLoopTask {
  public static final String PATH = "NDIEngine";
  private static NDIEngine current;

  private boolean isInitialized = false;
  private boolean isEnabled = true;
  private long time = 0;
  private static final int sourceRefreshInterval = 1000;

  public DevolaySource[] sources = new DevolaySource[0];
  protected DevolayFinder finder;

  public static NDIEngine get() {
    return current;
  }

  public void setEnabled(boolean enabled) {
    isEnabled = enabled;
  }

  public ArrayList<String> getSourceNames() {
    // TODO: This should be refreshed when sources is updated, not on every method call.
    ArrayList<String> sourceNames = new ArrayList<String>();
    for (DevolaySource source : sources) {
      sourceNames.add(source.getSourceName());
    }
    return sourceNames;
  }

  public void printSourceNames() {
    for (DevolaySource source : sources) {
      TE.log("NDI Source: %s", source.getSourceName());
    }
  }

  /**
   * Connect to the first source found with the given name.
   *
   * @param sourceName
   * @param receiver
   * @return True if the source is available and connection attempt was successful. False if the
   *     source is not available.
   */
  public boolean connectByName(String sourceName, DevolayReceiver receiver) {
    for (DevolaySource source : sources) {
      String name = source.getSourceName();
      if (name != null && name.contains(sourceName)) {
        receiver.connect(source);
        return true;
      }
    }
    return false;
  }

  /**
   * Connect to the source at the given index.
   *
   * @param index
   * @param receiver
   * @return True if the source is available and connection attempt was successful. False if the
   *     source is not available.
   */
  public boolean connectByIndex(int index, DevolayReceiver receiver) {
    if (receiver == null) {
      return false;
    }

    if (sources != null && index < sources.length) {
      receiver.connect(sources[index]);
      return true;
    }
    receiver.connect(null);
    return false;
  }

  public NDIEngine(LX lx) {
    current = this;

    // load the NDI libraries and start a finder thread.  Note that this
    // may take a little time to complete, so it must be done before
    // we fire up the loop task.
    Devolay.loadLibraries();
    finder = new DevolayFinder();

    // register NDIEngine so we can access it from patterns.
    lx.engine.registerComponent(PATH, this);
    lx.engine.addLoopTask(this);
  }

  public void loop(double deltaMs) {

    // if globally disabled, do nothing
    if (!isEnabled) return;

    // On first frame...
    if (!isInitialized) {

      // initialize stuff

      // set flag once initialization is complete
      isInitialized = true;
    }
    // Once initial setup is complete, refresh list of sources
    // periodically.
    else {
      if (finder != null) {
        if (System.currentTimeMillis() - time > sourceRefreshInterval) {
          sources = finder.getCurrentSources();
          time = System.currentTimeMillis();
        }
      }
      // additional per frame runtime stuff, TBD as needed;
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    // stop the finder thread
    if (finder != null) {
      finder.close();
    }
  }
}
