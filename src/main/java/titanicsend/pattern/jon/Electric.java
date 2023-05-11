package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.pattern.yoffa.shader_engine.ShaderOptions;

@LXCategory("Native Shaders Panels")
public class Electric extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    public Electric(LX lx) {
        super(lx);

        controls.setRange(TEControlTag.SPEED, 0.6, -1, 1);
        controls.setRange(TEControlTag.WOW1, 0, 0, 2.6);
        controls.setRange(TEControlTag.WOW2, 0.075, 0.075, 0.3);
        controls.setValue(TEControlTag.SPIN,0.125);

        // register common controls with LX
        addCommonControls();

        effect = new NativeShaderPatternEffect("electric.fs",
                new PatternTarget(this, TEShaderView.DOUBLE_LARGE));
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {

        //shader.setUniform("iRotationAngle",(float) -getRotationAngleFromSpin());

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

    @Override
    public String getDefaultView() {
        return effect.getDefaultView();
    }
}
