package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.Tempo;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;

import heronarts.lx.modulator.Click;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.ObjectParameter;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.pattern.yoffa.shader_engine.ShaderOptions;

@LXCategory("Native Shaders Panels")
public class FourStar extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    // Controls
    // In this pattern the "energy" is how quickly the scenes can progress,
    // IE shorter tempoDivisions
    Tempo.Division[] divisions;
    public ObjectParameter<Tempo.Division> tempoDivision;

    public final Click tempoDivisionClick =
            new Click("Tempo Division", lx.engine.tempo.period);

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .5, 0, 1)
                    .setDescription("Oh boy...");

    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PRIMARY,
                    "Panel Color");

    public FourStar(LX lx) {
        super(lx);
        addDivisionParam();
        addParameter("energy", energy);

        tempoDivisionClick.tempoSync.setValue(true);
        tempoDivisionClick.tempoDivision.setValue(tempoDivision.getValue());
        startModulator(tempoDivisionClick);
        tempoDivision.bang();

        // create new effect with alpha on and no automatic
        // parameter uniforms

        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        effect = new NativeShaderPatternEffect("fourstar.fs",
                PatternTarget.allPanelsAsCanvas(this), options);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {

        // Example of sending a vec3 to a shader.
        // Get the current color and convert to
        // normalized rgb in range 0..1 for openGL
        int baseColor = this.color.calcColor();

        float rn = (float) (0xff & LXColor.red(baseColor)) / 255f;
        float gn = (float) (0xff & LXColor.green(baseColor)) / 255f;
        float bn = (float) (0xff & LXColor.blue(baseColor)) / 255f;
        shader.setUniform("color", rn, gn, bn);

        // Sound reactivity - various brightness features are related to energy
        float e = energy.getValuef();
        shader.setUniform("energy",e);

        // movement speed is beat divided by the current time division
        shader.setUniform("basis",tempoDivisionClick.getBasisf());
        shader.setUniform("theta",getRotationAngle());

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

    @Override
    public void onParameterChanged(LXParameter parameter) {
        super.onParameterChanged(parameter);
        if (parameter.equals(tempoDivision)) {
            tempoDivisionClick.tempoDivision.setValue(tempoDivision.getObject());
        }
    }

    private void addDivisionParam() {
        divisions = new Tempo.Division[]{
                Tempo.Division.EIGHT,
                Tempo.Division.FOUR,
                Tempo.Division.DOUBLE,
                Tempo.Division.WHOLE,
                Tempo.Division.HALF,
                Tempo.Division.QUARTER,
                Tempo.Division.EIGHTH,
                Tempo.Division.SIXTEENTH
        };

        tempoDivision = new ObjectParameter<>("Division", divisions, divisions[5])
                .setDescription("Tempo division");
        tempoDivision.setWrappable(false);

        addParameter("tempoDivision", tempoDivision);
    }


}
