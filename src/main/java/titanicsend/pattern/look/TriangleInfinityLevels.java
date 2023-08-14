package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.ConstructedPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

import java.util.List;

@LXCategory("Look Shader Patterns")
public class TriangleInfinityLevels extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    public TriangleInfinityLevels(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);
//        controls.setValue(TEControlTag.YPOS, -0.14);
        controls.setRange(TEControlTag.SIZE, 1.20, 0.00, 3.0);
        controls.setRange(TEControlTag.SPEED, 0.01, 0.00, 0.5);
        controls.setRange(TEControlTag.QUANTITY, 8.0, 2.0, 12.0);
        controls.setRange(TEControlTag.WOW1, 0.04, 0.0, 0.5);
        controls.setRange(TEControlTag.WOW2, 1.1, 1.0, 3.0);
        controls.setValue(TEControlTag.BRIGHTNESS, 0.5);

        addCommonControls();

        effect = new NativeShaderPatternEffect("triangle_infinity.fs", new PatternTarget(this));
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        shader.setUniform("brightnessDampening", 0.5f);
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
