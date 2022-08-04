package titanicsend.pattern.jon;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.pattern.yoffa.shader_engine.ShaderOptions;
import titanicsend.util.TE;

import java.nio.FloatBuffer;
import java.util.HashMap;

@LXCategory("Native Shaders Panels")
public class EdgeFall extends TEAudioPattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    static final int LINE_COUNT = 4;
    FloatBuffer gl_segments;
    float[][] saved_lines;
    float[][] working_lines;

    public final CompoundParameter glow =
            new CompoundParameter("Glow", 80, 200, 10)
                    .setDescription("Line glow level");

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .15, 0, 1)
                    .setDescription("Oh boy...");

    // use iColor so we get the free iColorRGB uniform in our shader
    public final LinkedColorParameter iColor =
            registerColor("Color", "iColor", ColorType.PANEL,
                    "Panel Color");


    // store segment descriptors in our GL line segment buffer.
    void setUniformLine(int segNo,float x1,float y1, float x2, float y2) {
        TE.log("setLine %d : %.4f %.4f, %.4f %.4f",segNo,x1,y1,x2,y2);
        gl_segments.position(segNo * 4);
        gl_segments.put(-x1); gl_segments.put(y1);
        gl_segments.put(-x2); gl_segments.put(y2);
        gl_segments.rewind();
    }

    // convert from normalized physical model coords
    // to aspect corrected normalized 2D GL surface coords
    float modelToMapX(LXPoint pt) {
        return -0.5f + pt.zn;
    }

    // convert from normalized physical model coords
    // to aspect corrected normalized 2D GL surface coords
    float modelToMapY(LXPoint pt) {
        return -0.5f + pt.yn;
    }

    // given an edge id, adds a model edge's vertices to our list of line segments
    void getModelEdge(int index, String id) {
        float x1,y1,x2,y2;
        LXPoint v1,v2;

        HashMap<String,TEEdgeModel> edges = model.edgesById;

        TEEdgeModel edge = edges.get(id);
        TE.log("Found edge %s",id);
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

    // test -- something simple that moves the lines a little
    void moveLines(float[][] src, float[][] dst) {
        float p = (float) (-0.5 * measure());

        // just drop them straight downward with progress through a measure
        for (int i = 0; i < LINE_COUNT; i++) {
            dst[i][0] = src[i][0];      // x1
            dst[i][1] = src[i][1] + p; // move y1
            dst[i][2] = src[i][2];      // x2
            dst[i][3] = src[i][3] + p; // move y2
        }
    }


    public EdgeFall(LX lx) {
        super(lx);
        addParameter("glow",glow);
        addParameter("energy", energy);

        // create new effect with alpha on and no automatic
        // parameter uniforms

        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        // create an n x 4 array, so we can pass line segment descriptors
        // to GLSL shaders.
        // NOTE: This buffer needs to be *exactly* large enough to contain
        // the number of line segments you're using.  No smaller, no bigger.
        this.gl_segments = Buffers.newDirectFloatBuffer(LINE_COUNT * 4 * 4);

        // buffer to hold line descriptors taken from the vehicle
        saved_lines = new float[LINE_COUNT][4];

        // working storage so we can move those lines around
        working_lines = new float[LINE_COUNT][4];

        effect = new NativeShaderPatternEffect("edgefall.fs",
                PatternTarget.allPointsAsCanvas(this), options);

        // grab up some random edges to test
        // NOTE: To add more edges, you need to change LINE_COUNT so the
        // segment buffer will be the right size.
        getModelEdge(0,"99-100");
        getModelEdge(1,"26-115");
        getModelEdge(2,"69-127");
        getModelEdge(3,"90-93");
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        shader.setUniform("glow",glow.getValuef());

        moveLines(saved_lines, working_lines);

        // send line segment array data
        sendSegments(working_lines,LINE_COUNT);

        // Sound reactivity - various brightness features are related to energy
        // TODO -- does nothing yet.
        float e = energy.getValuef();
        shader.setUniform("energy",e*e);

        // run the shader
        effect.run(deltaMs);
    }

    @Override
    // THIS IS REQUIRED if you're not using ConstructedPattern!
    // Initialize the NativeShaderPatternEffect and retrieve the native shader object
    // from it when the pattern becomes active
    public void onActive() {
        effect.onActive();
        shader = effect.getNativeShader();
    }

}
