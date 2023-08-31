package titanicsend.pattern.mf64;

import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.util.TEMath;

public class MF64SpiralSquares extends TEMidiFighter64Subpattern {
    private boolean active;
    private boolean stopRequest;
    private final VariableSpeedTimer time;

    public MF64SpiralSquares(TEMidiFighter64DriverPattern driver) {
        super(driver);

        time = new VariableSpeedTimer();
        this.active = false;
        this.stopRequest = false;
    }

    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        this.stopRequest = false;
        buttons.addButton(mapping.col, overlayColors[mapping.col]);
        this.active = true;
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        if (buttons.removeButton(mapping.col) == 0) this.stopRequest = true;
    }

    private void paintAll(int color) {

        // clear the decks if we're getting ready to stop
        if (stopRequest) {
            stopRequest = false;
            active = false;
        }

        // calculate time scale at current bpm
        time.setScale(4f * (float) (driver.getTempo().bpm() / 60.0));

        // grab colors of all currently pressed buttons
        int[] colorSet = buttons.getAllColors();

        // rotation rate is one per second. We sneakily control speed
        // by controlling the speed of time, so we can avoid trig operations
        // at pixel time.
        float cosT = (float) Math.cos(TEMath.TAU / 1000);
        float sinT = (float) Math.sin(TEMath.TAU / 1000);
        float t1 = time.getTimef();
        float t2 = t1 / 8;

        // a squared spiral from Pixelblaze pattern "Tunnel of Squares" at
        // https://github.com/zranger1/PixelblazePatterns/tree/master/2D_and_3D
        for (LXPoint point : modelTE.getPoints()) {

            // move normalized coord origin to model center
            float x = point.zn - 0.5f;
            float y = point.yn - 0.25f;

            // repeat pattern over x axis at interval cx to make
            // two spirals, one on each end of car.
            float cx = 0.3f;
            x = TEMath.floorModf(x + 0.5f * cx, cx) - 0.5f * cx;

            // set up our square spiral
            float x1 = Math.signum(x);
            float y1 = Math.signum(y);

            float sx = x1 * cosT + y1 * sinT;
            float sy = y1 * cosT - x1 * sinT;

            float dx = (float) Math.abs(Math.sin(4.0 * Math.log(x * sx + y * sy) + point.azimuth - t1));
            boolean on = ((dx * dx * dx) < 0.15);
            if (on) {
                float azimuth = (float) Math.atan2(y, x);
                azimuth = (float) (t2 + ((azimuth > 0) ? azimuth : azimuth + TEMath.TAU) / TEMath.TAU);
                int colorIndex = (int) (colorSet.length * azimuth % colorSet.length);
                blendColor(point.index, colorSet[colorIndex]);
            }
        }
    }

    @Override
    public void run(double deltaMsec) {
        time.tick();
        if (this.active) {
            paintAll(buttons.getCurrentColor());
        }
    }
}
