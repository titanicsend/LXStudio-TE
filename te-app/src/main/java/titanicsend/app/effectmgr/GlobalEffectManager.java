package titanicsend.app.effectmgr;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.BooleanParameter;
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

  private final ObservableList<GlobalEffect<? extends LXEffect>> mutableSlots =
      new ObservableList<>();
  public final ObservableList<GlobalEffect<? extends LXEffect>> slots =
      this.mutableSlots.asUnmodifiableList();

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

  public GlobalEffectManager register(GlobalEffect<? extends LXEffect> effect) {
    Objects.requireNonNull(effect, "May not add null GlobalEffect.effect");
    // TODO: prevent multiple entries for one effect type
    // NOTE(look): ^ I could imagine having 2 versions for an Effect with lots of params,
    // where two versions of the effect are set up very differently.
    this.mutableSlots.add(effect);
    return this;
  }

  //  public LXEffect effectAtIndex()

  private void registerDefaults() {
    // TODO: move this TE-specific method somewhere else, keep GlobalEffectManager generic.

    // Random Strobe
    register(
        new GlobalEffect<RandomStrobeEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            return effect.depth;
          }

          @Override
          public LXListenableNormalizedParameter getSecondaryParameter() {
            return effect.speed;
          }
        });

    // Explode
    register(
        new GlobalEffect<ExplodeEffect>() {
          @Override
          public BooleanParameter getEnabledParameter() {
            // TODO: return effect.manualTrigger?
            return effect.enabled;
          }

          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            return effect.depth;
          }

          @Override
          public LXListenableNormalizedParameter getSecondaryParameter() {
            return effect.speed;
          }

          @Override
          public TriggerParameter getTriggerParameter() {
            return effect.trigger;
          }
        });

    // Simplify
    register(
        new GlobalEffect<SimplifyEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
            return effect.amount;
          }

          @Override
          public LXListenableNormalizedParameter getSecondaryParameter() {
            return effect.gain;
          }
        });

    // Sustain
    register(
        new GlobalEffect<SustainEffect>() {
          @Override
          public LXListenableNormalizedParameter getLevelParameter() {
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
    refresh();
  }

  @Override
  public void effectMoved(LXBus channel, LXEffect effect) {
    refresh();
  }

  /** Refresh all slots */
  private void refresh() {
    for (GlobalEffect<? extends LXEffect> globalEffect : this.slots) {
      // Allow null placeholder slots
      if (globalEffect == null) {
        continue;
      }

      // Find the first matching global effect for this slot
      for (LXEffect effect : this.lx.engine.mixer.masterBus.effects) {
        if (globalEffect.matches(effect)) {
          // This will quick return if effect is already registered to the slot.
          globalEffect.register(effect);
          break;
        }
      }
    }
  }

  @Override
  public void dispose() {
    this.lx.engine.mixer.masterBus.removeListener(this);
    for (GlobalEffect<? extends LXEffect> globalEffect : this.slots) {
      if (globalEffect != null) {
        globalEffect.dispose();
      }
    }
    super.dispose();
  }
}
