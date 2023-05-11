package titanicsend.pattern.jon;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

import java.nio.FloatBuffer;

@LXCategory("Combo FG")
public class EdgeFall extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;
    double eventStartTime;
    double elapsedTime;
    static final double fallingCycleLength = 2.75;
    static final double burstDuration = 0.2;
    boolean isFalling;
    static final int LINE_COUNT = 52;
    FloatBuffer gl_segments;
    float[][] saved_lines;
    float[][] working_lines;
    float[][] line_velocity;

    // Constructor
    public EdgeFall(LX lx) {
        super(lx);

        // Size controls line width/glow
        controls.setRange(TEControlTag.SIZE, 80, 200, 15);
        controls.setExponent(TEControlTag.SIZE,0.3);

        // Wow1 - beat reactive pulse
        controls.setRange(TEControlTag.WOW1, 0, 0, 0.65);
        // wow2 - foreground vs gradient color mix

        addCommonControls();

        effect = new NativeShaderPatternEffect("edgefall.fs",
            new PatternTarget(this, TEShaderView.ALL_POINTS));

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

        // Select the edges we want to draw. NOTE: To add more edges, you need
        // to change LINE_COUNT so the segment buffer will be the right size. OpenGL
        // is very picky about this!
        CarGeometryPatternTools.getPanelConnectedEdges(getModelTE(), "^S.*$", saved_lines, LINE_COUNT);

        randomizeLineVelocities();

        eventStartTime = -99;
        isFalling = false;
    }

    // store segment descriptors in our GL line segment buffer.
    void setUniformLine(int segNo, float x1, float y1, float x2, float y2) {
        //TE.log("setLine %d : %.4f %.4f, %.4f %.4f",segNo,x1,y1,x2,y2);
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
    void sendSegments(float[][] lines, int nLines) {
        for (int i = 0; i < nLines; i++) {
            setUniformLine(i, lines[i][0], lines[i][1], lines[i][2], lines[i][3]);
        }
        shader.setUniform("lines", gl_segments, 4);
    }

    // generate a random value between a and b with minimum absolute value of c
    float randomBetween(float a, float b, float c) {
        float r = (float) (Math.random() * (b - a) + a);
        if (Math.abs(r) < c) {
            r = (r < 0) ? -c : c;
        }
        return r;
    }

    // set speed and direction of falling lines
    void randomizeLineVelocities() {
        for (int i = 0; i < LINE_COUNT; i++) {
            line_velocity[i][0] = randomBetween(-8, 8, 1);
            line_velocity[i][1] = randomBetween(-5, 5, 1);
        }
    }

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
        float glowLevel;

        double t = getTime();
        elapsedTime = Math.abs(t - eventStartTime);

        glowLevel = (float) getSize();

        // tiny state machine for falling vs. resting states
        if (isFalling) {
            // simulate short explosive burst (by greatly increasing line
            // width/glow) when event is first triggered
            if (elapsedTime < burstDuration) {
                glowLevel *= elapsedTime / burstDuration;
            }
        } else {
            eventStartTime = t;
            randomizeLineVelocities();
            elapsedTime = 0;
        }

        moveLines(saved_lines, working_lines);

        shader.setUniform("iScale", glowLevel);

        // send line segment array data
        sendSegments(working_lines, LINE_COUNT);

        // run the shader
        effect.run(deltaMs);
    }

    @Override
    protected void onWowTrigger(boolean on) {
        // when the wow trigger button is pressed...
        if (on) {
            isFalling = !isFalling;
        }
    }

    @Override
    // THIS IS REQUIRED if you're not using ConstructedPattern!
    // Initialize the NativeShaderPatternEffect and retrieve the native shader object
    // from it when the pattern becomes active
    public void onActive() {
        effect.onActive();
        shader = effect.getNativeShader();
    }

    @Override
    public String getDefaultView() {
        return effect.getDefaultView();
    }
}
