package titanicsend.app.effectmgr;

import heronarts.lx.LX;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.mixer.LXGroup;
import heronarts.lx.mixer.LXMixerEngine;
import heronarts.lx.parameter.LXParameterListener;
import heronarts.lx.utils.LXUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Monitors the LX Mixer for a LXChannel (non-abstract) of a specific name */
public class ChannelTracker {

  public interface Listener {
    void channelChanged(ChannelTracker tracker, LXChannel channel);
  }

  private final List<Listener> listeners = new ArrayList<>();

  private final LX lx;
  private final String channelName;

  private LXChannel channel;

  public ChannelTracker(LX lx, String channelName) {
    if (LXUtils.isEmpty(channelName)) {
      throw new IllegalArgumentException("ChannelTracker requires non-null channel name");
    }

    this.lx = lx;
    this.channelName = channelName;

    this.lx.engine.mixer.addListener(this.mixerListener);
  }

  private final LXParameterListener labelListener =
      lxParameter -> {
        // If a label was changed on ANY LXChannel, re-scan
        refresh();
      };

  private final LXMixerEngine.Listener mixerListener =
      new LXMixerEngine.Listener() {
        @Override
        public void channelAdded(LXMixerEngine lxMixerEngine, LXAbstractChannel lxAbstractChannel) {
          if (lxAbstractChannel instanceof LXChannel lxChannel) {
            lxChannel.label.addListener(labelListener);
          }
          refresh();
        }

        @Override
        public void channelRemoved(
            LXMixerEngine lxMixerEngine, LXAbstractChannel lxAbstractChannel) {

          if (lxAbstractChannel instanceof LXChannel lxChannel) {
            lxChannel.label.removeListener(labelListener);

            if (lxChannel == channel) {
              refresh();
            }
          }
        }

        @Override
        public void channelMoved(
            LXMixerEngine lxMixerEngine, LXAbstractChannel lxAbstractChannel) {}
      };

  /** Search the mixer for a matching channel */
  private void refresh() {
    LXChannel channel = search(this.lx.engine.mixer.channels);
    setChannel(channel);
  }

  /** Recursive search a list of channels for a match */
  private LXChannel search(List<? extends LXAbstractChannel> channels) {
    for (LXAbstractChannel abstractChannel : channels) {
      if (abstractChannel instanceof LXGroup group) {
        LXChannel groupChannel = search(group.channels);
        if (groupChannel != null) {
          return groupChannel;
        }
      }
      if (abstractChannel instanceof LXChannel lxChannel && matches(lxChannel)) {
        return lxChannel;
      }
    }
    return null;
  }

  /** Is this the channel we are looking for? */
  private boolean matches(LXChannel channel) {
    return this.channelName.equals(channel.getLabel());
  }

  private void setChannel(LXChannel channel) {
    if (this.channel != channel) {
      this.channel = channel;
      for (Listener listener : this.listeners) {
        listener.channelChanged(this, channel);
      }
    }
  }

  public LXChannel getChannel() {
    return this.channel;
  }

  // Listeners

  public ChannelTracker addListener(Listener listener) {
    Objects.requireNonNull(listener, "May not add null Listener");
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException("Cannot add duplicate Listener: " + listener);
    }
    this.listeners.add(listener);
    return this;
  }

  public ChannelTracker removeListener(Listener listener) {
    if (!this.listeners.contains(listener)) {
      throw new IllegalStateException("Cannot remove non-existent Listener: " + listener);
    }
    this.listeners.remove(listener);
    return this;
  }

  public void dispose() {
    setChannel(null);
    this.listeners.clear();
    this.lx.engine.mixer.removeListener(this.mixerListener);
  }
}
