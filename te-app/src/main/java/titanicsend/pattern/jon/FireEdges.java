package titanicsend.pattern.jon;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.nio.FloatBuffer;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Combo FG")
public class FireEdges extends GLShaderPattern {
  static final int MAX_LINE_COUNT = 104;
  int lineCount = 0;
  boolean updateGeometry = true;
  FloatBuffer gl_segments;
  float[][] saved_lines;

  // Constructor
  public FireEdges(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);



    // LEVELREACTIVITY - Amount of "bounce" in reaction to bass stem signal
    controls.setRange(TEControlTag.LEVELREACTIVITY,0.2,0.0, 0.35);
    // WOW1 - How much of the color range of the current palette is
    // used to tint the fire. (0.0 = basically just the first color,
    // 1.0 = all colors in the palette)
    controls.setRange(TEControlTag.WOW1, 0.5, 0., 1.);
    // WOW2 - Controls the mix of palette-tinted fire vs. fire-colored fire.
    controls.setRange(TEControlTag.WOW2, 1., 0., 1.);
    // QUANTITY - HOW MUCH FIRE!!!
    controls.setRange(TEControlTag.QUANTITY, 0.5, 1., 0.2);

    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.SIZE));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    // register common controls with the UI
    addCommonControls();

    // create an n x 4 array, so we can pass line segment descriptors
    // to GLSL shaders.
    // NOTE: This buffer needs to be *exactly* large enough to contain
    // the number of line segments you're using.  No smaller, no bigger.
    this.gl_segments = Buffers.newDirectFloatBuffer(MAX_LINE_COUNT * 4 * 4);

    // buffer to hold line descriptors taken from the vehicle
    saved_lines = new float[MAX_LINE_COUNT][4];

    // NOTE: To add more edges, you need to change LINE_COUNT so the
    // segment buffer will be the right size.
    // lineCount = CarGeometryPatternTools.getPanelConnectedEdges(getModelTE(),".*S.*", saved_lines,
    // MAX_LINE_COUNT);
    lineCount =
        CarGeometryPatternTools.getAllEdgesOnSide(getModelTE(), -1, saved_lines, MAX_LINE_COUNT);

    // add the OpenGL shader and its frame-time setup function
    addShader(
        GLShader.config(lx).withFilename("fireedges.fs").withUniformSource(this::setUniforms));
  }

  private void setUniforms(GLShader s) {
    // Here, we update line segment geometry
    // Shader uniforms associated with a context stay resident
    // on the GPU,so we only need to set them when something changes.

    if (updateGeometry) {
      sendSegments(s, saved_lines, lineCount);
      updateGeometry = false;
    }
  }

  // store segment descriptors in our GL line segment buffer.
  void setUniformLine(int segNo, float x1, float y1, float x2, float y2) {
    // System.out.printf("setLine %d : %.4f %.4f, %.4f %.4f\n",segNo,x1,y1,x2,y2);
    gl_segments.position(segNo * 4);
    gl_segments.put(x1);
    gl_segments.put(y1);
    gl_segments.put(x2);
    gl_segments.put(y2);
    gl_segments.rewind();
  }

  // sends an array of line segments to the shader
  // should be called after all line computation is done,
  // before running the shader
  void sendSegments(GLShader s, float[][] lines, int nLines) {
    for (int i = 0; i < nLines; i++) {
      setUniformLine(i, lines[i][0], lines[i][1], lines[i][2], lines[i][3]);
    }
    s.setUniform("lineCount", nLines);
    s.setUniform("lines", gl_segments, 4);
  }
}
