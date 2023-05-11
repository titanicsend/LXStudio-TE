package titanicsend.pattern.yoffa.framework;

/**
 * Map the original PatternTarget methods to view labels,
 * so shader patterns can specify a preferred/default view.
 */
public enum TEShaderView {
  ALL_POINTS(null),
  ALL_EDGES("Edges"),
  ALL_PANELS("Panels"),
  ALL_PANELS_INDIVIDUAL("Panels;"),
  DOUBLE_LARGE("Double Large"),
  SPLIT_PANEL_SECTIONS("Panel Sections");  // The original used individual panels in Aft and Starboard Aft sections. Could add a view for this variation if desired.

  /**
   * This view's label in resources/vehicle/views.txt
   */
  public final String viewLabel;

  private TEShaderView(String viewLabel) {
    this.viewLabel = viewLabel;
  }
}
