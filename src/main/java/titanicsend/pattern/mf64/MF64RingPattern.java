package titanicsend.pattern.mf64;

import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.util.TEMath;

public class MF64RingPattern extends TEMidiFighter64Subpattern {
    boolean active = false;
    boolean stopRequest = false;
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
        buttons.addButton(mapping.col, overlayColors[mapping.col]);
        this.active = true;
        stopRequest = false;
        startTime = System.currentTimeMillis();
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        if (buttons.removeButton(mapping.col) == 0) this.stopRequest = true;
    }

    private void paintAll(int color) {
        time = System.currentTimeMillis();

        // calculate milliseconds per beat at current bpm
        float interval = (float) (1000.0 / (driver.getTempo().bpm() / 60.0));
        float ringSawtooth = (float) (time - startTime) / interval;

        // grab colors of all currently pressed buttons
        int[] colorSet = buttons.getAllColors();

        // make the rings slightly thinner as we add colors
        ringWidth = (float) TEMath.clamp(0.4f / (float) colorSet.length, 0.08, 0.2);

        // if we've completed a cycle see if we reset or stop
        if (ringSawtooth >= 1f) {
            if (stopRequest) {
                this.active = false;
                this.stopRequest = false;
                return;
            }
            startTime = time;
            ringSawtooth = 0;
        }

        // define a ring moving out from the model center at 1 cycle/beat
        for (LXPoint point : modelTE.getPoints()) {
            float k, offs;

            offs = 0f;
            for (int i = 0; i < colorSet.length; i++) {
                k = (1.0f - ringSawtooth) + (point.rcn + offs);
                int on = (int) (k * square(k, ringWidth));
                if (on > 0) {
                    blendColor(point.index,colorSet[i]);
                    break;
                }
                offs += ringWidth;
            }
        }
    }

    @Override
    public void run(double deltaMsec) {
        if (this.active) {
            paintAll(buttons.getCurrentColor());
        }
    }
}
