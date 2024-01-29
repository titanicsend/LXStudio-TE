package titanicsend.ndi;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import me.walkerknapp.devolay.*;

public class NDIEngine extends LXComponent implements LXLoopTask {
  public static final String PATH = "NDIEngine";
  private boolean isInitialized = false;
  private boolean isEnabled = true;


  // TODO - implement this
  private class NDIFinder extends Thread {
    

    public void run() {
      // TODO - implement this
    }
  }




  public void setEnabled(boolean enabled) {
    isEnabled = enabled;
  }

  public NDIEngine(LX lx) {
    Devolay.loadLibraries();

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

      // per frame runtime stuff;
    }
  }
}
