package titanicsend.pattern.yoffa.framework;

import heronarts.lx.GpuDevice;
import heronarts.lx.LX;
import heronarts.lx.parameter.LXParameter;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import titanicsend.pattern.TEPerformancePattern;

public abstract class ConstructedPattern extends TEPerformancePattern implements GpuDevice {

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
    // clearColors();
    for (PatternEffect effect : effects) {
      effect.run(deltaMillis);
    }
  }

  protected abstract List<PatternEffect> createEffects();
}
