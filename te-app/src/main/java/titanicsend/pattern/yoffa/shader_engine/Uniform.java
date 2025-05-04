package titanicsend.pattern.yoffa.shader_engine;

public class Uniform {

  private static final int LOCATION_NOT_FOUND = -1;

  public final int location;
  public final boolean hasLocation;
  public final UniformType type;
  public Object value = null;
  public boolean modified = false;

  public Uniform(int location, UniformType type) {
    this.location = location;
    this.hasLocation = location != LOCATION_NOT_FOUND;
    this.type = type;
  }

  public void set(Object value) {
    this.value = value;
    this.modified = true;
  }

  public boolean hasUpdate() {
    return modified && hasLocation;
  }
}
