package titanicsend.pattern.jon;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.pattern.yoffa.shader_engine.ShaderOptions;
import titanicsend.util.TE;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

@LXCategory("Native Shaders Panels")
public class EdgeFall extends TEAudioPattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;
    VariableSpeedTimer time;
    float eventStartTime;
    float elapsedTime;
    static final float fallingCycleLength = 2.75f;
    static final float pauseCycleLength = 0.25f;

    boolean isFalling = true;

    static final int LINE_COUNT = 32;
    FloatBuffer gl_segments;
    float[][] saved_lines;
    float[][] working_lines;
    float[][] line_velocity;

    public final CompoundParameter lineType = (CompoundParameter)
            new CompoundParameter("Type", 0, 0, 2)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("LineType");

    public final CompoundParameter glow =
            new CompoundParameter("Glow", 80, 200, 10)
                    .setDescription("Line glow level");

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .15, 0, 1)
                    .setDescription("Oh boy...");

    public final BooleanParameter explode =
            new BooleanParameter("Xplode!", false)
                    .setDescription("Yesssss!");

    // use iColor as the path so we get the free iColorRGB uniform in our shader
    public final LinkedColorParameter iColor =
            registerColor("Color", "iColor", ColorType.PRIMARY,
                    "Panel Color");

    // Constructor
    public EdgeFall(LX lx) {
        super(lx);
        addParameter("lineType",lineType);
        addParameter("glow",glow);
        addParameter("energy", energy);
        addParameter("explode",explode);

        // create new effect with alpha on and no automatic
        // parameter uniforms

        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        effect = new NativeShaderPatternEffect("edgefall.fs",
                PatternTarget.allPointsAsCanvas(this), options);

        // create an n x 4 array, so we can pass line segment descriptors
        // to GLSL shaders.
        // NOTE: This buffer needs to be *exactly* large enough to contain
        // the number of line segments you're using.  No smaller, no bigger.
        this.gl_segments = Buffers.newDirectFloatBuffer(LINE_COUNT * 4 * 4);

        // buffer to hold line descriptors taken from the vehicle
        saved_lines = new float[LINE_COUNT][4];

        // working storage so we can move those lines around
        working_lines = new float[LINE_COUNT][4];

        // per-line x and y velocity components
        line_velocity = new float[LINE_COUNT][2];

        // grab up some random edges to test
        // NOTE: To add more edges, you need to change LINE_COUNT so the
        // segment buffer will be the right size.
        scanForHappyEdges();
/*
        getLineFromEdge(0,"99-100");
        getLineFromEdge(1,"26-115");
        getLineFromEdge(2,"69-127");
        getLineFromEdge(3,"90-93");
        getLineFromEdge(4,"26-99");
        getLineFromEdge(5,"51-90");
*/

        randomizeLineVelocities();

        time = new VariableSpeedTimer();
        eventStartTime = -99f;
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
        return -0.5f + pt.zn;
    }

    // convert from normalized physical model coords
    // to aspect corrected normalized 2D GL surface coords
    float modelToMapY(LXPoint pt) { return -0.5f + pt.yn;  }

    void scanForHappyEdges() {
         Set<TEEdgeModel> edges = model.getAllEdges();
         int edgeCount = 0;

        for (TEEdgeModel edge : edges) {

            if (edge.connectedPanels.size() > 1) {
                for (TEPanelModel panel : edge.connectedPanels) {

                    // use only starboard side panels
                    if (panel.getId().startsWith("S")) {
                        // don't use the upper panels
                        if (panel.getId().startsWith("SU")) continue;

                        TE.log("We like edge: %s",edge.getId());
                        getLineFromEdge(edgeCount,edge.getId());
                        edgeCount++;
                        break;
                    }
                }
            }

        }
        //TE.log("We liked %d edges today!",edgeCount);
    }


    // given an edge id, adds a model edge's vertices to our list of line segments
    void getLineFromEdge(int index, String id) {
        LXPoint v1,v2;

        HashMap<String,TEEdgeModel> edges = model.edgesById;

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

    // set line falling speeds velocities.  This is a truly silly way to do it!
    void randomizeLineVelocities() {

        // we let the lines drift around a bit in x and
        // mostly fall quickly in y

        for (int i = 0; i < LINE_COUNT; i++) {
            line_velocity[i][0] = (float) (8.0 * (Math.random() - 0.5));
            line_velocity[i][1] = (float) (5.0 * (Math.random() - 0.5));
        }
    }

    // test -- something simple that moves the lines a little
    int measureCount = 0;
    void moveLines(float[][] src, float[][] dst) {

        float d = (isFalling) ? (float) (-0.5 * elapsedTime / fallingCycleLength) : 0f;

        for (int i = 0; i < LINE_COUNT; i++) {
            dst[i][0] = src[i][0] + d * line_velocity[i][0]; // x1
            dst[i][1] = src[i][1] + d * line_velocity[i][1]; // y1
            dst[i][2] = src[i][2] + d * line_velocity[i][0]; // x2
            dst[i][3] = src[i][3] + d * line_velocity[i][1]; // y2
        }
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        float glw;
        time.tick();

        // negative time b/c VariableSpeedTimer has a bug which I'm
        // stuck with for the moment because lots of patterns use it.
        float t = -time.getTime();
        elapsedTime = t - eventStartTime;

        if (!explode.getValueb()) isFalling = false;
        glw = glow.getValuef();

        // tiny state machine for falling vs. paused states
        if (isFalling) {
            if (elapsedTime > fallingCycleLength) {
                isFalling = false;
                eventStartTime = t;
                elapsedTime = 0f;
                explode.setValue(false);
            }
            else  if (elapsedTime <= 0.1) {
                // simulate short explosive burst
                glw *= elapsedTime / 0.1f;
            }
        } else {
            if (elapsedTime > pauseCycleLength) {
                isFalling = true;
                eventStartTime = t;
                randomizeLineVelocities();
                elapsedTime = 0f;
            }

        }

        moveLines(saved_lines, working_lines);

        // choose line drawing method.
        // type 0 glows and has pointy ends
        // type 1 glows more and is a normal line segment
        // type 2 is a bright, well defined line with very little glow.
        shader.setUniform("lineType",(int) Math.floor(lineType.getValuef()));

        // send line width "glow" parameter
        shader.setUniform("glow",glw);

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
