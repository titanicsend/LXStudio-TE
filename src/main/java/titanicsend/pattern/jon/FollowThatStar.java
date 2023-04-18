package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.pattern.yoffa.shader_engine.ShaderOptions;

@LXCategory("Combo FG")
public class FollowThatStar extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    public FollowThatStar(LX lx) {
        super(lx);


        // create new effect with alpha on and no automatic uniforms
        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        // adjust settings of common controls to suit this pattern
        // controls.getLXControl(TEControlTag.QUANTITY).setValue(0.5);
        //controls.getLXControl(TEControlTag.SIZE).setValue(0.2);

        controls.setRange(TEControlTag.SPIN,0,1,-1);

        controls.setRange(TEControlTag.QUANTITY, 5, 1, 10)
                .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

        controls.setRange(TEControlTag.SIZE, 0.2, 0.01, 1);
        controls.setRange(TEControlTag.WOW1, 0.0, 0, 1);
        controls.setRange(TEControlTag.WOW2, 100, 1, 200);

        // register common controls with LX
        addCommonControls();

        effect = new NativeShaderPatternEffect("followthatstar.fs",
                PatternTarget.allPointsAsCanvas(this), options);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {

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
