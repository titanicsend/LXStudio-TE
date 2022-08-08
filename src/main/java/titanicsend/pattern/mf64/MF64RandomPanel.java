package titanicsend.pattern.mf64;

import heronarts.lx.color.LXColor;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.VariableSpeedTimer;

import java.util.Random;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64RandomPanel extends TEMidiFighter64Subpattern {
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
    boolean active;
    boolean stopRequest;

    VariableSpeedTimer time;
    float eventStartTime;
    float elapsedTime;
    long seed;
    Random prng;

    private TEWholeModel model;

    public MF64RandomPanel(TEMidiFighter64DriverPattern driver) {
        super(driver);
        this.model = this.driver.getModel();

        this.active = false;
        this.stopRequest = false;

        seed = System.currentTimeMillis();
        prng = new Random(seed);

        time = new VariableSpeedTimer();
        eventStartTime = -99f;
    }
    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        this.flashColor = flashColors[mapping.col];
        this.active = true;
        startNewEvent();
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        this.stopRequest = true;
    }

    void startNewEvent() {
        seed = System.currentTimeMillis();
        eventStartTime = -time.getTime();
    }

    private void paintAll(int colors[], int color) {
        time.tick();

        // calculate time scale at current bpm
        time.setScale((float) (driver.getTempo().bpm() / 60.0));

        // clear the decks if we're getting ready to stop
        if (stopRequest) {
            stopRequest = false;
            active = false;
            color = TRANSPARENT;
        }

        // negative time b/c VariableSpeedTimer has a bug which I'm
        // stuck with for the moment because lots of patterns use it.
        // this is still handy because it lets me pretend we're running at
        // a constant 1 beat per second.  Makes the math simple.
        float t = -time.getTime();
        elapsedTime = t - eventStartTime;

        if (elapsedTime > 1) {
            // uncomment to enable auto retrigger on the beat
           // startNewEvent();
        }

        prng.setSeed(seed);
        for (TEPanelModel panel : model.getAllPanels()) {

            // light 25% of the panels, a different set on each beat.
            int col = (prng.nextFloat() <= 0.4) ? color : TRANSPARENT;
            float deltaT = Math.min(1.0f, 4f * elapsedTime);

            for (TEPanelModel.LitPointData p : panel.litPointData) {
                // quick out for uncolored panels
                if (col == TRANSPARENT) {
                    colors[p.point.index] = TRANSPARENT;
                // color an expanding radius
                } else if (p.radiusFraction <= deltaT) {
                    int alpha = (int) (255f * deltaT);
                    colors[p.point.index] = (col & 0x00FFFFFF) | (alpha << 24);
                // leave uncolored points alone
                } else {
                    colors[p.point.index] = TRANSPARENT;
                }
            }
        }
    }

    @Override
    public void run(double deltaMsec, int colors[]) {
        time.tick();
        if (this.active == true) {
            paintAll(colors, this.flashColor);
        }
    }
}
