package titanicsend.effect;

import heronarts.lx.parameter.LXListenableNormalizedParameter;

/** Effects for which we want easy trigger access on a dedicated MIDI controller. */
public interface TEPerformanceEffect {

  // Knobs to expose
  LXListenableNormalizedParameter primaryParam();

  LXListenableNormalizedParameter secondaryParam();

  // Behavior when triggered
  void trigger();

  // Idea: use these to annotate certain effects if they're only safe for GPU/CPU
  /*
  default boolean gpuMixerCompatible() {
    return true;
  }

  default boolean cpuMixerCompatible() {
    return true;
  }
  */

  // Idea: for anything TE-model specific (e.g. BassLighting)
  /*
  default boolean teModelOnly() {
    return false;
  }
  */
}
