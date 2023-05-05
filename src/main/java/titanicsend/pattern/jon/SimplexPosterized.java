package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

@LXCategory("Noise")
public class SimplexPosterized extends TEPerformancePattern {

    /**
     * Class to support smooth incremental canvas movement over variable-speed time.
     * This lets individual shaders override xOffs/yOffs control behavior and
     * use these controls to set a direction vector for patterns where
     * that makes sense.
     * <p>
     * This rate is based on the real-time clock and is independent of the
     * speed control.
     */
    protected class CanvasTranslator {

        protected double xOffset = 0;
        protected double yOffset = 0;

        void updateTranslation(double deltaMs) {
            // calculate change in position since last frame.
            xOffset += getXPos() * deltaMs / 1000.;
            yOffset += getYPos() * deltaMs / 1000.;
        }

        void reset() {
            xOffset = 0;
            yOffset = 0;
        }
    }

    NativeShaderPatternEffect effect;
    NativeShader shader;

    CanvasTranslator drift = new CanvasTranslator();

    public SimplexPosterized(LX lx) {
        super(lx);

        // common controls setup
        controls.setRange(TEControlTag.SPEED, 0, -4, 4);
        controls.setValue(TEControlTag.SPEED, 0.5);

        controls.setRange(TEControlTag.SIZE, 5, 2, 9);
        controls.setRange(TEControlTag.QUANTITY, 1.5, 3, 0.5);

        // register common controls with LX
        addCommonControls();

        effect = new NativeShaderPatternEffect("simplex_posterized.fs",
            PatternTarget.allPointsAsCanvas(this));
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        drift.updateTranslation(deltaMs);

        // calculate incremental transform based on elapsed time
        shader.setUniform("iTranslate", (float) drift.xOffset, (float) drift.yOffset);

        // run the shader
        effect.run(deltaMs);
    }

    @Override
    // THIS IS REQUIRED if you're not using ConstructedPattern!
    // Initialize the NativeShaderPatternEffect and retrieve the native shader object
    // from it when the pattern becomes active
    public void onActive() {
        super.onActive();
        effect.onActive();
        shader = effect.getNativeShader();
    }

}
