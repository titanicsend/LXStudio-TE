package titanicsend.preset;

import heronarts.lx.LXComponent;
import heronarts.lx.parameter.ObjectParameter;
import java.util.Arrays;
import titanicsend.pattern.TEPerformancePattern;

public class TEUserPresetParameter extends ObjectParameter<UserPreset> {
  private TEPerformancePattern pattern;
  private UserPresetCollection collection;

  public TEUserPresetParameter(
      TEPerformancePattern pat, UserPresetCollection collection, String label) {
    super(label, collection.getPresets().toArray(new UserPreset[0]));
    this.pattern = pat;
    this.collection = collection;
    this.updateLabels();

    this.collection.addListener(
        new UserPresetCollection.Listener() {
          @Override
          public void presetAdded(UserPreset preset) {
            updateObjects();
            updateLabels();
          }

          @Override
          public void presetMoved(UserPreset preset) {
            updateObjects();
            updateLabels();
          }

          @Override
          public void presetRemoved(UserPreset preset) {
            updateObjects();
            updateLabels();
          }
        });

    //        this.addListener(new LXParameterListener() {
    //            @Override
    //            public void onParameterChanged(LXParameter parameter) {
    //                UserPreset selected = getObject();
    //                selected.restore(pattern);
    //            }
    //        });
  }

  protected void updateObjects() {
    UserPreset[] presets = this.collection.getPresets().toArray(new UserPreset[0]);
    setObjects(presets);
  }

  protected void updateLabels() {
    String[] options =
        Arrays.stream(getObjects()).map(LXComponent::getLabel).toList().toArray(new String[0]);
    setOptions(options);
  }
}
