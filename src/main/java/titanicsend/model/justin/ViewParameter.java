package titanicsend.model.justin;

import java.util.Arrays;

import heronarts.lx.parameter.ObjectParameter;
import titanicsend.util.TE;

public class ViewParameter extends ObjectParameter<ViewDefinition> {

  public ViewParameter(String label, ViewDefinition[] objects) {
    super(label, objects);
    setIncrementMode(IncrementMode.RELATIVE);
    setWrappable(false);
  }

  /**
   * Set to view with the matching label.
   *
   * If parameter is null, sets to default / no view
   * If label is not found, does nothing
   */
  public ViewParameter setView(String label) {
    return setView(label, false);
  }

  public ViewParameter setView(String label, boolean defaultOnNotFound) {
    ViewDefinition view = null;
    if (!TE.isEmpty(label)) {
      view = Arrays.stream(getObjects())
              .filter(v -> label.equals(v.label))
              .findFirst()
              .orElse(null);
    }
    if (view != null) {
      setValue(view);
    } else {
      setValue(0);
    }
    return this;
  }
}
