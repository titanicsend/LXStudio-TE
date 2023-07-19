package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BoundedParameter;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

@LXCategory("Noise")
public class RainBands extends DriftEnabledPattern {

    NativeShaderPatternEffect effect;
    NativeShader shader;

    public RainBands(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        controls.setRange(TEControlTag.SPEED, 0, -4, 4);
        controls.setValue(TEControlTag.SPEED, 0.5);

        controls.setRange(TEControlTag.QUANTITY,0.15,0.01,1.6);   // field density

        controls.setRange(TEControlTag.SIZE,1,2,0.01);
        controls.setNormalizationCurve(TEControlTag.SIZE, BoundedParameter.NormalizationCurve.REVERSE);
        controls.setExponent(TEControlTag.SIZE,3);

        controls.setRange(TEControlTag.WOW1, 1.5, 8, 0.01);     // relative y scale
        // register common controls with LX
        addCommonControls();

        effect = new NativeShaderPatternEffect("rain_noise.fs",
            new PatternTarget(this));
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

}
