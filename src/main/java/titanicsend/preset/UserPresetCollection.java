package titanicsend.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import heronarts.lx.LX;
import heronarts.lx.LXPresetComponent;
import heronarts.lx.LXSerializable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A collection of presets for one component
 */
public class UserPresetCollection implements LXSerializable {

  private final LX lx;
  private final String clazz;

  private final List<UserPreset> mutablePresets = new ArrayList<UserPreset>();
  public final List<UserPreset> presets = Collections.unmodifiableList(this.mutablePresets);

  public interface Listener {
    public default void presetAdded(UserPreset preset) {}
    public default void presetMoved(UserPreset preset) {}
    public default void presetRemoved(UserPreset preset) {}
  }

  private final List<Listener> listeners = new ArrayList<Listener>();

  public UserPresetCollection(LX lx, String clazz) {
    this.lx = lx;
    this.clazz = clazz;
  }

  public UserPreset addPreset() {
    return addPreset(null);
  }

  public UserPreset addPreset(LXPresetComponent component) {
    UserPreset preset = new UserPreset(this.lx, this.clazz);
    if (component != null) {
      if (!preset.matches(component)) {
        throw new IllegalArgumentException("Component '" + component + "' does not match preset '" + preset + "'");
      }
      preset.capture(component);
    }
    preset.setIndex(this.mutablePresets.size());
    this.mutablePresets.add(preset);
    for (Listener listener : this.listeners) {
      listener.presetAdded(preset);
    }
    return preset;
  }

  public UserPreset addPreset(LXPresetComponent component, JsonObject object) {
    Objects.requireNonNull(component);
    UserPreset preset = new UserPreset(this.lx, this.clazz, object);
    if (!preset.matches(component)) {
      throw new IllegalArgumentException("Component '" + component + "' does not match preset '" + preset + "'");
    }
    preset.setIndex(this.mutablePresets.size());
    this.mutablePresets.add(preset);
    for (Listener listener : this.listeners) {
      listener.presetAdded(preset);
    }
    return preset;
  }

  public UserPresetCollection removePreset(UserPreset preset) {
    int index = this.mutablePresets.indexOf(preset);
    if (index < 0) {
      return this;
    }
    this.mutablePresets.remove(index);
    for (int i = index; i < this.mutablePresets.size(); ++i) {
      this.mutablePresets.get(i).setIndex(i);
    }
    for (Listener listener : this.listeners) {
      listener.presetRemoved(preset);
    }
    return this;
  }

  public UserPresetCollection movePreset(UserPreset pattern, int index) {
    this.mutablePresets.remove(pattern);
    this.mutablePresets.add(index, pattern);
    int i = 0;
    for (UserPreset p : this.mutablePresets) {
      p.setIndex(i++);
    }
    for (Listener listener : this.listeners) {
      listener.presetMoved(pattern);
    }
    return this;
  }

  public final void addListener(Listener listener) {
    Objects.requireNonNull(listener, "May not add null LXChannel.Listener");
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException("May not add duplicate UserPresetCollection.Listener: " + listener);
    }
    this.listeners.add(listener);
  }

  public final void removeListener(Listener listener) {
    if (!this.listeners.contains(listener)) {
      throw new IllegalStateException("May not remove non-registered UserPresetCollection.Listener: " + listener);
    }
    this.listeners.remove(listener);
  }

  public List<UserPreset> getPresets() {
    return this.presets;
  }


  // TODO: File save/load

  private boolean inLoad = false;


  public static final String KEY_CLASS = "clazz";
  private static final String KEY_PRESETS = "presets";

  @Override
  public void save(LX lx, JsonObject obj) {
    obj.addProperty(KEY_CLASS, this.clazz);
    obj.add(KEY_PRESETS, LXSerializable.Utils.toArray(lx, this.presets));
  }

  @Override
  public void load(LX lx, JsonObject obj) {
    if (obj.has(KEY_CLASS)) {
      // Should already match
    }

    if (obj.has(KEY_PRESETS)) {
      JsonArray presetsArray = obj.getAsJsonArray(KEY_PRESETS);
      for (JsonElement presetElement : presetsArray) {
        JsonObject presetObj = (JsonObject) presetElement;
        loadPreset(presetObj);
      }
    }
  }

  private void loadPreset(JsonObject presetObj) {
    UserPreset preset = new UserPreset(this.lx, this.clazz);
    preset.load(this.lx, presetObj);

    preset.setIndex(this.mutablePresets.size());
    this.mutablePresets.add(preset);
    for (Listener listener : this.listeners) {
      listener.presetAdded(preset);
    }
  }
}
