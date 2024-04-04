package titanicsend.pattern.look;


import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.nio.FloatBuffer;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TE;

@LXCategory("Bogus Test Pattern")
public class SketchDemo extends GLShaderPattern {
  private static final int MAX_POINTS = 250;
  private FloatBuffer gl_segments;

  private float[][] points = new float[MAX_POINTS][2];

  private SketchDataManager sketchMgr;
  private int currentSketchIdx = 3;
  private boolean hasSketchBeenPassed = false;

  private float progress = 0;

  private float cumulativeBassLevel = 0;

  // Constructor
  public SketchDemo(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    sketchMgr = SketchDataManager.get();
    int totalSketches = sketchMgr.sketches.size();
    System.out.println("Loaded sketches: "+totalSketches);

    controls.setRange(TEControlTag.SIZE, 0.5, 0.1, 2.0);

    controls.setRange(TEControlTag.WOW2, 2.0, 0.1, 100.0);

    // set the x-axis near the bottom of the car
    controls.setValue(TEControlTag.YPOS, 0.85);
    // offset the drawing position to mirror on either side of the central opening.
    controls.setValue(TEControlTag.XPOS, 0.5);

    // register common controls with the UI
    addCommonControls();

    // create an n x 2 native FloatBuffer to hold 32-bit float values, so we can
    // hand them off to the GPU.
    this.gl_segments = Buffers.newDirectFloatBuffer(MAX_POINTS * 2 * 4);

    // add the OpenGL shader and its frame-time setup function which,
    // in this case, will copy the current contents of the points array
    // to the native FloatBuffer and send it to the shader as an array uniform.
    addShader(
        "single_line_dynamicdata.fs",
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            float pullback = (float) getControls().getControl(TEControlTag.WOW1).getValue();
            cumulativeBassLevel += bassLevel - pullback;

            float nextDrawingThreshold = (float) getControls().getControl(TEControlTag.WOW2).getValue();
            progress = cumulativeBassLevel / nextDrawingThreshold;

//            progress = (float) getControls().getControl(TEControlTag.WOW1).getValue();

            s.setUniform("progress", progress);
            if (!hasSketchBeenPassed) {
                SketchDataManager.SketchData currSketch = sketchMgr.sketches.get(currentSketchIdx);
                setUniformPoints(s, currSketch);
                hasSketchBeenPassed = true;
            }

            if (progress >= 1.0) {
              progress = 0;
              cumulativeBassLevel = 0;
              swapDrawing();
            }
          }
        });
  }

  // store an x,y point in the native buffer
  void setPoint(int segNo, float x, float y) {
//    TE.log("setPoint %d : %.4f %.4f",segNo,x,y);
    gl_segments.position(segNo * 2);
    gl_segments.put(x);
    gl_segments.put(y);
    gl_segments.rewind();
  }

  // Sends an array of point coordinates to the shader. It's not
  // necessary to call this on every frame, particularly if the number
  // of points is large.  It can be called only when the points array changes.
  void setUniformPoints(GLShader s, SketchDataManager.SketchData data) {
    for (int i = 0; i < data.num_points; i++) {
      setPoint(i, data.points[i][0], data.points[i][1]);
    }
    s.setUniform("points", gl_segments, 2);
    s.setUniform("numPoints", data.num_points);
    s.setUniform("totalLength", data.total_dist);
//    TE.log("setNumPoints %d", data.num_points);
//    TE.log("setTotalDistance %f", data.total_dist);
  }

  void swapDrawing() {
    hasSketchBeenPassed = false;
    int totalSketches = sketchMgr.sketches.size();
    double rand = Math.random();
    currentSketchIdx = (int) Math.floor(rand * totalSketches);
    TE.log("newIDX: %d, (%d * %f)", currentSketchIdx, totalSketches, rand);
  }

  @Override
  protected void onWowTrigger(boolean on) {
    // when the wow trigger button is pressed...
    if (on) {
      swapDrawing();
    }
  }
}