package titanicsend.app.effectmgr;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.effect.StrobeEffect;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.utils.ObservableList;
import java.util.Objects;
import titanicsend.effect.DistortEffect;
import titanicsend.effect.ExplodeEffect;
import titanicsend.effect.RandomStrobeEffect;
import titanicsend.effect.SimplifyEffect;
import titanicsend.effect.SustainEffect;
import titanicsend.util.TE;

@LXCategory(LXCategory.OTHER)
@LXComponentName("Effect Manager")
public class GlobalEffectManager extends LXComponent implements LXOscComponent, LXBus.Listener {

  private static GlobalEffectManager instance;

  public static GlobalEffectManager get() {
    return instance;
  }

  private final ObservableList<GlobalEffect<? extends LXEffect>> mutableSlots =
      new ObservableList<>();
  public final ObservableList<GlobalEffect<? extends LXEffect>> slots =
      mutableSlots.asUnmodifiableList();

  public GlobalEffectManager(LX lx) {
    super(lx, "effectManager");
    instance = this;

    allocateEffectSlots();

    // When effects are added / removed / moved on Master Bus, listen and update
    this.lx.engine.mixer.masterBus.addListener(this.masterBusListener);
    refresh();
  }

  public void allocateSlot(GlobalEffect<? extends LXEffect> effect) {
    Objects.requireNonNull(effect, "May not add null GlobalEffect.effect");
    // TODO: prevent multiple entries for one effect type
    // NOTE(look): ^ I could imagine having 2 versions for an Effect with lots of params,
    // where two versions of the effect are set up very differently.
    mutableSlots.add(effect);
  }

  private void allocateEffectSlots() {
    // TODO: move this TE-specific method somewhere else, keep GlobalEffectManager generic.

    // 0: Random Strobe
    allocateSlot(
        new GlobalEffect<RandomStrobeEffect>() {
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
    allocateSlot(
        new GlobalEffect<StrobeEffect>() {
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
    allocateSlot(
        // TODO: separate effect slots for "sync" version? How to handle "trigger"
        //  (feels more similar to FX patterns like BassLightning / SpaceExplosion)
        new GlobalEffect<ExplodeEffect>() {
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
    allocateSlot(
        new GlobalEffect<SimplifyEffect>() {
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
    allocateSlot(
        new GlobalEffect<SustainEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (effect == null) {
              return null;
            }
            return effect.sustain;
          }
        });

    // 5 - Distort
    allocateSlot(
        new GlobalEffect<DistortEffect>() {
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

  private final LXBus.Listener masterBusListener =
      new LXBus.Listener() {
        @Override
        public void effectAdded(LXBus channel, LXEffect effect) {
          // If this effect matches one of our slots, we still need to check whether it is the first
          // instance or secondary instance in the master effects. To keep things simple we can
          // just refresh our slots every time there's a change.
          refresh();
        }

        @Override
        public void effectRemoved(LXBus channel, LXEffect effect) {
          refresh();
        }

        @Override
        public void effectMoved(LXBus channel, LXEffect effect) {
          refresh();
        }
      };

  /** Refresh all slots */
  private void refresh() {
    for (GlobalEffect<? extends LXEffect> globalEffect : slots) {
      // Allow null placeholder slots
      if (globalEffect == null) {
        continue;
      }

      // Find the first matching global effect for this slot
      boolean found = false;
      for (LXEffect effect : this.lx.engine.mixer.masterBus.effects) {
        if (globalEffect.matches(effect)) {
          // This will quick return if effect is already registered to the slot.
          globalEffect.setEffect(effect);
          found = true;
          break;
        }
      }
      if (!found) {
        globalEffect.setEffect(null);
      }
    }
  }

  public void debugStates() {
    TE.log("-------------------------------");
    for (int i = 0; i < slots.size(); i++) {
      GlobalEffect<? extends LXEffect> globalEffect = slots.get(i);
      if (globalEffect == null) {
        TE.log(String.format("\t[Slot %02d] - null", i));
      } else {
        TE.log(
            String.format(
                "\t[Slot %02d] '%s' - state %s",
                i, globalEffect.getName(), globalEffect.getState()));
      }
    }
    TE.log("-------------------------------");
  }

  @Override
  public void dispose() {
    this.lx.engine.mixer.masterBus.removeListener(this.masterBusListener);
    for (GlobalEffect<? extends LXEffect> globalEffect : slots) {
      if (globalEffect != null) {
        globalEffect.dispose();
      }
    }
    this.mutableSlots.clear();
    super.dispose();
  }
}
