package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

@LXCategory("Native Shaders Edges")
public class ElectricEdges extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    public ElectricEdges(LX lx) {
        super(lx, TEShaderView.ALL_EDGES);

        markUnusedControl(TEControlTag.BRIGHTNESS);
        markUnusedControl(TEControlTag.WOWTRIGGER);

        // Set control range -- this uses the same shader as the electric panel
        // pattern, but it is parameterized *very* differently.
        controls.setRange(TEControlTag.SIZE, 0.05, 0.005, 0.4);    // noise scale
        controls.setRange(TEControlTag.SPEED, 1, -2, 2);           // arc wave speed
        controls.setRange(TEControlTag.QUANTITY, 0.5, 0.5, 1);         // base glow level
        controls.setRange(TEControlTag.WOW1, 0.8, 0, 2.6);         // radial coord distortion
        controls.setRange(TEControlTag.WOW2, 0.02, 0.0, 0.2);  // noise field amplitude
        controls.setRange(TEControlTag.SPIN, 0.6, -3, 3);          // arc spin rate

        // register common controls with LX
        addCommonControls();

        effect = new NativeShaderPatternEffect("electric.fs",
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
