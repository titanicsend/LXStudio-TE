package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

@LXCategory("Look Shader Patterns")
public class TriangleInfinity extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    public TriangleInfinity(LX lx) {
        super(lx, TEShaderView.DOUBLE_LARGE);

        controls.setRange(TEControlTag.SPEED, 0.5, 0.25, 3.0);
        // number of "layers" of triangles to apply
        controls.setRange(TEControlTag.QUANTITY, 3.0, 1.0, 9.0);
        // Distortion/offset scaling the space between layers
        controls.setRange(TEControlTag.WOW1, 0.9, 0.5, 2.0);
        // "Neon-Ness" (how crisp the lines are)
        controls.setRange(TEControlTag.WOW2, 2.5, 1.0, 3.0);

        // register common controls with LX
        addCommonControls();

        effect = new NativeShaderPatternEffect("triangle_infinity.fs",
                new PatternTarget(this));
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

}
