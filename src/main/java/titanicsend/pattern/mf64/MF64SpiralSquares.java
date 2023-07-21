package titanicsend.pattern.mf64;

import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.util.TEMath;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64SpiralSquares extends TEMidiFighter64Subpattern {
    private boolean active;
    private boolean stopRequest;
    private VariableSpeedTimer time;
    private int refCount;

    public MF64SpiralSquares(TEMidiFighter64DriverPattern driver) {
        super(driver);

        time = new VariableSpeedTimer();
        this.active = false;
        this.stopRequest = false;

        refCount = 0;
    }

    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        this.stopRequest = false;
        buttons.addButton(mapping.col, overlayColors[mapping.col]);
        refCount++;
        this.active = true;
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        buttons.removeButton(mapping.col);
        refCount--;
        if (refCount == 0) this.stopRequest = true;
    }

    private void paintAll(int[] colors, int color) {

        // clear the decks if we're getting ready to stop
        if (stopRequest) {
            stopRequest = false;
            active = false;
            color = TRANSPARENT;
        }

        // calculate time scale at current bpm
        time.setScale(4f * (float) (driver.getTempo().bpm() / 60.0));

        // rotation rate is one per second. We sneakily control speed
        // by controlling the speed of time, so we can avoid trig operations
        // at pixel time.
        float cosT = (float) Math.cos(TEMath.TAU / 1000);
        float sinT = (float) Math.sin(TEMath.TAU / 1000);
        float t1 = time.getTimef();

        // a squared spiral from Pixelblaze pattern "Tunnel of Squares" at
        // https://github.com/zranger1/PixelblazePatterns/tree/master/2D_and_3D
        for (LXPoint point : modelTE.getPoints()) {

            // move normalized coord origin to model center
            float x = point.zn - 0.5f;
            float y = point.yn - 0.25f;

            // repeat pattern over x axis at interval cx to make
            // two spirals, one on each end of car.
            float cx = 0.3f;  // two spirals, one on each end of car
            x = TEMath.floorModf(x + 0.5f * cx, cx) - 0.5f * cx;

            // set up our square spiral
            float x1 = Math.signum(x);
            float y1 = Math.signum(y);

            float sx = x1 * cosT + y1 * sinT;
            float sy = y1 * cosT - x1 * sinT;

            float dx = (float) Math.abs(Math.sin(4.0 * Math.log(x * sx + y * sy) + point.azimuth - t1));
            int on = ((dx * dx * dx) < 0.15) ? 1 : 0;

            colors[point.index] = color * on;
        }
    }

    @Override
    public void run(double deltaMsec, int[] colors) {
        time.tick();
        if (this.active) {
            paintAll(colors, buttons.getCurrentColor());
        }
    }
}
