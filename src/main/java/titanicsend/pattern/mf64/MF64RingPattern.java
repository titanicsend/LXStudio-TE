package titanicsend.pattern.mf64;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.p4lx.pattern.P4LXPattern;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.ButtonColorMgr;
import titanicsend.util.TE;
import titanicsend.util.TEMath;

import java.util.ArrayList;

import static titanicsend.util.TEColor.BLUE;
import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64RingPattern extends TEMidiFighter64Subpattern {
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
    double startTime;

    double time;
    float ringWidth;
    private TEWholeModel modelTE;
    private LXPoint[] pointArray;

    private ButtonColorMgr colorMap;

    public MF64RingPattern(TEMidiFighter64DriverPattern driver) {
        super(driver);
        this.modelTE = this.driver.getModelTE();

        // get safe list of all pattern points.
        ArrayList<LXPoint> newPoints = new ArrayList<>(modelTE.points.length);
        newPoints.addAll(modelTE.edgePoints);
        newPoints.addAll(modelTE.panelPoints);
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

    private void clearAllPoints(int[] colors) {
        for (LXPoint point : this.pointArray) {
            colors[point.index] = TRANSPARENT;
        }
    }

    private void paintAll(int colors[], int color) {

        time = System.currentTimeMillis();

        // calculate milliseconds per beat at current bpm
        float interval = (float) (1000.0 / (driver.getTempo().bpm() / 60.0));
        float ringSawtooth = (float) (time - startTime) / interval;

        // grab colors of all currently pressed buttons
        int colorIndex = 0;
        int[] colorSet = colorMap.getAllColors();

        // make the rings slightly thinner as we add colors
        ringWidth = (float) TEMath.clamp(0.4f / (float) colorSet.length,0.08,0.2);

        // if we've completed a cycle see if we reset or stop
        if (ringSawtooth >= 1f) {
            if (stopRequest == true) {
                this.active = false;
                this.stopRequest = false;
                clearAllPoints(colors);
                return;
            }
            startTime = time;
            ringSawtooth = 0;
        }

        // define a ring moving out from the model center at 1 cycle/beat
        for (LXPoint point : this.pointArray) {
            float k,offs;
            int on,col;

            offs = 0f;
            col = TRANSPARENT;
            for (int i = 0; i < colorSet.length; i++) {
                k = (1.0f - ringSawtooth) + (point.rcn + offs);
                on = (int) (k * square(k, ringWidth));
                if (on > 0) {
                    col = colorSet[i];
                    break;
                }
                offs += ringWidth;
            }
            colors[point.index] = col;
        }
    }

    @Override
    public void run(double deltaMsec, int colors[]) {
        if (this.active == true) {
            paintAll(colors, colorMap.getCurrentColor());
        }
    }
}
