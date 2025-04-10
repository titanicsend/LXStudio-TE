package titanicsend.model;

import heronarts.lx.model.LXView;

/**
 * Intermediate class for loading view definitions from a text file, before creating the global
 * views.
 */
public class ViewDefinition {

  public String label;
  public boolean viewEnabled;
  public String viewSelector;
  public LXView.Normalization viewNormalization;

  public ViewDefinition(String label, String viewSelector) {
    this(label, viewSelector, LXView.Normalization.RELATIVE);
  }

  public ViewDefinition(String label, String viewSelector, LXView.Normalization viewNormalization) {
    this(label, true, viewSelector, viewNormalization);
  }

  public ViewDefinition(
      String label,
      boolean viewEnabled,
      String viewSelector,
      LXView.Normalization viewNormalization) {
    this.label = label;
    this.viewEnabled = viewEnabled;
    this.viewSelector = viewSelector;
    this.viewNormalization = viewNormalization;
  }
}
