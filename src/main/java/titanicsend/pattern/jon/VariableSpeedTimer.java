package titanicsend.pattern.jon;

// variable speed fake "iTime" timer class
// allows smooth speed changes
class VariableSpeedTimer {
    long previous = 0;
    long current = 0;
    float time = 0.0f;
    float scale = 1.0f;

    VariableSpeedTimer() {
        scale = 1.0f;
        reset();
    }

    void reset() {
        previous = current = System.currentTimeMillis();
        time = 0.0f;
    }

    void setScale(float s) {
        scale = s;
    }

    void tick() {
        float delta = (float) (previous - current);
        time += scale * delta / 1000.0;
        previous = current;
        current = System.currentTimeMillis();
    }

    float getTime() {
        return time;
    }
}