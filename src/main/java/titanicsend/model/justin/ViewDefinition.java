package titanicsend.model.justin;

import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXView;

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

  public ViewDefinition(String label, boolean viewEnabled) {
    this(label, viewEnabled, null, LXView.Normalization.ABSOLUTE);    
  }

  public ViewDefinition(String label, boolean viewEnabled, String viewSelector, LXView.Normalization viewNormalization) {
    this.label = label;
    this.viewEnabled = viewEnabled;
    this.viewSelector = viewSelector;
    this.viewNormalization = viewNormalization;
  }

  @Override
  public String toString() {
    return this.label;
  }
  
  private LXModel model = null;
  
  public LXModel getModel() {
    return this.model;
  }
  
  public void setModel(LXModel model) {
    this.model = model;
  }

}
