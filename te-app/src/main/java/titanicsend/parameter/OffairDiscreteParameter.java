package titanicsend.parameter;

import heronarts.lx.LXComponent;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXMixerEngine;
import heronarts.lx.mixer.LXPatternEngine;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.pattern.LXPattern;
import titanicsend.model.justin.LXVirtualDiscreteParameter;

/**
 * Wraps a discrete parameter and prevents the value from changing while the parent device is "live"
 */
public class OffairDiscreteParameter<T extends DiscreteParameter>
    extends LXVirtualDiscreteParameter<T> {

  public OffairDiscreteParameter(String label, T parameter) {
    super(label, parameter);
  }

  /**
   * Determine is this parameter is Off-Air, aka Not contributing to Live Output. JKB note: This
   * could be sped up by tracking the device and channel.
   */
  private boolean offAir() {
    LXPattern pattern = null;
    boolean firstPatternEngine = true;

    LXComponent parent = this.getParent();
    while (parent != null) {
      switch (parent) {
        case LXEffect effect -> {
          if (!effect.isEnabled()) {
            // If effect is disabled, we're off air
            return true;
          }
        }
        case LXPattern lxPattern -> {
          // Track the pattern so we can see if it is active or composited
          pattern = lxPattern;
        }
        case LXPatternEngine.Container container -> {
          LXPatternEngine patternEngine = container.getPatternEngine();
          if (firstPatternEngine) {
            firstPatternEngine = false;
            if (pattern != null) {
              if (patternEngine.isComposite()) {
                if (pattern.compositeLevel.getValue() == 0) {
                  // A composite pattern at zero composite level is off air
                  return true;
                }
              } else if (patternEngine.getActivePattern() != pattern
                  && (!patternEngine.isInTransition()
                      || (patternEngine.getNextPattern() != pattern))) {
                // Non-composite engine, and pattern is not active or next
                return true;
              }
            }
          }
          // A container could also be a channel
          if (container instanceof LXAbstractChannel channel && isChannelOffAir(channel)) {
            return true;
          }
        }
        case LXAbstractChannel abstractChannel -> {
          // If channel disabled, fader down, or auto-muted, we're off air.
          if (isChannelOffAir(abstractChannel)) {
            return true;
          }
        }
        case LXMixerEngine lxMixerEngine -> {
          // Reached the top of the mixer tree
          break;
        }
        default -> {}
      }
      // Check the next parent up the hierarchy
      parent = parent.getParent();
    }

    // The device is live
    bang();
    return false;
  }

  private boolean isChannelOffAir(LXAbstractChannel abstractChannel) {
    return !abstractChannel.enabled.isOn()
        || abstractChannel.fader.getValue() == 0
        || abstractChannel.isAutoMuted.isOn();
  }

  @Override
  public DiscreteParameter setNormalized(double value) {
    if (offAir()) {
      super.setNormalized(value);
    }
    return this;
  }

  @Override
  public LXListenableNormalizedParameter incrementNormalized(double amount) {
    if (offAir()) {
      super.incrementNormalized(amount);
    }
    return this;
  }

  @Override
  public LXListenableNormalizedParameter incrementNormalized(double amount, boolean wrap) {
    if (offAir()) {
      super.incrementNormalized(amount, wrap);
    }
    return this;
  }

  @Override
  public DiscreteParameter increment() {
    if (offAir()) {
      super.increment();
    }
    return this;
  }

  @Override
  public DiscreteParameter decrement() {
    if (offAir()) {
      super.decrement();
    }
    return this;
  }

  @Override
  public LXParameter reset() {
    if (offAir()) {
      super.reset();
    }
    return this;
  }
}
