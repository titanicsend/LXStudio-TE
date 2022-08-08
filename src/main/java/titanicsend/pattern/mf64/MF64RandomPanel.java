package titanicsend.pattern.mf64;

import heronarts.lx.color.LXColor;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.ButtonColorMgr;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.util.TEMath;

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
    private int refCount;
    ButtonColorMgr colorMap;

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
        time.setTimeDirectionForward(true);
        colorMap = new ButtonColorMgr();
        eventStartTime = -99f;
        refCount = 0;
    }
    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        colorMap.addButton(mapping.col,flashColors[mapping.col]);
        refCount++;
        this.active = true;
        this.stopRequest = false;
        startNewEvent();
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        colorMap.removeButton(mapping.col);
        refCount--;
        if (refCount == 0) this.stopRequest = true;
    }

    void startNewEvent() {
        seed = System.currentTimeMillis();
        eventStartTime = -time.getTime();
    }

    private void paintAll(int colors[], int color) {
        time.tick();

        // calculate time scale at current bpm
        time.setScale((float) (driver.getTempo().bpm() / 60.0));

        // grab colors of all currently pressed buttons
        int[] colorSet = colorMap.getAllColors();

        // number of lit panels increases slightly with number of buttons pressed.
        // TEMath.clamp's min and max indicate percentages of coverage.
        float litProbability = (float) TEMath.clamp(0.4 + 0.3f * ((float) colorSet.length - 1)/7f,
                0.4,0.7);

        // clear the decks if we're getting ready to stop
        if (stopRequest) {
            stopRequest = false;
            active = false;
        }

        float t = time.getTime();
        elapsedTime = t - eventStartTime;

        if (elapsedTime > 1) {
            // uncomment to enable auto retrigger on the beat
           // startNewEvent();
        }

        prng.setSeed(seed);
        int colorIndex = 0;
        int col;
        for (TEPanelModel panel : model.getAllPanels()) {

            boolean isLit = (prng.nextFloat() <= litProbability);

            // if panel is lit, pick a color from our set
            if (isLit) {
                col = colorSet[colorIndex];
                colorIndex = (colorIndex + 1) % colorSet.length;
            }
            else {
                col = TRANSPARENT;
            }

            // expand lit area out from center over a short time
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
            paintAll(colors, colorMap.getCurrentColor());
        }
    }
}
