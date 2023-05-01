package titanicsend.pattern.mf64;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.ButtonColorMgr;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.util.TEMath;

import java.util.ArrayList;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64EdgeSparks extends TEMidiFighter64Subpattern {
    private static final double PERIOD_MSEC = 100.0;
    private static final int[] flashColors = {
            LXColor.rgb(255, 0, 0),
            LXColor.rgb(255, 170, 0),
            LXColor.rgb(255, 255, 0),
            LXColor.rgb(0, 255, 0),
            LXColor.rgb(0, 170, 170),
            LXColor.rgb(0, 0, 255),
            LXColor.rgb(255, 0, 255),
            LXColor.rgb(255, 255, 255),
    };
    private int flashColor = TRANSPARENT;
    boolean active = false;
    boolean stopRequest = false;
    int refCount;
    double time;
    double startTime;
    static final float rocketSize = 0.045f;
    static final float rocketPos = 1f - rocketSize;
    static final float beatCount = 2f;
    private TEWholeModel modelTE;
    private LXPoint[] pointArray;
    private ButtonColorMgr colorMap;

    public MF64EdgeSparks(TEMidiFighter64DriverPattern driver) {
        super(driver);
        this.modelTE = this.driver.getModelTE();

        // get safe list of all pattern points.
        ArrayList<LXPoint> newPoints = new ArrayList<>(modelTE.points.length);
        newPoints.addAll(modelTE.edgePoints);
        pointArray = newPoints.toArray(new LXPoint[0]);
        colorMap = new ButtonColorMgr();
        startTime = 0;
    }

    /**
     * Converts a value  between 0.0 and 1.0, representing a sawtooth
     * waveform, to a position on a square wave between 0.0 to 1.0, using the
     * specified duty cycle.
     *
     * @param n         value between 0.0 and 1.0
     * @param dutyCycle - percentage of time the wave is "on", range 0.0 to 1.0
     * @return
     */
    public static float square(float n, float dutyCycle) {
        return (float) ((Math.abs((n % 1)) <= dutyCycle) ? 1.0 : 0.0);
    }

    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        this.flashColor = flashColors[mapping.col];
        colorMap.addButton(mapping.col,flashColors[mapping.col]);
        refCount++;
        this.active = true;
        stopRequest = false;
        // uncomment to enable "glitch in place" feature.
        //startTime += 200;
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        colorMap.removeButton(mapping.col);
        refCount--;
        if (refCount == 0) this.stopRequest = true;
    }

    private void clearAllPoints(int[] colors) {
        for (LXPoint point : this.pointArray) {
            colors[point.index] = TRANSPARENT;
        }
    }

    private void paintAll(int colors[], int color) {
        time = System.currentTimeMillis();

        // calculate milliseconds per beat at current bpm and build
        // a sawtooth wave that goes from 0 to 1 over that timespan
        float interval = beatCount * (float) (1000.0 / (driver.getTempo().bpm() / 60.0));
        float cycle = (float) (time - startTime) / interval;

        // grab colors of all currently pressed buttons
        int colorIndex = 0;
        int[] colorSet = colorMap.getAllColors();
        int col = colorSet[colorIndex];

        // if we've completed a cycle see if we reset or stop
        if (cycle >= 1f) {
            if (stopRequest == true) {
                this.active = false;
                this.stopRequest = false;
                clearAllPoints(colors);
                return;
            }
            startTime = time;
            cycle = 0;
        }

        // Basic approach adapted from Ben Hencke's "Fireworks Nova" pixelblaze pattern.
        // Build a moving sawtooth, coloring the leading edge with a solid color and
        // trailing off into random sparkles.  It winds up looking a lot like a particle
        // system, but it's much cheaper to compute.
        for (LXPoint point : this.pointArray) {
            float v;

            // get flipped y coord
            float y = 1f - point.yn;
            colorIndex = (int) (8f * point.zn) % colorSet.length;

            // calculate y distance from our moving wave
            float wavefront = 1.0f - (cycle - y);

            // "rocket" at leading edge of wave
            //v = ((wavefront <= 1.0f) && (wavefront >= rocketPos)) ? 1f - (1f-wavefront) / rocketSize : 0;
            v = ((wavefront <= 1.0f) && (wavefront >= rocketPos)) ? 1f : 0;

            // random decaying "sparks" behind
            float spark = ((wavefront < rocketPos) && (wavefront >= 0.35)) ? wavefront : 0;
            if (spark > (3f * Math.random())) v = wavefront * wavefront * wavefront;

            int alpha = (int) (255f * v);
            colors[point.index] = (colorSet[colorIndex] & 0x00FFFFFF) | (alpha << 24);
        }
    }

    @Override
    public void run(double deltaMsec, int colors[]) {
        if (this.active == true) {
            paintAll(colors, colorMap.getCurrentColor());
        }
    }
}
