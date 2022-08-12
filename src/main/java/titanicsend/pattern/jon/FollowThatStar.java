package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.pattern.yoffa.shader_engine.ShaderOptions;

@LXCategory("Combo FG")
public class FollowThatStar extends TEAudioPattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;
    VariableSpeedTimer vTime;
    float lastTimeScale = 0;

    // Controls
    // In this pattern the "energy" is how quickly the scenes can progress,
    // IE shorter tempoDivisions

    public final CompoundParameter starCount = (CompoundParameter)
            new CompoundParameter("Stars", 5, 1, 10)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Number of stars");

    public final CompoundParameter starSize =
            new CompoundParameter("Size", 0.2, 0.01, 1)
                    .setDescription("Base star size");

    public final CompoundParameter glow =
            new CompoundParameter("Glow", 100, 1, 200)
                    .setDescription("Stellar corona");

    public final BooleanParameter rotate =
            new BooleanParameter("Spin", false)
                    .setDescription("Rotation on/off");

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .15, 0, 1)
                    .setDescription("Oh boy...");
    protected final CompoundParameter beatScale = (CompoundParameter)
            new CompoundParameter("Speed", 60, 120, 1)
                    .setExponent(1)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Speed relative to beat");

    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PRIMARY,
                    "Panel Color");

    public FollowThatStar(LX lx) {
        super(lx);
        addParameter("starCount",starCount);
        addParameter("starSize",starSize);
        addParameter("glow",glow);
        addParameter("rotate",rotate);
        addParameter("energy", energy);
        addParameter("beatScale",beatScale);

        // create new effect with alpha on and no automatic
        // parameter uniforms

        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        effect = new NativeShaderPatternEffect("followthatstar.fs",
                PatternTarget.allPointsAsCanvas(this), options);

        vTime = new VariableSpeedTimer();
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {

        vTime.tick();

        // Example of sending a vec3 to a shader.
        // Get the current color and convert to
        // normalized hsb in range 0..1 for openGL
        int baseColor = this.color.calcColor();

        float hn = LXColor.h(baseColor) / 360f;
        float sn = LXColor.s(baseColor) / 100f;
        float bn = LXColor.b(baseColor)/ 100f;

        shader.setUniform("color", hn,sn,bn);
        shader.setUniform("stars", (float) Math.floor(starCount.getValue()));
        shader.setUniform("starSize",starSize.getValuef());
        shader.setUniform("glow",glow.getValuef());
        shader.setUniform("rotate",rotate.getValuef());

        // stars move over time, however fast time is running
        shader.setUniform("vTime",vTime.getTime());

        // set time speed for next frame
        float timeScale = (float) lx.engine.tempo.bpm()/beatScale.getValuef();
        if (timeScale != lastTimeScale) {
            vTime.setScale(timeScale);
            lastTimeScale = timeScale;
        }

        // Sound reactivity - various brightness features are related to energy
        float e = energy.getValuef();
        shader.setUniform("energy",e*e);

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
