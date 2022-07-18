package titanicsend.pattern.yoffa.shader_engine;

/**
 * List of supported shader uniform types.  This is the list that Processing 4 uses,
 * which is pretty much the list of things that you'd want to pass to a shader.
 */
class GeneralUniforms {
        static final int INT1      = 0;
        static final int INT2      = 1;
        static final int INT3      = 2;
        static final int INT4      = 3;
        static final int FLOAT1    = 4;
        static final int FLOAT2    = 5;
        static final int FLOAT3    = 6;
        static final int FLOAT4    = 7;

        // the "not yet supported" line...
        static final int INT1VEC   = 8;
        static final int INT2VEC   = 9;
        static final int INT3VEC   = 10;
        static final int INT4VEC   = 11;
        static final int FLOAT1VEC = 12;
        static final int FLOAT2VEC = 13;
        static final int FLOAT3VEC = 14;
        static final int FLOAT4VEC = 15;
        static final int MAT2      = 16;
        static final int MAT3      = 17;
        static final int MAT4      = 18;
        static final int SAMPLER2D = 19;

        int type;
        Object value;

        GeneralUniforms(int type, Object value) {
            this.type = type;
            this.value = value;
        }
    }
