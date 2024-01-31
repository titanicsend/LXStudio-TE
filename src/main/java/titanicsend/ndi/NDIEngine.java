package titanicsend.ndi;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import me.walkerknapp.devolay.*;

import java.util.ArrayList;

public class NDIEngine extends LXComponent implements LXLoopTask {
  public static final String PATH = "NDIEngine";
  private boolean isInitialized = false;
  private boolean isEnabled = true;

  public DevolaySource[] sources;
  protected DevolayFinder finder;

  public void setEnabled(boolean enabled) {
    isEnabled = enabled;
  }

  public ArrayList<String> getSourceNames() {
    ArrayList<String> sourceNames = new ArrayList<String>();
    for (DevolaySource source : sources) {
      sourceNames.add(source.getSourceName());
    }
    return sourceNames;
  }

  public void printSourceNames() {
    for (DevolaySource source : sources) {
      System.out.println(source.getSourceName());
    }
  }

  /**
   * Connect to the first source found with the given name.
   * @param sourceName
   * @param receiver
   * @return True if the source is available and connection attempt was successful.
   *        False if the source is not available.
   */
  public boolean connectByName(String sourceName,DevolayReceiver receiver) {
    for (DevolaySource source : sources) {
      if (source.getSourceName().equals(sourceName)) {
        receiver.connect(source);
        return true;
      }
    }
    return false;
  }

  /**
   * Connect to the source at the given index.
   * @param index
   * @param receiver
   * @return True if the source is available and connection attempt was successful.
   *        False if the source is not available.
   */
  public boolean connectByIndex(int index, DevolayReceiver receiver) {
    if (index < sources.length) {
      receiver.connect(sources[index]);
      return true;
    }
    return false;
  }

  public NDIEngine(LX lx) {

    // TODO - start NDI finder thread, and provide an API so patterns can
    // TODO - access the list of NDI sources.

    // TODO - we probably need to build a register/unregister API so patterns
    // TODO - can register for NDI sources they want to use
    // TODO - not sure how to best let them get the current frames
    // TODO - do we need to run a thread per source and provide a callback?
    // TODO - or do we just provide a (scaled) buffer that gets updated every frame
    // TODO - for every source that is registered?

    // TODO - also need to notify patterns when a source is added/removed
    // TODO - (we expect video sources to be transient, especially over
    // TODO - the several hour course of a run with patterns being swapped in/out)

    // register NDIEngine so we can access it from patterns.
    lx.engine.registerComponent(PATH, this);
    lx.engine.addLoopTask(this);

    Devolay.loadLibraries();
    finder = new DevolayFinder();
  }

  public void loop(double deltaMs) {

    // if globally disabled, do nothing
    if (!isEnabled) return;

    // On first frame...
    if (isInitialized) {

      // initialize stuff

      // set flag once initialization is complete
      isInitialized = true;
    }
    // On every frame, after initial setup
    else {

      sources = finder.getCurrentSources();
      if (sources.length > 0) {

      }

      // per frame runtime stuff;
    }
  }
}
