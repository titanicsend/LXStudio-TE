package titanicsend.app.effectmgr;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.TriggerParameter;
import heronarts.lx.utils.ObservableList;
import java.util.Objects;
import titanicsend.effect.ExplodeEffect;
import titanicsend.effect.RandomStrobeEffect;
import titanicsend.effect.SimplifyEffect;
import titanicsend.effect.SustainEffect;

@LXCategory(LXCategory.OTHER)
@LXComponentName("Effect Manager")
public class GlobalEffectManager extends LXComponent implements LXOscComponent, LXBus.Listener {

  private static GlobalEffectManager instance;

  private static final ObservableList<GlobalEffect<? extends LXEffect>> mutableSlots =
      new ObservableList<>();
  public static final ObservableList<GlobalEffect<? extends LXEffect>> slots =
      mutableSlots.asUnmodifiableList();

  public GlobalEffectManager(LX lx) {
    super(lx, "effectManager");
    GlobalEffectManager.instance = this;

    registerDefaults();

    // When effects are added / removed / moved on Master Bus, listen and update
    this.lx.engine.mixer.masterBus.addListener(this);
    refresh();
  }

  public static GlobalEffectManager get() {
    return instance;
  }

  public void registerSlot(GlobalEffect<? extends LXEffect> effect) {
    Objects.requireNonNull(effect, "May not add null GlobalEffect.effect");
    // TODO: prevent multiple entries for one effect type
    // NOTE(look): ^ I could imagine having 2 versions for an Effect with lots of params,
    // where two versions of the effect are set up very differently.
    mutableSlots.add(effect);
  }

  //  public LXEffect effectAtIndex()

  private void registerDefaults() {
    // TODO: move this TE-specific method somewhere else, keep GlobalEffectManager generic.

    // Random Strobe
    registerSlot(
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

    // Explode
    registerSlot(
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

    // Simplify
    registerSlot(
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

    // Sustain
    registerSlot(
        new GlobalEffect<SustainEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            if (effect == null) {
              return null;
            }
            return effect.sustain;
          }
        });
  }

  @Override
  public void effectAdded(LXBus channel, LXEffect effect) {
    // If this effect matches one of our slots, we still need to check whether it is the first
    // instance or secondary instance in the master effects. To keep things simple we can
    // just refresh our slots every time there's a change.
    refresh();
  }

  @Override
  public void effectRemoved(LXBus channel, LXEffect effect) {
    // TODO: this isn't actually removing a missing effect from 'slots'.
    refresh();
  }

  @Override
  public void effectMoved(LXBus channel, LXEffect effect) {
    refresh();
  }

  /** Refresh all slots */
  private void refresh() {
    for (GlobalEffect<? extends LXEffect> globalEffect : slots) {
      // Allow null placeholder slots
      if (globalEffect == null) {
        continue;
      }

      // Find the first matching global effect for this slot
      for (LXEffect effect : this.lx.engine.mixer.masterBus.effects) {
        if (globalEffect.matches(effect)) {
          // This will quick return if effect is already registered to the slot.
          globalEffect.registerEffect(effect);
          break;
        }
      }
    }
  }

  @Override
  public void dispose() {
    this.lx.engine.mixer.masterBus.removeListener(this);
    for (GlobalEffect<? extends LXEffect> globalEffect : slots) {
      if (globalEffect != null) {
        globalEffect.dispose();
      }
    }
    super.dispose();
  }
}
