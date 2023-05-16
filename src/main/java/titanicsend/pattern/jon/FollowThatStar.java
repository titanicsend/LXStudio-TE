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

@LXCategory("Combo FG")
public class FollowThatStar extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    public FollowThatStar(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);


        // create new effect with alpha on and no automatic uniforms
        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        controls.setRange(TEControlTag.QUANTITY, 5, 1, 10)
                .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

        controls.setRange(TEControlTag.SIZE, 1.75, 1.0, 5);
        controls.setRange(TEControlTag.WOW2, 1, 1, 4);

        // register common controls with LX
        addCommonControls();

        effect = new NativeShaderPatternEffect("followthatstar.fs",
                new PatternTarget(this), options);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {

        shader.setUniform("iRotationAngle",(float) -getRotationAngleFromSpin());

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
