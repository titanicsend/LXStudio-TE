package titanicsend.app.effectmgr;

import heronarts.lx.effect.StrobeEffect;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.TriggerParameter;
import titanicsend.effect.DistortEffect;
import titanicsend.effect.ExplodeEffect;
import titanicsend.effect.RandomStrobeEffect;
import titanicsend.effect.SimplifyEffect;
import titanicsend.effect.SustainEffect;
import titanicsend.pattern.ben.BassLightning;

/**
 * Register TE-specific slots for Global Effects. These provide static locations for effects
 * regardless of the order they are added to a project file, to keep the MIDI controller
 * knobs/buttons [on the MiniLab3] in consistent locations.
 */
public abstract class TEGlobalEffects {

  public static void allocateSlots() {
    final GlobalEffectManager manager = GlobalEffectManager.get();

    // Effect slots (pad + knobs)

    // 0: Random Strobe
    manager.allocateSlot(
        new Slot<RandomStrobeEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (device == null) {
              return null;
            }
            return device.depth;
          }

          @Override
          public LXListenableNormalizedParameter getSecondaryParameter() {
            if (device == null) {
              return null;
            }
            return device.speed;
          }
        });

    // 1 - Strobe
    manager.allocateSlot(
        new Slot<StrobeEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (device == null) {
              return null;
            }
            return device.depth;
          }

          @Override
          public LXListenableNormalizedParameter getSecondaryParameter() {
            if (device == null) {
              return null;
            }
            return device.speed;
          }
        });

    // 2 - Explode
    manager.allocateSlot(
        // TODO: separate effect slots for "sync" version? How to handle "trigger"
        //  (feels more similar to FX patterns like BassLightning / SpaceExplosion)
        new Slot<ExplodeEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (device == null) {
              return null;
            }
            return device.depth;
          }

          @Override
          public LXListenableNormalizedParameter getSecondaryParameter() {
            if (device == null) {
              return null;
            }
            return device.speed;
          }

          @Override
          public TriggerParameter getTriggerParameter() {
            if (device == null) {
              return null;
            }
            return device.trigger;
          }
        });

    // 3 - Simplify
    manager.allocateSlot(
        new Slot<SimplifyEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (device == null) {
              return null;
            }
            return device.amount;
          }

          @Override
          public LXListenableNormalizedParameter getSecondaryParameter() {
            if (device == null) {
              return null;
            }
            return device.gain;
          }
        });

    // 4 - Sustain
    manager.allocateSlot(
        new Slot<SustainEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (device == null) {
              return null;
            }
            return device.sustain;
          }
        });

    // 5 - Distort
    manager.allocateSlot(
        new Slot<DistortEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (device == null) {
              return null;
            }
            return device.depth;
          }

          @Override
          public LXListenableNormalizedParameter getSecondaryParameter() {
            if (device == null) {
              return null;
            }
            return device.size; // or speed?
          }
        });

    // Trigger slots

    // 1 - Bass Lightning
    manager.allocateTriggerSlot(
        new Slot<BassLightning>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (device == null) {
              return null;
            }
            return device.energy;
          }

          @Override
          public TriggerParameter getTriggerParameter() {
            if (device == null) {
              return null;
            }
            return device.trigger;
          }
        });
  }
}
