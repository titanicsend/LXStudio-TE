package titanicsend.pattern.yoffa.framework;

import heronarts.lx.LX;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
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
        System.out.printf("--------\n%s\n--------\n", this.getClass().toString());
        List<LXListenableNormalizedParameter> extraParams = new ArrayList<>();
        // add controls for any parameters found in the created effects
        for (LXParameter parameter : getPatternParameters()) {
            addParameter(parameter.getLabel(), parameter);

            System.out.printf("- patternParam: %s\n", parameter.getLabel());
            // checking because getPatternParameters() is typed as LXParameter (should it be LXListenableNormalizedParameter?)
            if (parameter instanceof LXListenableNormalizedParameter) {
                extraParams.add((LXListenableNormalizedParameter) parameter);
            }
        }
        if (extraParams.size() > 0) {
            this.controls.setRemoteControls(extraParams);
            this.remoteControlsChanged.bang();
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
