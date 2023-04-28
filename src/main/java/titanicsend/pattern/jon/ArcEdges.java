package titanicsend.pattern.jon;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.pattern.yoffa.shader_engine.ShaderOptions;
import titanicsend.util.TE;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Set;

@LXCategory("Native Shaders Panels")
public class ArcEdges extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;
    static final int LINE_COUNT = 52;
    FloatBuffer gl_segments;
    float[][] saved_lines;
    float[][] working_lines;
    float[][] line_velocity;


    // Constructor
    public ArcEdges(LX lx) {
        super(lx);

        // create new effect with alpha on and no automatic
        // parameter uniforms

        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        // TODO - Controls not yet tuned.  I just want to make sure this beast will actually
        // TODO - work on a Mac before proceeding.
        controls.setRange(TEControlTag.SIZE,0.008,0.0005,0.02);    // edge "line size"
        controls.setRange(TEControlTag.QUANTITY,0.675,0.72,0.35);  // noise field position
        controls.setRange(TEControlTag.WOW1,0.025,0.001,0.06);     // noise magnitude

        // register common controls with the UI
        addCommonControls();

        effect = new NativeShaderPatternEffect("arcedges.fs",
                PatternTarget.allPointsAsCanvas(this), options);

        // create an n x 4 array, so we can pass line segment descriptors
        // to GLSL shaders.
        // NOTE: This buffer needs to be *exactly* large enough to contain
        // the number of line segments you're using.  No smaller, no bigger.
        this.gl_segments = Buffers.newDirectFloatBuffer(LINE_COUNT * 4 * 4);

        // buffer to hold line descriptors taken from the vehicle
        saved_lines = new float[LINE_COUNT][4];

        // grab up some random edges to test
        // NOTE: To add more edges, you need to change LINE_COUNT so the
        // segment buffer will be the right size.
        scanForHappyEdges();
    }

    // store segment descriptors in our GL line segment buffer.
    void setUniformLine(int segNo,float x1,float y1, float x2, float y2) {
        //TE.log("setLine %d : %.4f %.4f, %.4f %.4f",segNo,x1,y1,x2,y2);
        gl_segments.position(segNo * 4);
        gl_segments.put(-x1); gl_segments.put(y1);
        gl_segments.put(-x2); gl_segments.put(y2);
        gl_segments.rewind();
    }

    // convert from normalized physical model coords
    // to aspect corrected normalized 2D GL surface coords
    float modelToMapX(LXPoint pt) {
        //correct for aspect ratio of render target
        return 1.33333f*((-0.5f+pt.zn));
    }

    // convert from normalized physical model coords
    // to aspect corrected normalized 2D GL surface coords
    float modelToMapY(LXPoint pt) {
        return -0.525f + pt.yn;
    }

    // scan for edges with at least one connected panel
    void scanForHappyEdges() {
         Set<TEEdgeModel> edges = modelTE.getAllEdges();
         int edgeCount = 0;

        for (TEEdgeModel edge : edges) {

            if (edge.connectedPanels.size() >= 1) {
                for (TEPanelModel panel : edge.connectedPanels) {

                    // use only starboard side panels
                    if (panel.getId().startsWith("S")) {

                        //TE.log("Found edge w/panel(s): %s",edge.getId());
                        getLineFromEdge(edgeCount,edge.getId());
                        edgeCount++;
                        break;
                    }
                    if (edgeCount >= LINE_COUNT) break;
                }
            }

        }
        //TE.log("%d edges found!",edgeCount);
    }


    // given an edge id, adds a model edge's vertices to our list of line segments
    void getLineFromEdge(int index, String id) {
        LXPoint v1,v2;

        HashMap<String,TEEdgeModel> edges = modelTE.edgesById;

        TEEdgeModel edge = edges.get(id);
        if (edge != null) {
            //TE.log("Found edge %s", id);
        }
        else {
            TE.log("Null edge %s",id);
        }
        v1 = edge.points[0]; v2 = edge.points[edge.points.length - 1];

        // set x1,y1,x2,y2 in line array
        saved_lines[index][0] = modelToMapX(v1);  saved_lines[index][1] = modelToMapY(v1);
        saved_lines[index][2] = modelToMapX(v2);  saved_lines[index][3] = modelToMapY(v2);
    }

    // sends an array of line segments to the shader
    // should be called after all line computation is done,
    // before running the shader
    void sendSegments(float[][] lines,int nLines) {
        for (int i = 0; i < nLines; i++) {
            setUniformLine(i,lines[i][0],lines[i][1],lines[i][2],lines[i][3]);
        }
        shader.setUniform("lines", gl_segments,4);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        // send line segment array data
        sendSegments(saved_lines,LINE_COUNT);

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
