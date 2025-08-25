package titanicsend.pattern.yoffa.shader_engine;

/**
 * List of supported shader uniform types. This is the list that Processing 4 uses, which is pretty
 * much the list of things that you'd want to pass to a shader.
 */
public enum UniformType {
  INT1,
  INT2,
  INT3,
  INT4,
  BOOLEAN1,
  BOOLEAN2,
  FLOAT1,
  FLOAT2,
  FLOAT3,
  FLOAT4,
  INT1VEC,
  INT2VEC,
  INT3VEC,
  INT4VEC,
  FLOAT1VEC,
  FLOAT2VEC,
  FLOAT3VEC,
  FLOAT4VEC,
  MAT2,
  MAT3,
  MAT4,
  SAMPLER2D,
  SAMPLER2DSTATIC
}
