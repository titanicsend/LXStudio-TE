package titanicsend.util;

import heronarts.lx.LX;

/**
 * Class to support incremental rotation over variable-speed time
 * <p>
 * The rate is tied to the engine bpm and the input time value, which is usually
 * controlled by the variable speed timer associated with the speed or spin controls.
 * (but anything with a seconds.millis timer can generate rotational angles this way.)
 */
public class Rotor {
    private double maxSpinRate = Math.PI;
    private double angle = 0;
    private double lastTime = 0;

    // Internal: Called on every frame to calculate and memoize the current
    // spin angle so that calls to getAngle() during a frame
    // will always return the same value no matter how long the frame
    // calculations take.
    // TODO(look): instead of 'public', if we want this to be internal to TEPerformancePattern, all the TEPerfPattern
    //             stuff could go in a package and this method could be package-protected
    public void updateAngle(double time, double ctlValue) {
        // if this is the first frame, or if the timer was restarted,
        // we skip calculation for a frame.  Otherwise, do
        // the incremental angle calculation...
        if (lastTime != 0) {
            // calculate change in angle since last frame.
            // Note: revised calculation restricts maximum speed while still allowing
            // you to get to maximum speed at slower bpm.
            double et = Math.min(maxSpinRate, maxSpinRate * (time - lastTime));
            angle -= et % LX.TWO_PI;
        }
        lastTime = time;
    }

    /**
     * @return Current rotational angle, either computed, or taken from
     */
    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public void addAngle(double offset) {
        this.angle += offset;
    }

    public void reset() {
        angle = 0;
        lastTime = 0;
    }

    /**
     * Sets maximum spin rate for all patterns using this rotor.  Note that a Rotor
     * object is associated with a timer, which can be a VariableSpeedTimer.  So
     * "seconds" may be variable in duration, and can be positive or negative.
     *
     * @param radiansPerSecond
     */
    public void setMaxSpinRate(double radiansPerSecond) {
        maxSpinRate = radiansPerSecond;
    }
}
