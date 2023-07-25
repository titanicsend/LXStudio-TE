package titanicsend.pattern.mf64;

import heronarts.lx.transform.LXVector;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.util.TEMath;

import java.util.Random;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64Spinwheel extends TEMidiFighter64Subpattern {
    boolean active;
    boolean stopRequest;
    VariableSpeedTimer time;
    float eventStartTime;
    float elapsedTime;
    long seed;
    Random prng;

    LXVector panelCenter;

    public MF64Spinwheel(TEMidiFighter64DriverPattern driver) {
        super(driver);
        this.active = false;
        this.stopRequest = false;

        seed = System.currentTimeMillis();
        prng = new Random(seed);

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

    void startNewEvent() {
        seed = System.currentTimeMillis();
        eventStartTime = -time.getTimef();
    }

    float fract(float n) {
        return (float) (n - Math.floor(n));
    }

    float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // does the spinwheel thing, returns the brightness
    // at the specified pixel.
    float spinwheel(TEPanelModel.LitPointData lp, float spin, float grow) {
        // move coordinate origin to panel center
        float x = lp.point.z - panelCenter.z;
        float y = lp.point.y - panelCenter.y;

        // can derive local azimuth geometrically, but atan2 is faster...
        // the arX multiplier controls the number of petals.  Higher is more.
        float arX = (float) (Math.atan2(y, x) * 1.25 + spin);
        float arY = (float) (lp.radiusFraction + grow);

        // Shape the pulse made by the arY + grow term. Higher divisor == more contrast
        float pulse = (float) (Math.floor(arY) / 6.0);
        // keep us from flashing the whole panel to absolute black
        pulse += (pulse == 0) ? 0.5 : 0;

        arX = Math.abs(fract(arX)) - 0.5f;
        arY = Math.abs(fract(arY)) - 0.5f;

        float bri = (float) ((0.2 / (arX * arX + arY * arY) * .19) * pulse);

        // clamp to range, then small gamma correction
        bri = clamp(bri * 4f, 0f, 1f);
        return bri * bri;
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

        float t = time.getTimef();
        elapsedTime = t - eventStartTime;
        float t0 = fract(elapsedTime);
        float spin = 2f * t;
        float grow = (float) (Math.sin(t0) * 1.414);

        prng.setSeed(seed);
        int colorIndex = 0;
        int col;
        for (TEPanelModel panel : modelTE.getAllPanels()) {

            boolean isLit = (prng.nextFloat() <= litProbability);
            if (!isLit) continue;

            // exclude the 4 flat-on-z front and back panels because x vs.z
            // gets too weird for radial math without time-consuming adjustments
            if (panel.getId().length() == 2) continue;

            // pick a color from our set
            col = colorSet[colorIndex];
            colorIndex = (colorIndex + 1) % colorSet.length;

            // now draw something on the panel
            for (TEPanelModel.LitPointData p : panel.litPointData) {
                // do the spinwheel thing
                panelCenter = panel.centroid;
                int alpha = (int) (255f * spinwheel(p, spin, grow));
                blendColor(p.point.index, (col & 0x00FFFFFF) | (alpha << 24));
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
