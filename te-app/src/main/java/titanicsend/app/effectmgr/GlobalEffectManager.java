package titanicsend.app.effectmgr;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.LXComponent;
import heronarts.lx.LXComponentName;
import heronarts.lx.LXDeviceComponent;
import heronarts.lx.effect.LXEffect;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.utils.ObservableList;
import java.util.Objects;
import titanicsend.util.TE;

@LXCategory(LXCategory.OTHER)
@LXComponentName("Effect Manager")
public class GlobalEffectManager extends LXComponent implements LXOscComponent, LXBus.Listener {

  private static GlobalEffectManager instance;

  public static GlobalEffectManager get() {
    return instance;
  }

  private final ObservableList<Slot<? extends LXDeviceComponent>> mutableSlots =
      new ObservableList<>();
  public final ObservableList<Slot<? extends LXDeviceComponent>> slots =
      mutableSlots.asUnmodifiableList();

  private final ChannelTracker channelTracker;

  public GlobalEffectManager(LX lx) {
    super(lx, "effectManager");
    instance = this;

    // When effects are added / removed / moved on Master Bus, listen and update
    this.lx.engine.mixer.masterBus.addListener(this.masterBusListener);
    refresh();

    // Actively watch for a channel labeled "FX"
    this.channelTracker = new ChannelTracker(lx, "FX");
    this.channelTracker.addListener(this.channelTrackerListener);
  }

  public void allocateSlot(Slot<? extends LXDeviceComponent> slot) {
    Objects.requireNonNull(slot, "May not add null Slot");
    // TODO: prevent multiple entries for one effect type
    // NOTE(look): ^ I could imagine having 2 versions for an Effect with lots of params,
    // where two versions of the effect are set up very differently.
    mutableSlots.add(slot);
  }

  /** Master effects */
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

  /** "FX" Channel */
  private final ChannelTracker.Listener channelTrackerListener =
      (tracker, channel) -> {
        if (channel != null) {
          // Note the mixer will re-index channels, so this value may not stay the same:
          verbose("Found FX Channel at index " + channel.getIndex());
        } else {
          verbose("Lost connection to FX Channel");
        }
        refresh();
      };

  // Slot contents

  /** Refresh all slots */
  private void refresh() {
    for (Slot<? extends LXDeviceComponent> slot : slots) {
      // Allow null placeholder slots
      if (slot == null) {
        continue;
      }

      // Find a matching global effect for this slot
      boolean found = false;
      for (LXEffect effect : this.lx.engine.mixer.masterBus.effects) {
        if (slot.matches(effect)) {
          // This will quick return if effect is already registered to the slot.
          slot.setDevice(effect);
          found = true;
          break;
        }
      }

      // Check for matching pattern in the FX channel
      LXChannel fxChannel = this.channelTracker.getChannel();
      if (fxChannel != null) {
        // TODO
      }

      if (!found) {
        slot.setDevice(null);
      }
    }
  }

  public void debugStates() {
    TE.log("-------------------------------");
    for (int i = 0; i < slots.size(); i++) {
      Slot<? extends LXDeviceComponent> slot = slots.get(i);
      if (slot == null) {
        TE.log(String.format("\t[Slot %02d] - null", i));
      } else {
        TE.log(String.format("\t[Slot %02d] '%s' - state %s", i, slot.getName(), slot.getState()));
      }
    }
    TE.log("-------------------------------");
  }

  @Override
  public void dispose() {
    // Stop listening to external events
    this.lx.engine.mixer.masterBus.removeListener(this.masterBusListener);
    this.channelTracker.removeListener(this.channelTrackerListener);

    // Clear all slots
    for (Slot<? extends LXDeviceComponent> slot : slots) {
      if (slot != null) {
        // Disposing a slot sets the target to null, then the MIDI controller will unregister
        slot.dispose();
      }
    }
    this.mutableSlots.clear();

    // Dispose children
    this.channelTracker.dispose();

    super.dispose();
  }

  private void verbose(String message) {
    TE.log("GlobalEffectManager: " + message);
  }
}
