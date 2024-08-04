package titanicsend.preset;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXPresetComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PresetEngine extends LXComponent {

  private static PresetEngine current;
  public static PresetEngine get() {
    return current;
  }

  private final List<UserPresetLibrary> mutableLibraries = new ArrayList<UserPresetLibrary>();
  public final List<UserPresetLibrary> libraries = Collections.unmodifiableList(this.mutableLibraries);

  public UserPresetLibrary currentLibrary;

  public PresetEngine(LX lx) {
    super(lx, "PresetEngine");
    current = this;

    currentLibrary = new UserPresetLibrary(lx);
  }

  public static String getPresetName(LXPresetComponent component) {
    return component.getPresetClass().getCanonicalName();
  }
}
