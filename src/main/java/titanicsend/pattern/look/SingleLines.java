package titanicsend.pattern.look;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

import java.nio.FloatBuffer;

@LXCategory("Look Shader Patterns")
public class SingleLines extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;
//    static final int LINE_COUNT = 52;
//    FloatBuffer gl_segments;
//    float[][] saved_lines;

    // Constructor
    public SingleLines(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

//        controls.setRange(TEControlTag.SIZE, 1, 5, 0.1);              // scale
//        controls.setRange(TEControlTag.QUANTITY, 0.6, 0.72, 0.35);  // noise field position
//        controls.setRange(TEControlTag.WOW1, 0.025, 0.001, 0.08);     // noise magnitude
//        controls.setRange(TEControlTag.WOW2, 0.008, 0.0005, 0.03);    // edge "line width"

        // register common controls with the UI
        addCommonControls();

        effect = new NativeShaderPatternEffect("single_line.fs",
                new PatternTarget(this));

//        // create an n x 4 array, so we can pass line segment descriptors
//        // to GLSL shaders.
//        // NOTE: This buffer needs to be *exactly* large enough to contain
//        // the number of line segments you're using.  No smaller, no bigger.
//        this.gl_segments = Buffers.newDirectFloatBuffer(LINE_COUNT * 4 * 4);
//
//        // buffer to hold line descriptors taken from the vehicle
//        saved_lines = new float[LINE_COUNT][4];
//
//        // NOTE: To add more edges, you need to change LINE_COUNT so the
//        // segment buffer will be the right size.
//        CarGeometryPatternTools.getPanelConnectedEdges(getModelTE(), "^S.*$", saved_lines, LINE_COUNT);
    }
//
//    // store segment descriptors in our GL line segment buffer.
//    void setUniformLine(int segNo, float x1, float y1, float x2, float y2) {
//        //TE.log("setLine %d : %.4f %.4f, %.4f %.4f",segNo,x1,y1,x2,y2);
//        gl_segments.position(segNo * 4);
//        gl_segments.put(-x1);
//        gl_segments.put(y1);
//        gl_segments.put(-x2);
//        gl_segments.put(y2);
//        gl_segments.rewind();
//    }

//    // sends an array of line segments to the shader
//    // should be called after all line computation is done,
//    // before running the shader
//    void sendSegments(float[][] lines, int nLines) {
//        for (int i = 0; i < nLines; i++) {
//            setUniformLine(i, lines[i][0], lines[i][1], lines[i][2], lines[i][3]);
//        }
//        shader.setUniform("lines", gl_segments, 4);
//    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
//        // send line segment array data
//        sendSegments(saved_lines, LINE_COUNT);

        // run the shader
        effect.run(deltaMs);
    }

    @Override
    // THIS IS REQUIRED if you're not using ConstructedPattern!
    // Initialize the NativeShaderPatternEffect and retrieve the native shader object
    // from it when the pattern becomes active
    public void onActive() {
        super.onActive();
        effect.onActive();
        shader = effect.getNativeShader();
    }
}
