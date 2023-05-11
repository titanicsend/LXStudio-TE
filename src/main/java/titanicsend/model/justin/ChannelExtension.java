/**
 * Licensing Notes (JKB)
 *
 * The expected permanent home for this concept and its derivatives is the LX Studio
 * software library or a LX Studio / Chromatik extension distributed by JKB.
 *
 * Due to time constraints, doing a first release of this code in either
 * of the above code bases would add too much delay to be usable
 * for the immediate TE events.
 *
 * It is acknowledged that by releasing the code here, the TE code base may
 * continue to use this original version in perpetuity.
 * It is also the stated intent that the long-term license for this code
 * and its derivatives will be the LX Studio Software License and
 * Distribution Agreement (http://lx.studio/license), or another license
 * as determined by the author. 
 *
 * @author Justin Belcher <jkbelcher@gmail.com>
 */

package titanicsend.model.justin;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXMixerEngine;

/**
 * A ChannelExtension is meant to be a singleton, adding a
 * per-channel service to an LX instance.  This is similar to
 * an effect that is always available on every channel.
 *
 * (The ability to register a custom channel class with the
 * engine would be a nicer way to solve this type of problem...)
 */
public abstract class ChannelExtension<T extends LXComponent> extends LXComponent {
  
  private final Map<LXAbstractChannel, T> items = new HashMap<LXAbstractChannel, T>();

  public class MixerListener implements heronarts.lx.mixer.LXMixerEngine.Listener {
    @Override
    public void channelAdded(LXMixerEngine mixer, LXAbstractChannel channel) {
      addChannel(channel);   
    }

    @Override
    public void channelRemoved(LXMixerEngine mixer, LXAbstractChannel channel) {
      removeChannel(channel);
    }

    @Override
    public void channelMoved(LXMixerEngine mixer, LXAbstractChannel channel) { }    
  }
  
  private final MixerListener mixerListener = new MixerListener();
  
  public ChannelExtension(LX lx) {
    super(lx);
    
    initialize();
    
    addChannels(lx.engine.mixer.getChannels());
    
    lx.engine.mixer.addListener(this.mixerListener);
  }

  /*
   * Methods for child classes
   */
  
  /**
   * Early setup, gets called on child class BEFORE channels are added
   * and T items are created.
   */
  protected abstract void initialize();

  /**
   * Subclasses can override
   * @return TRUE if items should be created for groups
   */
  protected boolean applyToGroups() {
    return true;
  }

  /**
   * Subclasses can override
   * @return TRUE if item should be created for the passed channel
   */
  protected boolean applyToChannel(LXAbstractChannel channel) {
    return true;
  }

  protected abstract T createItem(LXAbstractChannel channel);
  
  /*
   * Boring channel management stuff
   */
  
  protected void addChannels(List<LXAbstractChannel> channels) {
    for (LXAbstractChannel channel : channels) {
      addChannel(channel);
    }
  }
  
  protected T addChannel(LXAbstractChannel channel) {
    if (!applyToGroups() && channel.isGroup()) {
      return null;
    }    
    if (!applyToChannel(channel)) {
      return null;
    }
    
    T item;
    if (!this.items.containsKey(channel)) {
      item = createItem(channel);
      this.items.put(channel, item);
    } else {
      item = this.items.get(channel);
    }
    return item;
  }
  
  protected void removeChannel(LXAbstractChannel channel) {
    T item = this.items.get(channel);
    if (item != null) {
      //item.dispose();      
    }
    this.items.remove(channel);
  }
  
  public T get(LXAbstractChannel channel) {
    return items.get(channel);
  }

  public Collection<T> getAll() {
    return items.values();
  }

  @Override
  public void dispose() {
    this.lx.engine.mixer.removeListener(this.mixerListener);
    for (Entry<LXAbstractChannel, T> entry : this.items.entrySet()) {
      entry.getValue().dispose();
    }
    this.items.clear();
    super.dispose();
  }
}
