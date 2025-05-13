package titanicsend.pattern.look;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.nio.FloatBuffer;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Bogus Test Pattern")
public class SketchStem extends GLShaderPattern {
  private static final int MAX_POINTS = 250;
  private FloatBuffer gl_segments;
  private SketchDataManager sketchMgr;
  private int currSketchIdx = 3;
  private int prevSketchIdx = 2;
  private boolean hasSketchBeenPassed = false;
  private float progress = 0;

  private float normalizedLevelCumulative = 0;
  private float bassLevelCumulative = 0;

  //  private final SignalLogger signalLogger;

  // Constructor
  public SketchStem(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    sketchMgr = SketchDataManager.get();
    int totalSketches = sketchMgr.sketches.size();
    System.out.println("Loaded sketches: " + totalSketches);

    controls.setRange(TEControlTag.SIZE, 0.87, 0.1, 2.0);

    //    // how much progress is added by bass levels
    //    controls.setValue(TEControlTag.LEVELREACTIVITY, 0.25);

    controls.setRange(TEControlTag.SPEED, 0.03, -0.5, 0.5);

    // "next drawing threshold": how much total progress needs to be made before switching drawings
    controls.setRange(TEControlTag.WOW2, 20.0, 0.5, 50.0);

    // "pullback": how quickly progress is subtracted when no bass is present
    controls.setRange(TEControlTag.WOW1, 0.02, 0.0, 1.0);

    // offset the drawing position to mirror on either side of the central opening.
    controls.setValue(TEControlTag.XPOS, 0.53);

    // set the x-axis near the bottom of the car
    controls.setValue(TEControlTag.YPOS, 0.85);

    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));

    // register common controls with the UI
    addCommonControls();

    // create an n x 2 native FloatBuffer to hold 32-bit float values, so we can
    // hand them off to the GPU.
    this.gl_segments = Buffers.newDirectFloatBuffer(MAX_POINTS * 2 * 4);

    //    List<String> signalNames = Arrays.asList(
    //        "bassLevel",
    //        "bassLevelCumulative",
    //        "squareLevel",
    //        "squareLevelCumulative",
    //        "normalizedLevel",
    //        "normalizedLevelCumulative",
    //        "peakLevel",
    //        "peakLevelCumulative",
    //        "pullback",
    //        "progress",
    //        "nextDrawingThreshold"
    //    );
    //    signalLogger = new SignalLogger(signalNames, "Logs/signal_data.csv");
    //    signalLogger.startLogging(10);

    // add the OpenGL shader and its frame-time setup function which,
    // in this case, will copy the current contents of the points array
    // to the native FloatBuffer and send it to the shader as an array uniform.
    addShader(
        GLShader.config(lx)
            .withFilename("single_line_dynamicdata.fs")
            .withUniformSource(this::setUniforms));
  }

  private void setUniforms(GLShader s) {
    float currSpeed = (float) getControls().getControl(TEControlTag.SPEED).getValue();
    float pullback = (float) getControls().getControl(TEControlTag.WOW1).getValue();
    float nextDrawingThreshold = (float) getControls().getControl(TEControlTag.WOW2).getValue();

    normalizedLevelCumulative += currSpeed;

    s.setUniform("currProgress", progress);
    if (!hasSketchBeenPassed) {
      SketchDataManager.SketchData currSketch = sketchMgr.sketches.get(currSketchIdx);
      SketchDataManager.SketchData prevSketch = sketchMgr.sketches.get(prevSketchIdx);
      setUniformPoints(s, currSketch, "curr");
      setUniformPoints(s, prevSketch, "prev");
      hasSketchBeenPassed = true;
    }

    if (progress > -0.1) {
      normalizedLevelCumulative -= pullback;
    }
    progress = normalizedLevelCumulative / nextDrawingThreshold;

    if (progress >= 1.0) {
      swapDrawing();
    } else if (progress <= -0.1) {
      swapDrawing();
    }
  }

  @Override
  protected void onWowTrigger(boolean on) {
    if (on) {
      swapDrawing();
    }
  }

  private void swapDrawing() {
    progress = 0;
    normalizedLevelCumulative = 0;
    hasSketchBeenPassed = false;
    prevSketchIdx = currSketchIdx;
    currSketchIdx = (int) Math.floor(Math.random() * sketchMgr.sketches.size());
  }

  // store an x,y point in the native buffer
  private void setPoint(int segNo, float x, float y) {
    gl_segments.position(segNo * 2);
    gl_segments.put(x);
    gl_segments.put(y);
    gl_segments.rewind();
  }

  // Sends an array of point coordinates to the shader. It's not
  // necessary to call this on every frame, particularly if the number
  // of points is large.  It can be called only when the points array changes.
  private void setUniformPoints(GLShader s, SketchDataManager.SketchData data, String prefix) {
    for (int i = 0; i < data.num_points; i++) {
      setPoint(i, data.points[i][0], data.points[i][1]);
    }
    s.setUniform(prefix + "Points", gl_segments, 2);
    s.setUniform(prefix + "Count", data.num_points);
    s.setUniform(prefix + "Length", data.total_dist);
  }
}
