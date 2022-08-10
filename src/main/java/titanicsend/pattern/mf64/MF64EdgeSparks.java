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
    private TEWholeModel model;
    private LXPoint[] pointArray;
    private ButtonColorMgr colorMap;

    public MF64EdgeSparks(TEMidiFighter64DriverPattern driver) {
        super(driver);
        this.model = this.driver.getModel();

        // get safe list of all pattern points.
        ArrayList<LXPoint> newPoints = new ArrayList<>(model.points.length);
        newPoints.addAll(model.edgePoints);
        pointArray = newPoints.toArray(new LXPoint[0]);
        colorMap = new ButtonColorMgr();
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
        startTime = System.currentTimeMillis();
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        colorMap.removeButton(mapping.col);
        refCount--;
        if (refCount == 0) this.stopRequest = true;
    }

    private void paintAll(int colors[], int color) {
        time = System.currentTimeMillis();

        // calculate milliseconds per beat at current bpm and build
        // a sawtooth wave that goes from 0 to 1 over that timespan
        // here, we want the fireworks thing to complete the course of a measure,
        // so we multiply the per-beat interval by 4.
        float interval = 4f * (float) (1000.0 / (driver.getTempo().bpm() / 60.0));
        float cycle = (float) (time - startTime) / interval;

        int colorIndex = 0;
        int col = color;

        // if we've completed a cycle see if we reset or stop
        if (cycle >= 1f) {
            if (stopRequest == true) {
                this.active = false;
                this.stopRequest = false;
                col = TRANSPARENT;
            }
            startTime = time;
            cycle = 0;
        }

        // "sparks" starting at the top of the frame, running down all the edges
        for (LXPoint point : this.pointArray) {
            float v;

            // get flipped y coord
            float y = 1f - point.yn;

            // calculate y distance from our moving wave
            float wavefront = 1.0f - (cycle - y);

            // "rocket" at leading edge of wave
            v = ((wavefront <= 1.0f) && (wavefront >= 0.95)) ? 1f - (1f-wavefront) / 0.05f : 0;

            // random decaying "sparks" behind
            float spark = ((wavefront < 0.95) && (wavefront >= 0.4)) ? wavefront : 0;
            if (spark > (3f * Math.random())) v = wavefront * wavefront * wavefront;

            int alpha = (int) (255f * v);
            colors[point.index] = (col == TRANSPARENT) ? col : (col & 0x00FFFFFF) | (alpha << 24);
        }
    }

    @Override
    public void run(double deltaMsec, int colors[]) {
        if (this.active == true) {
            paintAll(colors, colorMap.getCurrentColor());
        }
    }
}
