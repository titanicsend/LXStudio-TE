package titanicsend.preset;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXPresetComponent;
import heronarts.lx.parameter.StringParameter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import titanicsend.util.TE;

public class PresetEngine extends LXComponent {

  private static PresetEngine current;

  public static PresetEngine get() {
    return current;
  }

  private final List<UserPresetLibrary> mutableLibraries = new ArrayList<UserPresetLibrary>();
  public final List<UserPresetLibrary> libraries =
      Collections.unmodifiableList(this.mutableLibraries);

  private UserPresetLibrary currentLibrary;

  public final StringParameter libraryName = new StringParameter("Library", "");

  public PresetEngine(LX lx) {
    super(lx, "PresetEngine");
    current = this;

    this.currentLibrary = new UserPresetLibrary(lx);
    updateLibraryName();
  }

  private void updateLibraryName() {
    this.libraryName.setValue(this.currentLibrary.getFile().getName());
  }

  public UserPresetLibrary getLibrary() {
    return this.currentLibrary;
  }

  public static String getPresetName(LXPresetComponent component) {
    return component.getPresetClass().getCanonicalName();
  }

  public static String getPresetShortName(LXPresetComponent component) {
    if (!(component instanceof LXComponent)) {
      return null;
    }
    return LXComponent.getComponentName(((LXComponent) component).getClass());
  }

  public void newLibrary() {
    this.currentLibrary.reset();
    updateLibraryName();
  }

  public void openLibrary(String path) {
    this.currentLibrary.load(new File(path));
    updateLibraryName();
  }

  public void saveLibrary(String path) {
    this.currentLibrary.save(new File(path));
    updateLibraryName();
  }

  /**
   * Import existing .lxd presets from Chromatik's Presets/ folder, for the current component
   * (pattern) class.
   */
  public void importPresets(LXPresetComponent component) {
    File presetFolder = this.lx.getPresetFolder((LXComponent) component);
    File[] files = presetFolder.listFiles((dir, name) -> name.endsWith(".lxd"));
    if (files != null) {
      for (File file : files) {
        try (FileReader fr = new FileReader(file)) {
          JsonObject obj = new Gson().fromJson(fr, JsonObject.class);
          String name = nameWithoutExtension(file);
          UserPresetCollection collection = this.currentLibrary.get(component);
          collection.addPreset(component, obj).setLabel(name);
        } catch (IOException iox) {
          TE.error("Could not load preset file: %s", iox.getLocalizedMessage());
          this.lx.pushError(
              iox, String.format("Could not load preset file: %s", iox.getLocalizedMessage()));
        } catch (Exception x) {
          TE.error(x, "Exception loading preset file: %s", x.getLocalizedMessage());
          this.lx.pushError(
              x, String.format("Exception in importPresets: %s", x.getLocalizedMessage()));
        }
      }
    }
  }

  /** Import all file system presets for all patterns */
  public void importAllPatternPresets() {
    // TODO
  }

  private String nameWithoutExtension(File file) {
    String name = file.getName();
    int d = name.lastIndexOf('.');
    if (d > 0 && d < name.length() - 1) {
      return name.substring(0, d);
    } else {
      return name;
    }
  }

  public PresetEngine openFile(File file) {
    this.currentLibrary.load(file);
    updateLibraryName();
    return this;
  }
}
