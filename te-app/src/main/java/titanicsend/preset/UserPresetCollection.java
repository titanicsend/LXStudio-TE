package titanicsend.preset;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import heronarts.lx.LX;
import heronarts.lx.LXPresetComponent;
import heronarts.lx.LXSerializable;
import heronarts.lx.parameter.ObjectParameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A collection of presets for one component */
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
        throw new IllegalArgumentException(
            "Component '" + component + "' does not match preset '" + preset + "'");
      }
      preset.capture(component);
    }
    preset.setIndex(this.mutablePresets.size());
    this.mutablePresets.add(preset);
    for (Listener listener : this.listeners) {
      listener.presetAdded(preset);
    }
    updateSelectors();
    return preset;
  }

  public UserPreset addPreset(LXPresetComponent component, JsonObject object) {
    Objects.requireNonNull(component);
    UserPreset preset = new UserPreset(this.lx, this.clazz, object);
    if (!preset.matches(component)) {
      throw new IllegalArgumentException(
          "Component '" + component + "' does not match preset '" + preset + "'");
    }
    preset.setIndex(this.mutablePresets.size());
    this.mutablePresets.add(preset);
    for (Listener listener : this.listeners) {
      listener.presetAdded(preset);
    }
    updateSelectors();
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
    updateSelectors();
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
    updateSelectors();
    return this;
  }

  public final void addListener(Listener listener) {
    Objects.requireNonNull(listener, "May not add null LXChannel.Listener");
    if (this.listeners.contains(listener)) {
      throw new IllegalStateException(
          "May not add duplicate UserPresetCollection.Listener: " + listener);
    }
    this.listeners.add(listener);
  }

  public final void removeListener(Listener listener) {
    if (!this.listeners.contains(listener)) {
      throw new IllegalStateException(
          "May not remove non-registered UserPresetCollection.Listener: " + listener);
    }
    this.listeners.remove(listener);
  }

  public List<UserPreset> getPresets() {
    return this.presets;
  }

  // ----------------------------------------------------------------------------------
  // Selectors
  // ----------------------------------------------------------------------------------

  private final String DEFAULT_PRESET = "Default";
  private UserPreset[] presetObjects = {null};
  private String[] presetLabels = {DEFAULT_PRESET};

  private final List<Selector> selectors = new ArrayList<>();

  public class Selector extends ObjectParameter<UserPreset> {
    public Selector(String label) {
      super(label, presetObjects, presetLabels);
      setWrappable(false);
      UserPresetCollection.this.selectors.add(this);
    }

    @Override
    public void dispose() {
      UserPresetCollection.this.selectors.remove(this);
      super.dispose();
    }
  }

  public Selector newUserPresetSelector(String label) {
    return new Selector(label);
  }

  private void updateSelectors() {
    int numOptions = 1 + this.presets.size();
    this.presetObjects = new UserPreset[numOptions];
    this.presetLabels = new String[numOptions];
    this.presetObjects[0] = null;
    this.presetLabels[0] = DEFAULT_PRESET;

    int i = 1;
    for (UserPreset preset : this.presets) {
      this.presetObjects[i] = preset;
      this.presetLabels[i] = preset.getLabel();
      ++i;
    }

    // Update all of the params to have new range/options
    for (Selector parameter : this.selectors) {
      final UserPreset selected = parameter.getObject();
      parameter.setObjects(this.presetObjects, this.presetLabels);

      // Check if a param had a non-null selection, if so it should be restored in the case of
      // renaming/reordering where it is still in the list but its index may be different.
      if ((selected != parameter.getObject()) && this.presets.contains(selected)) {
        parameter.setValue(selected);
      }
    }
  }

  // ----------------------------------------------------------------------------------
  // Save / Load
  // ----------------------------------------------------------------------------------

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
    updateSelectors();
  }
}
