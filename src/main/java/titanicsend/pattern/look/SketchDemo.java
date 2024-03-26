package titanicsend.pattern.look;


import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.nio.FloatBuffer;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Bogus Test Pattern")
public class SketchDemo extends GLShaderPattern {
  static final int NUM_POINTS = 200;
  FloatBuffer gl_segments;

  // IRL, initialize the points array with actual data for the line segments
  // here, we just allocate and zero it.
  float[][] points = new float[NUM_POINTS][2];

  // Constructor
  public SketchDemo(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // register common controls with the UI
    addCommonControls();

    // create an n x 2 native FloatBuffer to hold 32-bit float values, so we can
    // hand them off to the GPU.
    this.gl_segments = Buffers.newDirectFloatBuffer(NUM_POINTS * 2 * 4);

    // add the OpenGL shader and its frame-time setup function which,
    // in this case, will copy the current contents of the points array
    // to the native FloatBuffer and send it to the shader as an array uniform.
    addShader(
        "sketchy.fs",
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            setUniformPoints(s, points, NUM_POINTS);
          }
        });
  }

  // store an x,y point in the native buffer
  void setPoint(int segNo, float x, float y) {
    // TE.log("setLine %d : %.4f %.4f, %.4f %.4f",segNo,x1,y1,x2,y2);
    gl_segments.position(segNo * 4);
    gl_segments.put(x);
    gl_segments.put(y);
  }

  // Sends an array of point coordinates to the shader. It's not
  // necessary to call this on every frame, particularly if the number
  // of points is large.  It can be called only when the points array changes.
  void setUniformPoints(GLShader s, float[][] points, int nPoints) {
    for (int i = 0; i < nPoints; i++) {
      setPoint(i, points[i][0], points[i][1]);
    }
    gl_segments.rewind();
    s.setUniform("points", gl_segments, 2);

    // n.b. If you ever need to draw fewer than NUM_POINTS points, you might
    // also set a uniform to tell the shader how many points it should draw:
    // s.setUniform("nPoints", nPoints);
  }
}