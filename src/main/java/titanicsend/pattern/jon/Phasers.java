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

@LXCategory("Native Shaders Panels")
public class Phasers extends TEAudioPattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;
    VariableSpeedTimer vTime;

    // Controls
    // In this pattern the "energy" is how quickly the scenes can progress,
    // IE shorter tempoDivisions

    public final CompoundParameter beamCount1 = (CompoundParameter)
            new CompoundParameter("Beams1", 2, 1, 8)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Beam Count 1");

    public final CompoundParameter beamCount2 = (CompoundParameter)
            new CompoundParameter("Beams2", 2, 1, 8)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Beam Count 2");

    public final CompoundParameter glow =
            new CompoundParameter("Fog", 0.75, 0, 2)
                    .setDescription("Fog glow level");

    public final CompoundParameter hScan =
            new CompoundParameter("Dance", 0.000, 0., 0.035)
                    .setDescription("Horizontal Movement");

    public final BooleanParameter vScan =
            new BooleanParameter("Scan", false)
                    .setDescription("Vertical movement");

    public final BooleanParameter rotate =
            new BooleanParameter("Spin", false)
                    .setDescription("Rotation on/off");

    public final BooleanParameter reverse =
            new BooleanParameter("Reverse", false)
                    .setDescription("All spin backwards!");

    public final CompoundParameter yPos1 =
            new CompoundParameter("yPos1", 0f, -0.5, 0.5)
                    .setDescription("Beam 1 Y");

    public final CompoundParameter yPos2 =
            new CompoundParameter("yPos2", 0f, -0.5, 0.5)
                    .setDescription("Beam 2 Y");

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .15, 0, 1)
                    .setDescription("Oh boy...");

    protected final CompoundParameter beatScale = (CompoundParameter)
            new CompoundParameter("Speed", 60, 120, 1)
                    .setExponent(1)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Overall movement speed");



    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PANEL,
                    "Panel Color");

    public Phasers(LX lx) {
        super(lx);
        addParameter("beamCount1",beamCount1);
        addParameter("beamCount2",beamCount2);
        addParameter("glow",glow);
        addParameter("hScan",hScan);
        addParameter("vScan",vScan);
        addParameter("rotate",rotate);
        addParameter("reverse",reverse);
        addParameter("yPos1",yPos1);
        addParameter("yPos2",yPos2);
        addParameter("energy", energy);
        addParameter("beatScale",beatScale);

        // create new effect with alpha on and no automatic
        // parameter uniforms

        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        effect = new NativeShaderPatternEffect("phasers.fs",
                PatternTarget.allPanelsAsCanvas(this), options);

        vTime = new VariableSpeedTimer();
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        vTime.tick();

        // Example of sending a vec3 to a shader.
        // Get the current color and convert to
        // normalized rgb in range 0..1 for openGL
        int baseColor = this.color.calcColor();

        float rn = (float) (0xff & LXColor.red(baseColor)) / 255f;
        float gn = (float) (0xff & LXColor.green(baseColor)) / 255f;
        float bn = (float) (0xff & LXColor.blue(baseColor)) / 255f;

        float rev = (reverse.getValuef() != 0) ? 1f : -1f;

        shader.setUniform("color", rn, gn, bn);

        shader.setUniform("beamCount1",beamCount1.getValuef());
        shader.setUniform("beamCount2",beamCount2.getValuef());

        shader.setUniform("glow",glow.getValuef());
        shader.setUniform("hScan",hScan.getValuef());
        shader.setUniform("vScan",rev * vScan.getValuef());
        shader.setUniform("yPos1",yPos1.getValuef());
        shader.setUniform("yPos2",yPos2.getValuef());
        shader.setUniform("rotate",rev * rotate.getValuef());

        // calculate beats/sec for light "spin rate"
        shader.setUniform("vTime",vTime.getTime());

        // set time speed for next frame
        float beat = (float) lx.engine.tempo.bpm();
        float scale = beatScale.getValuef();
        vTime.setScale(beat/scale);


        // Sound reactivity - various brightness features are related to energy
        float e = energy.getValuef();
        shader.setUniform("energy",e*e);

        // movement speed is beat divided by the current time division
        //shader.setUniform("basis",tempoDivisionClick.getBasisf());

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
