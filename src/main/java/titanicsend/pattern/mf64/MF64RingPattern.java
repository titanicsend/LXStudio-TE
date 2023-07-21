package titanicsend.pattern.mf64;

import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.util.TEMath;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64RingPattern extends TEMidiFighter64Subpattern {
    boolean active = false;
    boolean stopRequest = false;
    int refCount;
    double startTime;

    double time;
    float ringWidth;

    public MF64RingPattern(TEMidiFighter64DriverPattern driver) {
        super(driver);
    }

    /**
     * Converts a value  between 0.0 and 1.0, representing a sawtooth
     * waveform, to a position on a square wave between 0.0 to 1.0, using the
     * specified duty cycle.
     *
     * @param n         value between 0.0 and 1.0
     * @param dutyCycle - percentage of time the wave is "on", range 0.0 to 1.0
     * @return square wave value
     */
    public static float square(float n, float dutyCycle) {
        return (float) ((Math.abs((n % 1)) <= dutyCycle) ? 1.0 : 0.0);
    }

    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        buttons.addButton(mapping.col,overlayColors[mapping.col]);
        refCount++;
        this.active = true;
        stopRequest = false;
        startTime = System.currentTimeMillis();
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        buttons.removeButton(mapping.col);
        refCount--;
        if (refCount == 0) this.stopRequest = true;
    }

    private void clearAllPoints(int[] colors) {
        for (LXPoint point : modelTE.getPoints()) {
            colors[point.index] = TRANSPARENT;
        }
    }

    private void paintAll(int[] colors, int color) {

        time = System.currentTimeMillis();

        // calculate milliseconds per beat at current bpm
        float interval = (float) (1000.0 / (driver.getTempo().bpm() / 60.0));
        float ringSawtooth = (float) (time - startTime) / interval;

        // grab colors of all currently pressed buttons
        int[] colorSet = buttons.getAllColors();

        // make the rings slightly thinner as we add colors
        ringWidth = (float) TEMath.clamp(0.4f / (float) colorSet.length,0.08,0.2);

        // if we've completed a cycle see if we reset or stop
        if (ringSawtooth >= 1f) {
            if (stopRequest) {
                this.active = false;
                this.stopRequest = false;
                clearAllPoints(colors);
                return;
            }
            startTime = time;
            ringSawtooth = 0;
        }

        // define a ring moving out from the model center at 1 cycle/beat
        for (LXPoint point : modelTE.getPoints()) {
            float k,offs;
            int on,col;

            offs = 0f;
            col = TRANSPARENT;
            for (int i = 0; i < colorSet.length; i++) {
                k = (1.0f - ringSawtooth) + (point.rcn + offs);
                on = (int) (k * square(k, ringWidth));
                if (on > 0) {
                    col = colorSet[i];
                    break;
                }
                offs += ringWidth;
            }
            colors[point.index] = col;
        }
    }

    @Override
    public void run(double deltaMsec, int[] colors) {
        if (this.active) {
            paintAll(colors, buttons.getCurrentColor());
        }
    }
}
