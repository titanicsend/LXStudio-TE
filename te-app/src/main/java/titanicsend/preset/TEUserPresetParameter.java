package titanicsend.preset;

import heronarts.lx.LXComponent;
import heronarts.lx.parameter.ObjectParameter;
import java.util.Arrays;
import titanicsend.pattern.TEPerformancePattern;

public class TEUserPresetParameter extends ObjectParameter<UserPreset> {
  private TEPerformancePattern pattern;

  public TEUserPresetParameter(TEPerformancePattern pat, String label) {
    super(label, PresetEngine.get().getLibrary().get(pat).getPresets().toArray(new UserPreset[0]));
    this.pattern = pat;
    this.updateObjects();
  }

  public void updateObjects() {
    //    // TODO: do I need to fetch this every time, or is it OK to save a handle?
    //    UserPresetCollection collection = PresetEngine.get().getLibrary().get(this.pattern);
    //    UserPreset[] presets = collection.getPresets().toArray(new UserPreset[0]);
    UserPreset[] presets =
        PresetEngine.get().getLibrary().get(this.pattern).getPresets().toArray(new UserPreset[0]);
    setObjects(presets);

    String[] options =
        Arrays.stream(getObjects()).map(LXComponent::getLabel).toList().toArray(new String[0]);
    setOptions(options);
  }
}
