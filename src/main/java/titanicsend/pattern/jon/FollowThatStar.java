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

    // Non-standard Controls

    public final CompoundParameter glow =
            new CompoundParameter("Glow", 100, 1, 200)
                    .setDescription("Stellar corona");

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .15, 0, 1)
                    .setDescription("Oh boy...");

    protected final CompoundParameter beatScale = (CompoundParameter)
            new CompoundParameter("BeatScale", 60, 120, 1)
                    .setExponent(1)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Speed relative to beat");

    public FollowThatStar(LX lx) {
        super(lx);

        // adjust settings of common controls to suit this pattern
        controls.getLXControl(TEControlTag.QUANTITY).setValue(0.5);
        controls.getLXControl(TEControlTag.SIZE).setValue(0.2);

        addParameter("glow", glow);
        addParameter("energy", energy);
        addParameter("beatScale", beatScale);

        // create new effect with alpha on and no automatic
        // parameter uniforms

        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        effect = new NativeShaderPatternEffect("followthatstar.fs",
                PatternTarget.allPointsAsCanvas(this), options);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {

        shader.setUniform("iQuantity", 1f + (float) Math.floor(getQuantity() * 9));
        shader.setUniform("iScale", 0.01f + (float) getSize());
        shader.setUniform("glow", glow.getValuef());

        // cycle speed is roughly synced to some multiple of the beat.
        iTime.setScale(getSpeed() * (float) lx.engine.tempo.bpm() / beatScale.getValuef());

        // Sound reactivity - various brightness features are related to energy
        float e = energy.getValuef();
        shader.setUniform("energy", e * e);

        // run the shader
        effect.run(deltaMs);
    }

    @Override
    // THIS IS REQUIRED if you're not using ConstructedPattern!
    // Initialize the NativeShaderPatternEffect and retrieve the native shader object
    // from it when the pattern becomes active
    public void onActive() {
        effect.onActive();
        shader = effect.getNativeShader();
    }

}
