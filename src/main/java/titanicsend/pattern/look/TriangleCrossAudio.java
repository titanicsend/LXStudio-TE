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
public class TriangleCrossAudio extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    public TriangleCrossAudio(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        controls.setValue(TEControlTag.SIZE, 0.46);
        controls.setValue(TEControlTag.WOW1, 0.11);
        controls.setValue(TEControlTag.XPOS, 0.08);
        controls.setValue(TEControlTag.YPOS, -0.04);

        // register common controls with LX
        addCommonControls();

        effect = new NativeShaderPatternEffect("triangle_cross_audio.fs",
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
