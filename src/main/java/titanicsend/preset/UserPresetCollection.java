package titanicsend.preset;

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

  private void save() {
    /*        // Don't re-save the file on updates caused by loading it...
        if (this.inLoad) {
            return;
        }

        JsonObject obj = new JsonObject();
        save(this.lx, obj);
        try (JsonWriter writer = new JsonWriter(new FileWriter(this.file))) {
            writer.setIndent("  ");
            new GsonBuilder().create().toJson(obj, writer);
        } catch (IOException iox) {
            LX.error(iox, "Exception writing the preferences file: " + this.file);
        }*/
  }

  private static final String KEY_PRESETS = "presets";

  @Override
  public void save(LX lx, JsonObject object) {
    /*        if (this.fileName != null) {
            object.addProperty("TestPropertyName", this.fileName);
        }
        object.add(KEY_PRESETS, LXSerializable.Utils.toArray(lx, this.presets));*/
  }

  @Override
  public void load(LX lx, JsonObject object) {

  }
}
