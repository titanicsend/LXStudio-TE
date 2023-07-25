package titanicsend.pattern.mf64;

import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.util.TEMath;

public class MF64XWave extends TEMidiFighter64Subpattern {
    boolean active = false;
    boolean stopRequest = false;
    double time;
    double startTime;
    static final float beatCount = 2f;

    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        buttons.addButton(mapping.col, overlayColors[mapping.col]);
        this.active = true;
        stopRequest = false;
        startTime = System.currentTimeMillis();
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        if (buttons.removeButton(mapping.col) == 0) this.stopRequest = true;
    }

    public MF64XWave(TEMidiFighter64DriverPattern driver) {
        super(driver);
        buttons = new ButtonColorMgr();
    }

    private void paintAll(int color) {

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
                return;
            }
            startTime = time;
        }

        float lightWave = movement;

        // do one wave on the panels
        for (LXPoint point : modelTE.panelPoints) {
            float dist = 1f - Math.abs(point.zn - lightWave);
            dist = dist * dist;

            if (dist > 0.9) {
                int alpha = (int) (255 * TEMath.clamp(dist, 0, 1));
                blendColor(point.index,(color & 0x00FFFFFF) | (alpha << 24));
            }
        }

        lightWave = 1 - movement;

        // and another on the edges
        for (LXPoint point : modelTE.edgePoints) {
            float dist = 1f - Math.abs(point.zn - lightWave);
            dist = dist * dist;

            if (dist > 0.9) {
                int alpha = (int) (255 * TEMath.clamp(dist, 0, 1));
                blendColor(point.index,(color & 0x00FFFFFF) | (alpha << 24));
            }
        }
    }

    @Override
    public void run(double deltaMsec) {
        if (this.active) {
            paintAll(buttons.getCurrentColor());
        }
    }
}
