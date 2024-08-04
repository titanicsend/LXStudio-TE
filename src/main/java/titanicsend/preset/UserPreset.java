package titanicsend.preset;

import com.google.gson.JsonObject;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXPresetComponent;
import heronarts.lx.LXSerializable;

/**
 * A saveable snapshot of a component including parameter values and modulators.
 * Adds a renamable label and list ordering.
 */
public class UserPreset extends LXComponent implements LXComponent.Renamable {

  public final String clazz;
  private JsonObject preset;

  private int index = -1;

  public UserPreset(LX lx, String clazz) {
    super(lx);
    this.label.setDescription("The name of this preset");

    this.clazz = clazz;
  }

  /**
   * Checks whether an object instance matches this preset class
   */
  public boolean matches(LXPresetComponent component) {
    return this.clazz.equals(PresetEngine.getPresetName(component));
  }

  /**
   * Update this preset with the current state of a component
   * @param component Component matching the class of this UserPreset
   */
  public UserPreset capture(LXPresetComponent component) {
    if (!(component instanceof LXComponent)) {
      throw new IllegalArgumentException("Component must be LXComponent to capture preset");
    }
    if (!matches(component)) {
      throw new IllegalArgumentException("Can not capture component that does not match UserPreset class");
    }
    this.preset = LXSerializable.Utils.toObject((LXComponent)component);
    component.postProcessPreset(this.lx, this.preset);
    return this;
  }

  /**
   * Restore a preset to a component.  Component type must match.
   */
  public UserPreset restore(LXPresetComponent component) {
    if (!(component instanceof LXComponent)) {
      throw new IllegalArgumentException("Component must be LXComponent to restore preset");
    }
    if (!matches(component)) {
      throw new IllegalArgumentException("Can not restore UserPreset to unmatching component" + component);
    }
    ((LXComponent)component).load(this.lx, this.preset);
    return this;
  }


  public void setIndex(int index) {
    this.index = index;
  }

  public int getIndex() {
    return this.index;
  }

  // TODO

  private static final String KEY_PATTERN_CLASS = "pattern";

  @Override
  public void save(LX lx, JsonObject obj) {
    super.save(lx, obj);
    //obj.addProperty(KEY_PATTERN_CLASS, this.pattern.getCanonicalName());
  }
}
