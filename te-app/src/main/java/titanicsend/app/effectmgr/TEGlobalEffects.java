package titanicsend.app.effectmgr;

import heronarts.lx.effect.StrobeEffect;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.TriggerParameter;
import titanicsend.effect.DistortEffect;
import titanicsend.effect.ExplodeEffect;
import titanicsend.effect.RandomStrobeEffect;
import titanicsend.effect.SimplifyEffect;
import titanicsend.effect.SustainEffect;

/**
 * Register TE-specific slots for Global Effects. These provide static locations for effects
 * regardless of the order they are added to a project file, to keep the MIDI controller
 * knobs/buttons [on the MiniLab3] in consistent locations.
 */
public abstract class TEGlobalEffects {

  public static void allocateSlots() {
    final GlobalEffectManager manager = GlobalEffectManager.get();

    // 0: Random Strobe
    manager.allocateSlot(
        new Slot<RandomStrobeEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (effect == null) {
              return null;
            }
            return effect.depth;
          }

          @Override
          public LXListenableNormalizedParameter getSecondaryParameter() {
            if (effect == null) {
              return null;
            }
            return effect.speed;
          }
        });

    // 1 - Strobe
    manager.allocateSlot(
        new Slot<StrobeEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (effect == null) {
              return null;
            }
            return effect.depth;
          }

          @Override
          public LXListenableNormalizedParameter getSecondaryParameter() {
            if (effect == null) {
              return null;
            }
            return effect.speed;
          }
        });

    // 2 - Explode
    manager.allocateSlot(
        // TODO: separate effect slots for "sync" version? How to handle "trigger"
        //  (feels more similar to FX patterns like BassLightning / SpaceExplosion)
        new Slot<ExplodeEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (effect == null) {
              return null;
            }
            return effect.depth;
          }

          @Override
          public LXListenableNormalizedParameter getSecondaryParameter() {
            if (effect == null) {
              return null;
            }
            return effect.speed;
          }

          @Override
          public TriggerParameter getTriggerParameter() {
            if (effect == null) {
              return null;
            }
            return effect.trigger;
          }
        });

    // 3 - Simplify
    manager.allocateSlot(
        new Slot<SimplifyEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (effect == null) {
              return null;
            }
            return effect.amount;
          }

          @Override
          public LXListenableNormalizedParameter getSecondaryParameter() {
            if (effect == null) {
              return null;
            }
            return effect.gain;
          }
        });

    // 4 - Sustain
    manager.allocateSlot(
        new Slot<SustainEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (effect == null) {
              return null;
            }
            return effect.sustain;
          }
        });

    // 5 - Distort
    manager.allocateSlot(
        new Slot<DistortEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (effect == null) {
              return null;
            }
            return effect.depth;
          }

          @Override
          public LXListenableNormalizedParameter getSecondaryParameter() {
            if (effect == null) {
              return null;
            }
            return effect.size; // or speed?
          }
        });
  }
}
