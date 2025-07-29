package titanicsend.app.effectmgr;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.osc.LXOscComponent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import titanicsend.audio.LOG;
import titanicsend.effect.*;

@LXCategory(LXCategory.OTHER)
@LXComponentName("Effect Manager")
public class GlobalEffectManager extends LXComponent implements LXOscComponent, LXBus.Listener {

  public static final int NUM_EFFECT_SLOTS = 8;
  public static final List<Class<? extends TEPerformanceEffect>> EFFECT_SLOTS =
      List.of(
          RandomStrobeEffect.class,
          ExplodeEffect.class,
          SimplifyEffect.class,
          SustainEffect.class,
          null,
          null,
          null,
          null);

  private static GlobalEffectManager instance;

  private final List<TEPerformanceEffect> mutableSlots = new ArrayList<>(NUM_EFFECT_SLOTS);
  public final List<TEPerformanceEffect> slots = Collections.unmodifiableList(this.mutableSlots);

  public GlobalEffectManager(LX lx) {
    super(lx, "effectManager");
    GlobalEffectManager.instance = this;

    // When effects are added / removed / moved on Master Bus, listen and update
    this.lx.engine.mixer.masterBus.addListener(this);
  }

  public static GlobalEffectManager get() {
    return instance;
  }

  @Override
  public void effectAdded(LXBus channel, LXEffect effect) {
    if (isPerformanceEffect(effect) && !mutableSlots.contains((TEPerformanceEffect) effect)) {
      int slotIndex = EFFECT_SLOTS.indexOf(effect.getClass());
      if (slotIndex >= 0) {
        mutableSlots.set(slotIndex, (TEPerformanceEffect) effect);
        LOG.log("TEPerformanceEffect added to slot " + slotIndex + ": " + effect.getLabel());
      }
    }
  }

  @Override
  public void effectRemoved(LXBus channel, LXEffect effect) {
    if (isPerformanceEffect(effect) && mutableSlots.contains((TEPerformanceEffect) effect)) {
      int slotIndex = EFFECT_SLOTS.indexOf(effect.getClass());
      if (slotIndex >= 0) {
        mutableSlots.set(slotIndex, null);
        LOG.log("TEPerformanceEffect removed: " + effect.getLabel());
      }
    }
  }

  @Override
  public void effectMoved(LXBus channel, LXEffect effect) {}

  private static boolean isPerformanceEffect(LXEffect effect) {
    return effect instanceof TEPerformanceEffect;
    // NOTE(look): alternative approach if instanceof is not working
    // System.out.println(TEPerformanceEffect.class.isAssignableFrom(effect.getClass());
  }

  @Override
  public void dispose() {
    this.lx.engine.mixer.masterBus.removeListener(this);
    super.dispose();
  }
}
