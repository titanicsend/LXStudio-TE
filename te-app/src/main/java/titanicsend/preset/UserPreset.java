package titanicsend.preset;

import com.google.gson.JsonObject;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXPresetComponent;
import heronarts.lx.LXSerializable;
import titanicsend.pattern.TEPattern;

/**
 * A saveable snapshot of a component including parameter values and modulators. Adds a renamable
 * label and list ordering.
 */
public class UserPreset extends LXComponent implements LXComponent.Renamable, LXSerializable {

  public final String clazz;
  private JsonObject preset;

  private int index = -1;

  public UserPreset(LX lx, String clazz) {
    this(lx, clazz, null);
  }

  public UserPreset(LX lx, String clazz, JsonObject preset) {
    super(lx);
    this.label.setDescription("The name of this preset");

    this.clazz = clazz;
    this.preset = preset;
  }

  public UserPreset setLabel(String name) {
    this.label.setValue(name);
    return this;
  }

  /** Checks whether an object instance matches this preset class */
  public boolean matches(LXPresetComponent component) {
    return this.clazz.equals(PresetEngine.getPresetName(component));
  }

  /**
   * Update this preset with the current state of a component
   *
   * @param component Component matching the class of this UserPreset
   */
  public UserPreset capture(LXPresetComponent component) {
    if (!(component instanceof LXComponent)) {
      throw new IllegalArgumentException("Component must be LXComponent to capture preset");
    }
    if (!matches(component)) {
      throw new IllegalArgumentException(
          "Can not capture component that does not match UserPreset class");
    }

    if (component instanceof TEPattern) {
      // Set the current parameter values as default before creating preset.
      // Panic button will return to these.
      ((TEPattern) component).captureDefaults();
    }

    this.preset = new JsonObject();
    ((LXComponent) component).save(this.lx, this.preset);
    component.postProcessPreset(this.lx, this.preset);

    return this;
  }

  /** Restore a preset to a component. Component type must match. */
  public UserPreset restore(LXPresetComponent component) {
    if (!(component instanceof LXComponent)) {
      throw new IllegalArgumentException("Component must be LXComponent to restore preset");
    }
    if (!matches(component)) {
      throw new IllegalArgumentException(
          "Can not restore UserPreset to unmatching component type" + component);
    }

    // Custom tweak to LX framework, allow loading of preset from JsonObject
    ((LXComponent) component).loadPreset(this.preset);

    return this;
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public int getIndex() {
    return this.index;
  }

  public static final String KEY_LABEL = "label";
  public static final String KEY_PRESET_OBJ = "presetObj";

  @Override
  public void save(LX lx, JsonObject obj) {
    obj.addProperty(KEY_LABEL, this.getLabel());
    obj.add(KEY_PRESET_OBJ, this.preset.deepCopy());
  }

  @Override
  public void load(LX lx, JsonObject obj) {
    if (obj.has(KEY_LABEL)) {
      this.setLabel(obj.get(KEY_LABEL).getAsString());
    }
    if (obj.has(KEY_PRESET_OBJ)) {
      JsonObject presetObj = obj.get(KEY_PRESET_OBJ).getAsJsonObject().deepCopy();
      this.preset = presetObj;
    }
  }
}
