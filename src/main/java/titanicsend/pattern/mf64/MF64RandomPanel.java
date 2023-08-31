package titanicsend.pattern.mf64;

import titanicsend.model.TEPanelModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.util.TEMath;

import java.util.Random;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64RandomPanel extends TEMidiFighter64Subpattern {
    boolean active;
    boolean stopRequest;
    VariableSpeedTimer time;
    float eventStartTime;
    float elapsedTime;
    long seed;
    Random prng;

    public MF64RandomPanel(TEMidiFighter64DriverPattern driver) {
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

        /*  uncomment block to enable auto retrigger on the beat
        if (elapsedTime > 1) {
            startNewEvent();
        }
        */

        prng.setSeed(seed);
        int colorIndex = 0;
        for (TEPanelModel panel : modelTE.getAllPanels()) {
            boolean isLit = (prng.nextFloat() <= litProbability);
            if (!isLit) continue;

            // get the next color from our set
            int col = colorSet[colorIndex];
            colorIndex = (colorIndex + 1) % colorSet.length;

            for (TEPanelModel.LitPointData p : panel.litPointData) {
                if (p.radiusFraction <= 1.0) {
                     blendColor(p.point.index, col | 0xFF000000);
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
