package titanicsend.pattern.mf64;

import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.util.TEMath;

import java.util.Random;

public class MF64Hearts extends TEMidiFighter64Subpattern {
    boolean active;
    boolean stopRequest;
    long seed;
    Random prng;

    public MF64Hearts(TEMidiFighter64DriverPattern driver) {
        super(driver);
        this.active = false;
        this.stopRequest = false;

        seed = System.currentTimeMillis();
        prng = new Random(seed);
    }

    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        buttons.addButton(mapping.col, overlayColors[mapping.col]);
        this.active = true;
        this.stopRequest = false;
        startNewEvent();
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        if (buttons.removeButton(mapping.col) == 0) this.stopRequest = true;
    }

    private void startNewEvent() {
        seed = System.currentTimeMillis();
    }

    // heart sdf ported from:
    // https://github.com/zranger1/PixelblazePatterns/blob/master/2D_and_3D/heartbeat-SDF-2D.js
    // heart plus a little extra curvature to look nice on LED displays.
    private double heart(double x, double y, double radius) {
        // tweak aspect ratio a little for our panels
        x = x / radius * 0.75;

        // signed distance from 1/2 heart, mirrored about x axis
        y = y / radius + 0.4 - Math.sqrt(Math.abs(x));
        radius = Math.hypot(x, y);

        // invert sdf result and return distance
        return 1 - radius;
    }

    private void paintAll(int color) {
        // clear the decks if we're getting ready to stop
        if (stopRequest) {
            stopRequest = false;
            active = false;
        }

        // grab colors of all currently pressed buttons
        int[] colorSet = buttons.getAllColors();

        prng.setSeed(seed);

        // set up animation timing
        // NOTE: This animation requires an LX engine beat, either from OSC or INT.
        double t = 0.5 + 0.5 * Math.sin(TEMath.TAU * driver.getTempo().basis());
        double heartSize = 0.075 + 0.075 * t;

        for (LXPoint point : modelTE.panelPoints) {

            // adjust x and y to repeat pattern twice,
            // roughly centered on large car sections and
            float x = ((2f * point.zn) % 1f) - 0.5f;
            x += (point.z > 0f) ? -0.1f : 0.1f;
            float y = point.yn - 0.18f;

            // heart sdf returns inverse distance from repeating figure.
            // 1.0 at center, decreasing as you move out. We use this
            // to calculate brightness.
            double d = heart(x, y, heartSize);
            // NOTE: at this point, d can have values outside the range 0..1.
            // We deliberately use the overflow to repeat the heart pattern at
            // various scales.
            int colorIndex = (int) Math.floor(Math.abs(d / 2) * colorSet.length) % colorSet.length;
            int alpha = (int) (255 * d);
            blendColor(point.index, (colorSet[colorIndex] & 0x00FFFFFF) | (alpha << 24));
        }
    }

    @Override
    public void run(double deltaMsec) {
        if (this.active) {
            paintAll(buttons.getCurrentColor());
        }
    }
}
