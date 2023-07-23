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
    private int refCount;

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
        refCount = 0;
    }

    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        buttons.addButton(mapping.col, overlayColors[mapping.col]);
        refCount++;
        this.active = true;
        this.stopRequest = false;
        startNewEvent();
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        buttons.removeButton(mapping.col);
        refCount--;
        if (refCount == 0) this.stopRequest = true;
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
        int col;
        for (TEPanelModel panel : modelTE.getAllPanels()) {

            boolean isLit = (prng.nextFloat() <= litProbability);

            // if panel is lit, pick a color from our set
            if (isLit) {
                col = colorSet[colorIndex];
                colorIndex = (colorIndex + 1) % colorSet.length;
            } else {
                col = TRANSPARENT;
            }

            // expand lit area out from center over a short time
            float deltaT = Math.min(1.0f, 4f * elapsedTime);

            for (TEPanelModel.LitPointData p : panel.litPointData) {
                // quick out for uncolored panels
                if (col == TRANSPARENT) {
                    setColor(p.point.index, TRANSPARENT);
                    // color an expanding radius
                } else if (p.radiusFraction <= deltaT) {
                    int alpha = (int) (255f * deltaT);
                    setColor(p.point.index, (col & 0x00FFFFFF) | (alpha << 24));
                    // leave uncolored points alone
                } else {
                    setColor(p.point.index, TRANSPARENT);
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
