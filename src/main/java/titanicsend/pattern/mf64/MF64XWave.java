package titanicsend.pattern.mf64;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.ButtonColorMgr;
import titanicsend.util.TE;
import titanicsend.util.TEMath;

import java.util.ArrayList;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64XWave extends TEMidiFighter64Subpattern {
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
    private TEWholeModel modelTE;
    private ButtonColorMgr colorMap;

    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        this.flashColor = flashColors[mapping.col];
        colorMap.addButton(mapping.col, flashColors[mapping.col]);
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
        for (LXPoint point : modelTE.panelPoints) {
            colors[point.index] = TRANSPARENT;
        }
        for (LXPoint point : modelTE.edgePoints) {
            colors[point.index] = TRANSPARENT;
        }
    }

    public MF64XWave(TEMidiFighter64DriverPattern driver) {
        super(driver);
        this.modelTE = this.driver.getModelTE();
        colorMap = new ButtonColorMgr();
    }

    private void paintAll(int colors[], int color) {
        int col;

        // calculate milliseconds per beat at current bpm and build
        // a sawtooth wave that goes from 0 to 1 over that timespan
        time = System.currentTimeMillis();
        float interval = beatCount * (float) (1000.0 / (driver.getTempo().bpm() / 60.0));
        float cycle = (float) (time - startTime) / interval;
        // create a two peak sawtooth so we can run our wave twice per cycle,
        // and shape it for a more organic look
        float movement = (2f * cycle) % 1;
        movement = movement * movement * movement;

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

        float lightWave = movement;

        // do one wave on the panels
        for (LXPoint point : modelTE.panelPoints) {
            float dist = 1f - Math.abs(point.zn - lightWave);
            dist = dist * dist;

            int alpha = (int) (255 * TEMath.clamp(dist, 0, 1));
            col = (dist > 0.9) ? (color & 0x00FFFFFF) | (alpha << 24) : TRANSPARENT;

            colors[point.index] = col;
        }

        lightWave = 1 - movement;

        // and another on the edges
        for (LXPoint point : modelTE.edgePoints) {
            float dist = 1f - Math.abs(point.zn - lightWave);
            dist = dist * dist;

            int alpha = (int) (255 * TEMath.clamp(dist, 0, 1));
            col = (dist > 0.9) ? (color& 0x00FFFFFF) | (alpha << 24) : TRANSPARENT;

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
