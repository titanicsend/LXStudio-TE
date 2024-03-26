package titanicsend.pattern.yoffa.shader_engine;

/**
 * List of supported shader uniform types. This is the list that Processing 4 uses, which is pretty
 * much the list of things that you'd want to pass to a shader.
 */
public class UniformTypes {
  public static final int INT1 = 0;
  public static final int INT2 = 1;
  public static final int INT3 = 2;
  public static final int INT4 = 3;
  public static final int FLOAT1 = 4;
  public static final int FLOAT2 = 5;
  public static final int FLOAT3 = 6;
  public static final int FLOAT4 = 7;
  public static final int INT1VEC = 8;
  public static final int INT2VEC = 9;
  public static final int INT3VEC = 10;
  public static final int INT4VEC = 11;
  public static final int FLOAT1VEC = 12;
  public static final int FLOAT2VEC = 13;
  public static final int FLOAT3VEC = 14;
  public static final int FLOAT4VEC = 15;
  public static final int MAT2 = 16;
  public static final int MAT3 = 17;
  public static final int MAT4 = 18;
  public static final int SAMPLER2D = 19;
  public static final int SAMPLER2DSTATIC = 20;

  public int type;
  public Object value;

  public UniformTypes(int type, Object value) {
    this.type = type;
    this.value = value;
  }
}
