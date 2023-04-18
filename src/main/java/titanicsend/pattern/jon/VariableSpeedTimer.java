package titanicsend.pattern.jon;

/**
 * Variable speed class for iTime -- time since program
 * start in seconds.millis.   The setScale method allows
 * for smooth speed changes.   setScale(1) == 1 timer second/real second,
 * setScale(2) == 2 timer seconds/real second,  etc.
 *
 * To use, create a VariableSpeedTimer object, then call the tick() method on every frame.
 * Read the current scaled time with getTime(), and change
 * speed at any time with setScale().
 */
// variable speed fake "iTime" timer class
// allows smooth speed changes
public class VariableSpeedTimer {
    long previous = 0;
    long current = 0;
    double time = 0.0;
    double scale = 1.0;
    double delta = 0;
    double direction = -1.0;

    public VariableSpeedTimer() {
        scale = 1.0;
        reset();
    }

    public void reset() {
        previous = current = System.currentTimeMillis();
        time = 0.0;
    }

    public void setScale(double s) {
        scale = s;
    }

    public void setTimeDirectionForward(boolean t) {
        direction = (t) ? 1.0f : -1.0f;
    }

    public void tick() {
        delta = scale * direction *  (double) (current - previous);
        time += delta;
        previous = current;
        current = System.currentTimeMillis();
    }

    public double getTime() { return  time / 1000; }
    public float getTimef() { return (float) getTime(); }
    public double getTimeMs()  { return time; }
    public double getDeltaMs() { return delta; }
}