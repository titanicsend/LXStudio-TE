package titanicsend.pattern.mf64;

import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.util.TEMath;

import java.util.Random;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64Hearts extends TEMidiFighter64Subpattern {
    boolean active;
    boolean stopRequest;
    VariableSpeedTimer time;
    float eventStartTime;
    double elapsedTime;
    long seed;
    Random prng;

    double heartSize;
    double yCenter;

    public MF64Hearts(TEMidiFighter64DriverPattern driver) {
        super(driver);
        this.active = false;
        this.stopRequest = false;

        seed = System.currentTimeMillis();
        prng = new Random(seed);
        heartSize = 0.35;

        time = new VariableSpeedTimer();
        eventStartTime = -99f;
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
        eventStartTime = -time.getTimef();
    }

    private double fract(double n) {
        return (n - Math.floor(n));
    }

    // heart sdf ported from:
    // https://github.com/zranger1/PixelblazePatterns/blob/master/2D_and_3D/heartbeat-SDF-2D.js
    // heart plus a little extra curvature to look nice on LED displays.
    private double heart(double x, double y, double radius) {
        // tweak aspect ratio a little for triangular panels
        x = x / radius * 1.25;

        // signed distance from 1/2 heart, mirrored about x axis
        y = y / radius + this.yCenter - Math.sqrt(Math.abs(x));
        radius = Math.hypot(x, y);

        // invert sdf result and return distance
        return 1 - radius;
    }

    private void paintAll(int color) {
        time.tick();

        // calculate time scale at current bpm
        time.setScale((float) (driver.getTempo().bpm() / 60.0));

        // grab colors of all currently pressed buttons
        int[] colorSet = buttons.getAllColors();

        // number of lit panels increases slightly with number of buttons pressed.
        // TEMath.clamp's min and max indicate percentages of coverage.
        float litProbability = (float) TEMath.clamp(0.4 + 0.3f * ((float) colorSet.length - 1) / 7f,
            0.4, 0.7);

        // clear the decks if we're getting ready to stop
        if (stopRequest) {
            stopRequest = false;
            active = false;
            colorSet[0] = TRANSPARENT;
        }

        double t = time.getTime();
        elapsedTime = t - eventStartTime;
        double t0 = fract(elapsedTime);
        heartSize = 0.35 + 0.05 * Math.sin(t0);

        prng.setSeed(seed);
        int colorIndex = 0;
        int col;
        for (TEPanelModel panel : modelTE.getAllPanels()) {

            // exclude the 4 flat-on-z front and back panels because x vs.z
            // gets weird without time-consuming adjustments.
            if (panel.getId().length() == 2) continue;

            boolean isLit = (prng.nextFloat() <= litProbability);

            // if panel is lit, pick a color from our set
            if (isLit) {
                col = colorSet[colorIndex];
                colorIndex = (colorIndex + 1) % colorSet.length;
                yCenter = Math.min(0.4, panel.yRange / panel.zRange);
            } else {
                col = TRANSPARENT;
            }

            // now draw something on the lit panel
            for (TEPanelModel.LitPointData p : panel.litPointData) {
                // quick out for uncolored panels
                if (col != TRANSPARENT) {
                    // generate roughly centered and normalized (relative to panel)
                    // coordinates
                    double x = (p.point.z - panel.centroid.z) / panel.zRange;
                    double y = (p.point.y - panel.centroid.y) / panel.yRange;

                    // heart sdf returns inverse distance from repeating figure.
                    // 1.0 at center, decreasing as you move out. We use this
                    // to calculate brightness, applied here as alpha
                    double d = heart(x, y, heartSize) / heartSize;
                    int alpha = (int) (255 * d);
                    setColor(p.point.index, (col & 0x00FFFFFF) | (alpha << 24));
                }
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
