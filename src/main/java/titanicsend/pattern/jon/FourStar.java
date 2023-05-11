package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.Tempo;

import heronarts.lx.modulator.Click;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.ObjectParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.pattern.yoffa.shader_engine.ShaderOptions;

// TODO - NEEDS FULL CONVERSION TO COMMON CONTROLS

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

    public FourStar(LX lx) {
        super(lx);

        // create new effect with alpha on and no automatic
        // parameter uniforms

        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        // register common controls with the UI
        addCommonControls();

        // add this pattern's custom controls
        addDivisionParam();
        addParameter("energy", energy);

        tempoDivisionClick.tempoSync.setValue(true);
        tempoDivisionClick.tempoDivision.setValue(tempoDivision.getValue());
        startModulator(tempoDivisionClick);
        tempoDivision.bang();

        effect = new NativeShaderPatternEffect("fourstar.fs",
                new PatternTarget(this, TEShaderView.ALL_PANELS), options);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {

        // Sound reactivity - various brightness features are related to energy
        float e = energy.getValuef();
        shader.setUniform("energy",e);

        // movement speed is beat divided by the current time division
        shader.setUniform("basis",tempoDivisionClick.getBasisf());

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

    @Override
    public String getDefaultView() {
        return effect.getDefaultView();
    }

}
