package titanicsend.preset;

import com.google.gson.JsonObject;
import heronarts.lx.LX;
import heronarts.lx.LXPresetComponent;
import heronarts.lx.LXSerializable;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.LXColor;
import java.io.File;
import java.util.*;

/**
 * Contains presets for multiple components
 */
public class UserPresetLibrary implements LXSerializable {
  private final LX lx;

  private String fileName = "newFile.userPresets";
  private final File file;

  public final ColorParameter color = new ColorParameter("Color", LXColor.RED);

  private final List<UserPresetCollection> mutableCollections = new ArrayList<UserPresetCollection>();
  public final List<UserPresetCollection> collections = Collections.unmodifiableList(this.mutableCollections);

  private final Map<String, UserPresetCollection> collectionDict = new HashMap<String, UserPresetCollection>();

  public UserPresetLibrary(LX lx) {
    this.lx = lx;
    this.file = lx.getMediaFile(fileName);
  }

  /**
   * Retrieve a collection of presets for a given component (pattern, etc)
   */
  public UserPresetCollection get(LXPresetComponent component) {
    return get(PresetEngine.getPresetName(component));
  }

  public UserPresetCollection get(String clazz) {
    UserPresetCollection c = collectionDict.get(clazz);
    if (c == null) {
      c = new UserPresetCollection(this.lx, clazz);
      collectionDict.put(clazz, c);
    }
    return c;
  }

  // TODO!

  @Override
  public void save(LX lx, JsonObject object) {

  }

  @Override
  public void load(LX lx, JsonObject object) {

  }
}
