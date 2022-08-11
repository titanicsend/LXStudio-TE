package titanicsend.pattern.mf64;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.ButtonColorMgr;

import java.util.ArrayList;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64XRay extends TEMidiFighter64Subpattern {
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
    static final float beatCount = 2f;
    private TEWholeModel model;
    private ButtonColorMgr colorMap;


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
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        colorMap.removeButton(mapping.col);
        refCount--;
        if (refCount == 0) this.stopRequest = true;
    }

    MF64XRay(TEMidiFighter64DriverPattern driver)    {
      super(driver);
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
                // do something to stop
                return;
            }
            startTime = time;
            cycle = 0;
        }

        // do one wave on the panels
        for (LXPoint point : model.panelPoints) {
            // get flipped y coord
            float y = 1f - point.yn;
            colorIndex = (int) (8f * point.zn) % colorSet.length;

            // calculate y distance from our moving wave
            float wavefront = 1.0f - (cycle - y);

            int alpha = 0;
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
