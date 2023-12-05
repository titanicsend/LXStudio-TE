package titanicsend.color;

public enum TEGradient {
  FULL_PALETTE("Full Palette"),
  PRIMARY("Primary"),
  SECONDARY("Secondary"),
  FOREGROUND("Foreground");

  public final String label;

  private TEGradient(String label) {
    this.label = label;
  }

  @Override
  public String toString() {
    return this.label;
  }
}
