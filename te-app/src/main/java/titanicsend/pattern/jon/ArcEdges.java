package titanicsend.pattern.jon;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import java.nio.FloatBuffer;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Combo FG")
public class ArcEdges extends GLShaderPattern {
  static final int LINE_COUNT = 52;
  boolean updateGeometry = true;
  FloatBuffer gl_segments;
  float[][] saved_lines;

  // Constructor
  public ArcEdges(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // defaults settings for reactivity controls
    controls.setRange(TEControlTag.LEVELREACTIVITY, 0.333, 0.0, 1.0);
    controls.setRange(TEControlTag.FREQREACTIVITY, 0.333, 0.0, 1.0);

    // Noise field magnitude - controls size + density of arcs
    controls.setRange(TEControlTag.SIZE, 0.025, 0.001, 0.08);

    // Controls number of arcs by modifying noise field position offset
    controls.setRange(TEControlTag.QUANTITY, 0.6, 0.72, 0.5);

    // Base width of "lines" drawn on car edges.
    // NOTE: Edge lighting will be modified by audio reactivity. Generally, setting WOW1
    // higher will make the car's edges more visible while slightly magnifying the
    // effect of the audio reactivity controls
    controls.setRange(TEControlTag.WOW1, 0.015, 0.001, 0.04);

    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));

    // register common controls with the UI
    addCommonControls();

    // create an n x 4 array, so we can pass line segment descriptors
    // to GLSL shaders.
    // NOTE: This buffer needs to be *exactly* large enough to contain
    // the number of line segments you're using.  No smaller, no bigger.
    this.gl_segments = Buffers.newDirectFloatBuffer(LINE_COUNT * 4 * 4);

    // buffer to hold line descriptors taken from the vehicle
    saved_lines = new float[LINE_COUNT][4];

    // NOTE: To add more edges, you need to change LINE_COUNT so the
    // segment buffer will be the right size.
    // TODO - eventually, we'll want arcs on the fore/aft car ends too.
    CarGeometryPatternTools.getPanelConnectedEdges(getModelTE(), "^S.*$", saved_lines, LINE_COUNT);

    // add the OpenGL shader and its frame-time setup function
    addShader(
        GLShader.config(lx)
            .withFilename("arcedges.fs")
            .withUniformSource(
                (s) -> {
                  // Here, we update line segment geometry
                  // Shader uniforms associated with a context stay resident
                  // on the GPU,so we only need to set them when something changes.
                  if (updateGeometry) {
                    sendSegments(s, saved_lines, LINE_COUNT);
                    updateGeometry = false;
                  }
                }));
  }

  // store segment descriptors in our GL line segment buffer.
  void setUniformLine(int segNo, float x1, float y1, float x2, float y2) {
    // TE.log("setLine %d : %.4f %.4f, %.4f %.4f",segNo,x1,y1,x2,y2);
    gl_segments.position(segNo * 4);
    gl_segments.put(-x1);
    gl_segments.put(y1);
    gl_segments.put(-x2);
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
    s.setUniform("lines", gl_segments, 4);
  }
}
