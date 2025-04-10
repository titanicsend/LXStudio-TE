package titanicsend.color;

// See TE Art Direction Standards:
// https://docs.google.com/document/d/16FGnQ8jopCGwQ0qZizqILt3KYiLo0LPYkDYtnYzY7gI/edit
public enum TEColorType {
  // These are 1-based UI indices; to get to a 0-based palette swatch index, subtract 1
  PRIMARY(1), // Primary color of any edge or panel pattern
  SECONDARY(2), // Secondary color; optional, commonly set to SECONDARY_BACKGROUND or PRIMARY
  TERTIARY(3); // 3rd color, now that ColorPaletteManager sometimes provides three

  public final int index; // The UI index (1-indexed)

  private TEColorType(int index) {
    this.index = index;
  }

  // UI swatches are 1-indexed; internally, swatch arrays are 0-indexed
  public int swatchIndex() {
    return index - 1;
  }
}
