package titanicsend.pattern.mf64;

import heronarts.lx.color.LXColor;
import heronarts.lx.transform.LXVector;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.ButtonColorMgr;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.util.TEMath;

import java.util.Random;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64Spinwheel extends TEMidiFighter64Subpattern {
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

    private TEWholeModel modelTE;
    LXVector panelCenter;

    public MF64Spinwheel(TEMidiFighter64DriverPattern driver) {
        super(driver);
        this.modelTE = this.driver.getModelTE();

        this.active = false;
        this.stopRequest = false;

        seed = System.currentTimeMillis();
        prng = new Random(seed);

        time = new VariableSpeedTimer();
        //time.setTimeDirectionForward(true);
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
        eventStartTime = -time.getTimef();
    }

    /**
     * Again, wtf doesn't Java have one of these?
     */
    float frac(float n) {
        return (float) (n - Math.floor(n));
    }

    float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    // does the spinwheel thing, returns the brightness
    // at the specified pixel.
    float spinwheel(TEPanelModel.LitPointData lp,float spin, float grow) {
        // move coordinate origin to panel center
        float x = lp.point.z - panelCenter.z;
        float y = lp.point.y - panelCenter.y;

        // can derive local azimuth geometrically, but atan2 is faster...
        // the arX multiplier controls the number of petals.  Higher is more.
        float arX = (float) (Math.atan2(y,x) * 1.25 + spin);
        float arY = (float) (lp.radiusFraction + grow);

        // Shape the pulse made by the arY + grow term. Higher divisor == more contrast
        float pulse = (float) (Math.floor(arY) / 6.0);
        // keep us from flashing the whole panel to absolute black
        pulse += (pulse == 0) ? 0.5 : 0;

        arX = Math.abs(frac(arX)) - 0.5f;
        arY = Math.abs(frac(arY)) - 0.5f;

        float bri = (float) ((0.2/(arX*arX+arY*arY) * .19) * pulse);

        // clamp to range, then small gamma correction
        bri = clamp(bri*4f,0f,1f);
        return bri * bri;
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
            colorSet[0] = TRANSPARENT;
        }

        float t = time.getTimef();
        elapsedTime = t - eventStartTime;
        float t0 = frac(elapsedTime);
        float spin = 2f * t;
        float grow = (float) (Math.sin(t0) * 1.414);

        prng.setSeed(seed);
        int colorIndex = 0;
        int col;
        for (TEPanelModel panel : modelTE.getAllPanels()) {

            // exclude 4 front and back panels because x vs.z gets too weird
            // for radial math without time-consuming adjustments
            if (panel.getId().length() == 2) continue;

            boolean isLit = (prng.nextFloat() <= litProbability);

            // if panel is lit, pick a color from our set
            if (isLit) {
                col = colorSet[colorIndex];
                colorIndex = (colorIndex + 1) % colorSet.length;
            }
            else {
                col = TRANSPARENT;
            }

            // now draw something on the lit panel
            for (TEPanelModel.LitPointData p : panel.litPointData) {
                // quick out for uncolored panels
                if (col == TRANSPARENT) {
                    colors[p.point.index] = TRANSPARENT;
                } else {
                    // do the spinwheel thing
                    panelCenter = panel.centroid;
                    int alpha = (int) (255f * spinwheel(p,spin,grow));
                    colors[p.point.index] = (col & 0x00FFFFFF) | (alpha << 24);
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
