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
    float time = 0.0f;
    float scale = 1.0f;
    float direction = -1.0f;

    public VariableSpeedTimer() {
        scale = 1.0f;
        reset();
    }

    public void reset() {
        previous = current = System.currentTimeMillis();
        time = 0.0f;
    }

    public void setScale(float s) {
        scale = s;
    }

    /**
     * HACK WARNING: This is a workaround for a bug that caused time to
     * run backwards. It made some patterns look better, so I left it in
     * and of course, now I need forward time as well. So if you want
     * time to run forward, call setTimeDirectionForward(true) before
     * you call getTime().
     */
    public void setTimeDirectionForward(boolean t) {
        direction = (t) ? 1.0f : -1.0f;
    }

    public void tick() {
        float delta = (float) (current - previous);
        time += scale * delta / 1000.0;
        previous = current;
        current = System.currentTimeMillis();
    }

    public float getTime() {
        return time*direction;
    }
}