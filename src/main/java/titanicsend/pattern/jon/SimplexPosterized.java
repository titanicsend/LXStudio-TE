package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

@LXCategory("Noise")
public class SimplexPosterized extends DriftEnabledPattern {

    NativeShaderPatternEffect effect;
    NativeShader shader;

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
            new PatternTarget(this, TEShaderView.ALL_POINTS));
    }

    @Override
    protected void runTEAudioPattern(double deltaMs) {

        // calculate incremental transform based on elapsed time
        shader.setUniform("iTranslate", (float) getXPosition(), (float) getYPosition());

        // run the shader
        effect.run(deltaMs);
    }

    @Override
    // THIS IS REQUIRED if you're not using ConstructedPattern!
    // Initialize the NativeShaderPatternEffect and retrieve the native shader object
    // from it when the pattern becomes active
    protected void onActive() {
        super.onActive();
        effect.onActive();
        shader = effect.getNativeShader();
    }

    @Override
    public String getDefaultView() {
        return effect.getDefaultView();
    }
}
