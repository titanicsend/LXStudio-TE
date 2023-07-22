package titanicsend.pattern.mf64;

import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.util.TEMath;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64XWave extends TEMidiFighter64Subpattern {
    boolean active = false;
    boolean stopRequest = false;
    int refCount;
    double time;
    double startTime;
    static final float beatCount = 2f;

    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        buttons.addButton(mapping.col, overlayColors[mapping.col]);
        refCount++;
        this.active = true;
        stopRequest = false;
        startTime = System.currentTimeMillis();
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        buttons.removeButton(mapping.col);
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
        buttons = new ButtonColorMgr();
    }

    private void paintAll(int[] colors, int color) {
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
            if (stopRequest) {
                this.active = false;
                this.stopRequest = false;
                clearAllPoints(colors);
                return;
            }
            startTime = time;
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
            col = (dist > 0.9) ? (color & 0x00FFFFFF) | (alpha << 24) : TRANSPARENT;

            colors[point.index] = col;
        }
    }

    @Override
    public void run(double deltaMsec, int[] colors) {
        if (this.active) {
            paintAll(colors, buttons.getCurrentColor());
        }
    }
}
