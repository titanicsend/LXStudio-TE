package titanicsend.pattern.yoffa.shader_engine;

public class Uniform {

  public final UniformType type;
  public final Object value;

  public Uniform(UniformType type, Object value) {
    this.type = type;
    this.value = value;
  }
}
