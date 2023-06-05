package titanicsend.pattern.yoffa.framework;

import heronarts.lx.LX;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;

import java.util.*;
import java.util.stream.Collectors;

public abstract class ConstructedPattern extends TEPerformancePattern {

    private final List<PatternEffect> effects;

    protected ConstructedPattern(LX lx, TEShaderView defaultView) {
        super(lx, defaultView);

        effects = createEffects();

        // initialize common controls
        addCommonControls();

        // add controls for any parameters found in the created effects
        for (LXParameter parameter : getPatternParameters()) {
            addParameter(parameter.getLabel(), parameter);
        }
    }

    protected Collection<LXParameter> getPatternParameters() {
        return effects.stream()
                .map(PatternEffect::getParameters)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public void onActive() {
        super.onActive();
        for (PatternEffect effect : effects) {
            effect.onActive();
        }
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
        super.onParameterChanged(parameter);
        for (PatternEffect effect : effects) {
            effect.onParameterChanged(parameter);
        }
    }

    public void onInactive() {
        super.onInactive();
        for (PatternEffect effect : effects) {
            effect.onPatternInactive();
        }
    }

    @Override
    protected void runTEAudioPattern(double deltaMillis) {
        //clearColors();
        for (PatternEffect effect : effects) {
            effect.run(deltaMillis);
        }
    }

    protected abstract List<PatternEffect> createEffects();

}
