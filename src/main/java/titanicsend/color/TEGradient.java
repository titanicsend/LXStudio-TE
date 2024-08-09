package titanicsend.color;

public enum TEGradient {
  NORMAL("Normal"),
  DARK("Dark");

  public final String label;

  private TEGradient(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return this.label;
  }
}
