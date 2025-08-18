package titanicsend.preset;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXDeviceComponent;
import heronarts.lx.LXModulatorComponent;
import heronarts.lx.LXPresetComponent;
import heronarts.lx.mixer.LXBus;
import heronarts.lx.modulation.LXModulationContainer;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
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

  public void loadLibrary(String path) {
    this.currentLibrary.load(new File(path), true);
    updateLibraryName();
  }

  public void mergeLibrary(String path) {
    this.currentLibrary.load(new File(path), false);
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

  /**
   * If a preset is applied from a parameter listener, and the parameter is also a device remote
   * control (such as a preset Selector), the action needs to be delayed. Otherwise, a
   * ConcurrentModificationException will occur when listeners of the remote controls list (such as
   * UI device and UI performance device) perform downstream actions (such as disposing and
   * rebuilding UI parameter controls).
   *
   * @param preset Preset to apply, or null for defaults
   * @param presetComponent Target component
   */
  public void applyPresetDelayed(UserPreset preset, LXPresetComponent presetComponent) {
    this.lx.engine.addTask(
        () -> {
          applyPreset(preset, presetComponent);
        });
  }

  /**
   * Apply a preset to a component of the matching class.
   *
   * @param preset Preset to apply, or null for defaults
   * @param presetComponent Target component
   */
  public void applyPreset(UserPreset preset, LXPresetComponent presetComponent) {
    if (preset != null) {
      if (preset.matches(presetComponent)) {
        // Normal operation: restore preset to component
        preset.restore(presetComponent);
      } else {
        throw new IllegalArgumentException("Preset does not match component: " + presetComponent);
      }
    } else {
      // "Default": No preset was supplied, so fully reset the component.

      // Remove all modulations currently acting on the component's parameters
      if (presetComponent instanceof LXModulatorComponent modulatorComponent) {
        for (LXParameter parameter : modulatorComponent.getParameters()) {
          if (parameter instanceof CompoundParameter compoundParameter) {
            disposeModulations(modulatorComponent, compoundParameter);
          }
        }
      }

      // Remove all modulators that live directly on the component
      if (presetComponent instanceof LXDeviceComponent deviceComponent) {
        deviceComponent.modulation.clear();
      } else if (presetComponent instanceof LXBus bus) {
        bus.modulation.clear();
      }

      // Reset parameters
      if (presetComponent instanceof LXComponent component) {
        for (LXParameter p : component.getParameters()) {
          // Avoid clearing the label
          if (p != component.label) {
            p.reset();
          }
        }
      }
    }
  }

  private void disposeModulations(LXComponent parent, CompoundParameter parameter) {
    // Remove modulations from any containers up the chain
    while ((parent != null) && (parent != this.lx.engine)) {
      if (parent instanceof LXModulationContainer modulationContainer) {
        modulationContainer.getModulationEngine().removeParameterModulations(parameter);
      }
      parent = parent.getParent();
    }

    // Remove global modulations
    this.lx.engine.getModulationEngine().removeParameterModulations(parameter);
  }

  /**
   * Save only the active preset from the specified component to disk,
   * preserving the saved library state. This reloads the library from disk,
   * adds only the specified preset, and saves it back - ensuring other 
   * in-memory presets that may be in development are not persisted.
   */
  public void saveActivePresetOnly(LXPresetComponent component, UserPreset activePreset) {
    if (activePreset == null) {
      return;
    }

    // Create a fresh library and load the current saved state from disk
    UserPresetLibrary saveLibrary = new UserPresetLibrary(this.lx);
    File currentFile = this.currentLibrary.getFile();
    if (currentFile.exists()) {
      saveLibrary.load(currentFile);
    }

    // Get the collection for this component type in the save library
    UserPresetCollection saveCollection = saveLibrary.get(component);

    // Remove any existing preset with the same label within this specific component class
    // Note: saveCollection already contains only presets for this component class,
    // so we only need to check label for deduplication within this collection
    UserPreset existingPreset = null;
    for (UserPreset preset : saveCollection.getPresets()) {
      if (preset.getLabel().equals(activePreset.getLabel())) {
        existingPreset = preset;
        break;
      }
    }
    if (existingPreset != null) {
      saveCollection.removePreset(existingPreset);
    }

    // Add the current active preset to the save library
    UserPreset newPreset = saveCollection.addPreset(component);
    newPreset.setLabel(activePreset.getLabel());

    // Save to the current library's file location
    saveLibrary.save(currentFile);
  }

  /**
   * Remove a specific preset from the saved library on disk,
   * preserving all other presets in their saved state.
   * Uses component class + preset label as unique identifier.
   */
  public void removePresetFromDisk(LXPresetComponent component, UserPreset presetToRemove) {
    if (presetToRemove == null) {
      return;
    }
    String presetLabel = presetToRemove.getLabel();
    String componentClass = PresetEngine.getPresetName(component);

    // Create a fresh library and load the current saved state from disk
    UserPresetLibrary saveLibrary = new UserPresetLibrary(this.lx);
    File currentFile = this.currentLibrary.getFile();
    if (currentFile.exists()) {
      saveLibrary.load(currentFile);
    }

    // Get the collection for this component type in the save library
    UserPresetCollection saveCollection = saveLibrary.get(component);

    // Find and remove the preset with the specified class + label combination
    // Note: saveCollection already contains only presets for this component class,
    // so we verify both class and label for absolute safety
    UserPreset diskPresetToRemove = null;
    for (UserPreset preset : saveCollection.getPresets()) {
      if (preset.clazz.equals(componentClass) && preset.getLabel().equals(presetLabel)) {
        diskPresetToRemove = preset;
        break;
      }
    }
    if (diskPresetToRemove != null) {
      saveCollection.removePreset(diskPresetToRemove);
    }

    // Save to the current library's file location
    saveLibrary.save(currentFile);
  }

  /**
   * Rename a specific preset in the saved library on disk,
   * preserving all other presets in their saved state.
   * Uses component class + preset label as unique identifier.
   */
  public void renamePresetOnDisk(LXPresetComponent component, String oldLabel, String newLabel) {
    if (oldLabel == null || newLabel == null || oldLabel.equals(newLabel)) {
      return;
    }
    String componentClass = PresetEngine.getPresetName(component);

    // Create a fresh library and load the current saved state from disk
    UserPresetLibrary saveLibrary = new UserPresetLibrary(this.lx);
    File currentFile = this.currentLibrary.getFile();
    if (currentFile.exists()) {
      saveLibrary.load(currentFile);
    }

    // Get the collection for this component type in the save library
    UserPresetCollection saveCollection = saveLibrary.get(component);

    // Find the preset with the specified class + old label combination and rename it
    // Note: saveCollection already contains only presets for this component class,
    // so we verify both class and label for absolute safety
    for (UserPreset preset : saveCollection.getPresets()) {
      if (preset.clazz.equals(componentClass) && preset.getLabel().equals(oldLabel)) {
        preset.setLabel(newLabel);
        break;
      }
    }

    // Save to the current library's file location
    saveLibrary.save(currentFile);
  }
}
