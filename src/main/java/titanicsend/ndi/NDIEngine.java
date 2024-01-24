package titanicsend.ndi;

import static com.jogamp.opengl.GL.*;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import heronarts.lx.audio.GraphicMeter;
import java.nio.FloatBuffer;
import titanicsend.pattern.yoffa.shader_engine.ShaderUtils;

public class NDIEngine extends LXComponent implements LXLoopTask {
  public static final String PATH = "NDIEngine";

  // default canvas size
  private static final int xSize = 640;
  private static final int ySize = 480;

  private boolean isRunning = false;

  public NDIEngine(LX lx) {

    // TODO - start NDI finder thread, and provide an API so patterns can
    // TODO - access the list of NDI sources.

    // TODO - we probably need to build a register/unregister API so patterns
    // TODO - can register for NDI sources they want to use
    // TODO - not sure how to best let them get the current frames
    // TODO - do we need to run a thread per source and provide a callback?
    // TODO - or do we just provide a (scaled) buffer that gets updated every frame
    // TODO - for every source that is registered?

    // register NDIEngine so we can access it from patterns.
    lx.engine.registerComponent(PATH, this);
    lx.engine.addLoopTask(this);
  }

  public void loop(double deltaMs) {

    // On first frame...
    if (isRunning == false) {

      // initialize stuff

      // set running flag once initialization is complete
      isRunning = true;
    }

    // On every frame, after initial setup
    if (isRunning) {



      // per frame runtime stuff;
    }
  }
}
