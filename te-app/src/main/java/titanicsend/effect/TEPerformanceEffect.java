package titanicsend.effect;

import heronarts.lx.parameter.LXListenableNormalizedParameter;

/** Effects for which we want easy trigger access on a dedicated MIDI controller. */
public interface TEPerformanceEffect {

  // Knobs to expose
  LXListenableNormalizedParameter primaryParam();

  LXListenableNormalizedParameter secondaryParam();

  // Behavior when triggered
  void trigger();
}
