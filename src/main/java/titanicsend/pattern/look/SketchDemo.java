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
  static final int MAX_POINTS = 250;
  FloatBuffer gl_segments;

  // IRL, initialize the points array with actual data for the line segments
  // here, we just allocate and zero it.
  float[][] points = new float[MAX_POINTS][2];

  SketchDataManager sketchMgr;
  int currentSketchIdx = 3;
  boolean hasSketchBeenPassed = false;

  // Constructor
  public SketchDemo(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    sketchMgr = SketchDataManager.get();
    int totalSketches = sketchMgr.sketches.size();
    System.out.println("Loaded sketches: "+totalSketches);

    controls.setRange(TEControlTag.SIZE, 0.5, 0.1, 2.0);

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
            if (!hasSketchBeenPassed) {
                SketchDataManager.SketchData currSketch = sketchMgr.sketches.get(currentSketchIdx);
                setUniformPoints(s, currSketch);
                hasSketchBeenPassed = true;
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
}