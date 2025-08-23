package titanicsend.app.effectmgr;

import heronarts.lx.effect.StrobeEffect;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.TriggerParameter;
import titanicsend.effect.DistortEffect;
import titanicsend.effect.ExplodeEffect;
import titanicsend.effect.RandomStrobeEffect;
import titanicsend.effect.SimplifyEffect;
import titanicsend.effect.SustainEffect;
import titanicsend.pattern.ben.BassLightning;
import titanicsend.pattern.jon.FxLaserCharge;
import titanicsend.pattern.jon.TEControlTag;

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
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.depth;
          }

          @Override
          protected LXListenableNormalizedParameter _getSecondaryParameter() {
            return device.speed;
          }
        });

    // 1 - Strobe
    manager.allocateSlot(
        new Slot<StrobeEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.depth;
          }

          @Override
          protected LXListenableNormalizedParameter _getSecondaryParameter() {
            return device.speed;
          }
        });

    // 2 - Explode
    manager.allocateSlot(
        // TODO: separate effect slots for "sync" version? How to handle "trigger"
        //  (feels more similar to FX patterns like BassLightning / SpaceExplosion)
        new Slot<ExplodeEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.depth;
          }

          @Override
          protected LXListenableNormalizedParameter _getSecondaryParameter() {
            return device.speed;
          }

          @Override
          protected TriggerParameter _getTriggerParameter() {
            return device.trigger;
          }
        });

    // 3 - Simplify
    manager.allocateSlot(
        new Slot<SimplifyEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.amount;
          }

          @Override
          protected LXListenableNormalizedParameter _getSecondaryParameter() {
            return device.gain;
          }
        });

    // 4 - Sustain
    manager.allocateSlot(
        new Slot<SustainEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.sustain;
          }
        });

    // 5 - Distort
    manager.allocateSlot(
        new Slot<DistortEffect>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.depth;
          }

          @Override
          protected LXListenableNormalizedParameter _getSecondaryParameter() {
            return device.size; // or speed?
          }
        });

    // Trigger slots

    // 19 (4th white key from right) - FxLaserCharge "Slow" (requires preset!)
    manager.allocateTriggerSlot(
        19,
        new Slot<FxLaserCharge>("Slow") {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.getControls().getLXControl(TEControlTag.WOW2);
          }

          @Override
          protected BooleanParameter _getTriggerParameter() {
            return (BooleanParameter) device.getControls().getLXControl(TEControlTag.WOWTRIGGER);
          }
        });

    // 21 (3rd white key from right) - FxLaserCharge "Fast" (requires preset!)
    manager.allocateTriggerSlot(
        21,
        new Slot<FxLaserCharge>("Fast") {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.getControls().getLXControl(TEControlTag.WOW2);
          }

          @Override
          protected BooleanParameter _getTriggerParameter() {
            return (BooleanParameter) device.getControls().getLXControl(TEControlTag.WOWTRIGGER);
          }
        });

    // Example of non-linear allocation:
    // 22 (3rd to last key) - Bass Lightning
    manager.allocateTriggerSlot(
        23,
        new Slot<BassLightning>() {
          @Override
          protected LXListenableNormalizedParameter _getLevelParameter() {
            return device.energy;
          }

          @Override
          protected BooleanParameter _getTriggerParameter() {
            return device.trigger;
          }
        });
  }
}
