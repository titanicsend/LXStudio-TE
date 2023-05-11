package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.pattern.yoffa.shader_engine.ShaderOptions;
import titanicsend.util.TEMath;

// TODO - NEEDS FULL CONVERSION TO COMMON CONTROLS

@LXCategory("Other")
public class Iceflow extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    float lastTimeScale = 0;

    // Controls
    // In this pattern the "energy" is how quickly the scenes can progress,
    // IE shorter tempoDivisions

    protected final CompoundParameter focus =
            new CompoundParameter("Detail", 5, 4, 8)
                    .setDescription("Detail/Sharpness");

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .225, 0, 1)
                    .setDescription("Oh boy...");
    protected final CompoundParameter beatScale = (CompoundParameter)
            new CompoundParameter("Speed", 60, 100, 10)
                    .setExponent(1)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Speed relative to beat");

    public Iceflow(LX lx) {
        super(lx);

        // create new effect with alpha on and no automatic
        // parameter uniforms

        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        // register common controls with the UI
        addCommonControls();

        // Add this pattern's custom controls.
        addParameter("focus",focus);
        addParameter("energy", energy);
        addParameter("beatScale",beatScale);

        effect = new NativeShaderPatternEffect("iceflow.fs",
                new PatternTarget(this, TEShaderView.ALL_PANELS), options);

    }

    @Override
    public void runTEAudioPattern(double deltaMs) {


        // Example of sending a vec3 to a shader.
        // Get the current color and convert to
        // normalized hsb in range 0..1 for openGL
        int baseColor = this.controls.color.calcColor();
        float hn = LXColor.h(baseColor) / 360f;
        float sn = LXColor.s(baseColor) / 100f;
        float bn = LXColor.b(baseColor) / 100f;

        shader.setUniform("color", hn,sn,bn);
        shader.setUniform("focus",focus.getValuef());

        /*
        // set time speed for next frame. This moves w/measure rather than beat
        float timeScale = (float) lx.engine.tempo.bpm()/beatScale.getValuef() / 4.0f;
        if (timeScale != lastTimeScale) {
            vTime.setScale(timeScale);
            lastTimeScale = timeScale;
        }

        // movement over time, however fast time is running
        shader.setUniform("vTime",vTime.getTime());

         */

        // Sound reactivity - various brightness features are related to energy
        float e = energy.getValuef();
        float phase = 0.05f * TEMath.wavef((float) (measure() * 2)) * e;
        shader.setUniform("phase",phase);
        shader.setUniform("energy",e*e);

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
