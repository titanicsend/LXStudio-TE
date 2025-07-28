package titanicsend.effect;

import heronarts.lx.parameter.LXListenableNormalizedParameter;

public interface TEPerformanceEffect {

  // One knob to expose
  LXListenableNormalizedParameter getParam();

  // Behavior when triggered
  void trigger();
}
