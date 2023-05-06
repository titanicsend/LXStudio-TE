package titanicsend.pattern.jon;

import titanicsend.pattern.TEPerformancePattern;

/**
 * Class to support smooth incremental canvas movement over variable-speed time.
 * This lets individual shaders override xOffs/yOffs control behavior and
 * use these controls to set a direction vector for patterns where
 * that makes sense.
 * <p>
 * This rate is based on the real-time clock and is independent of the
 * speed control.
 */
class CanvasTranslator {

    private final TEPerformancePattern pattern;
    protected double xOffset = 0;
    protected double yOffset = 0;

    public CanvasTranslator(TEPerformancePattern pattern) {
        this.pattern = pattern;
    }

    void updateTranslation(double deltaMs) {
        // calculate change in position since last frame.
        xOffset += pattern.getXPos() * deltaMs / 1000.;
        yOffset += pattern.getYPos() * deltaMs / 1000.;
    }

    void reset() {
        xOffset = 0;
        yOffset = 0;
    }
}
