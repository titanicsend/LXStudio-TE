package titanicsend.resolume;

/**
 * Enum defining Resolume OSC variables and their paths.
 * Based on Resolume's standard OSC API for composition control.
 */
public enum ResolumeVariable {
  // Composition controls
  TEMPO_BPM("Tempo BPM", "/composition/tempo/resync", 120, 1, 300),
  MASTER_SPEED("Master Speed", "/composition/speed", 1.0, 0.0, 2.0),
  
  // Column controls
  COLUMN_1_OPACITY("Column 1 Opacity", "/composition/columns/1/opacity", 1.0, 0.0, 1.0),
  COLUMN_2_OPACITY("Column 2 Opacity", "/composition/columns/2/opacity", 1.0, 0.0, 1.0),
  COLUMN_3_OPACITY("Column 3 Opacity", "/composition/columns/3/opacity", 1.0, 0.0, 1.0),
  COLUMN_4_OPACITY("Column 4 Opacity", "/composition/columns/4/opacity", 1.0, 0.0, 1.0),
  
  // Layer controls (Column 1)
  LAYER_1_OPACITY("Layer 1 Opacity", "/composition/layers/1/opacity", 1.0, 0.0, 1.0),
  LAYER_2_OPACITY("Layer 2 Opacity", "/composition/layers/2/opacity", 1.0, 0.0, 1.0),
  LAYER_3_OPACITY("Layer 3 Opacity", "/composition/layers/3/opacity", 1.0, 0.0, 1.0),
  LAYER_4_OPACITY("Layer 4 Opacity", "/composition/layers/4/opacity", 1.0, 0.0, 1.0),
  
  // Color controls
  BRIGHTNESS("Brightness", "/composition/master/brightness", 1.0, 0.0, 2.0),
  CONTRAST("Contrast", "/composition/master/contrast", 1.0, 0.0, 2.0),
  SATURATION("Saturation", "/composition/master/saturation", 1.0, 0.0, 2.0),
  HUE("Hue", "/composition/master/hue", 0.0, -1.0, 1.0),
  
  // Effect controls
  CROSSFADE("Crossfade", "/composition/crossfader", 0.5, 0.0, 1.0),
  MASTER_OPACITY("Master Opacity", "/composition/master/opacity", 1.0, 0.0, 1.0)
  ;

  public final String label;
  public final String oscPath;
  public final double defaultValue;
  public final double min;
  public final double max;

  ResolumeVariable(String label, String oscPath, double defaultValue, double min, double max) {
    this.label = label;
    this.oscPath = oscPath;
    this.defaultValue = defaultValue;
    this.min = min;
    this.max = max;
  }

  @Override
  public String toString() {
    return this.label;
  }
}