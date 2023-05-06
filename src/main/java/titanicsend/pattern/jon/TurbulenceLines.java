package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

@LXCategory("Noise")
public class TurbulenceLines extends TEPerformancePattern {

    NativeShaderPatternEffect effect;
    NativeShader shader;

    CanvasTranslator drift;

    public TurbulenceLines(LX lx) {
        super(lx);

        drift = new CanvasTranslator(this);

        // common controls setup
        controls.setRange(TEControlTag.SPEED, 0,-4, 4);
        controls.setValue(TEControlTag.SPEED,0.5);

        controls.setRange(TEControlTag.SIZE, 1,0.4, 1.25);
        controls.setRange(TEControlTag.QUANTITY,60,20, 200);
        controls.setRange(TEControlTag.WOW1,0.5,0, 1);

        controls.setValue(TEControlTag.XPOS,0.25);
        controls.setValue(TEControlTag.YPOS,-0.25);

        // register common controls with LX
        addCommonControls();

        effect = new NativeShaderPatternEffect("turbulent_noise_lines.fs",
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
