package titanicsend.pattern.yoffa.shader_engine;

import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.texture.Texture;
import heronarts.lx.LX;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public abstract class Uniform {

  private static final int LOCATION_NOT_FOUND = -1;

  protected final GL4 gl4;
  public final String name;
  public final int location;
  public final boolean hasLocation;
  public final UniformType type;
  protected boolean modified;

  public Uniform(GL4 gl4, String name, int location, UniformType type) {
    this.gl4 = gl4;
    this.name = name;
    this.location = location;
    this.hasLocation = location != LOCATION_NOT_FOUND;
    this.type = type;
    // Ensure uniform will get sent on the first frame
    this.modified = true;
  }

  public boolean hasUpdate() {
    return modified && hasLocation;
  }

  /** Send latest value to OpenGL */
  public abstract void update();

  /** Factory to create a new uniform by type */
  public static Uniform create(
      GL4 gl4, String name, int location, UniformType type, Object... args) {
    switch (type) {
      case INT1 -> {
        return new Int1(gl4, name, location);
      }
      case INT2 -> {
        return new Int2(gl4, name, location);
      }
      case INT3 -> {
        return new Int3(gl4, name, location);
      }
      case INT4 -> {
        return new Int4(gl4, name, location);
      }
      case BOOLEAN1 -> {
        return new Boolean1(gl4, name, location);
      }
      case BOOLEAN2 -> {
        return new Boolean2(gl4, name, location);
      }
      case FLOAT1 -> {
        return new Float1(gl4, name, location);
      }
      case FLOAT2 -> {
        return new Float2(gl4, name, location);
      }
      case FLOAT3 -> {
        return new Float3(gl4, name, location);
      }
      case FLOAT4 -> {
        return new Float4(gl4, name, location);
      }
      case INT1VEC -> {
        return new Int1Vec(gl4, name, location);
      }
      case INT2VEC -> {
        return new Int2Vec(gl4, name, location);
      }
      case INT3VEC -> {
        return new Int3Vec(gl4, name, location);
      }
      case INT4VEC -> {
        return new Int4Vec(gl4, name, location);
      }
      case FLOAT1VEC -> {
        return new Float1Vec(gl4, name, location);
      }
      case FLOAT2VEC -> {
        return new Float2Vec(gl4, name, location);
      }
      case FLOAT3VEC -> {
        return new Float3Vec(gl4, name, location);
      }
      case FLOAT4VEC -> {
        return new Float4Vec(gl4, name, location);
      }
      case MAT2 -> {
        return new Mat2(gl4, name, location);
      }
      case MAT3 -> {
        return new Mat3(gl4, name, location);
      }
      case MAT4 -> {
        return new Mat4(gl4, name, location);
      }
      case SAMPLER2D, SAMPLER2DSTATIC -> {
        return new Sampler2D(gl4, name, location, (int) args[0]);
      }
      default -> {
        throw new IllegalArgumentException("Unknown uniform type: " + type);
      }
    }
  }

  // Child classes

  public static class Int1 extends Uniform {
    public Int1(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.INT1);
    }

    // Overridable constructor for Boolean
    protected Int1(GL4 gl4, String name, int location, UniformType type) {
      super(gl4, name, location, type);
    }

    private int value = 0;

    public Int1 setValue(int value) {
      if (this.value != value) {
        this.value = value;
        this.modified = true;
      }
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform1i(this.location, this.value);
      this.modified = false;
    }
  }

  public static class Int2 extends Uniform {
    public Int2(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.INT2);
    }

    private final int[] value = new int[2];

    public Int2 setValue(int x, int y) {
      if (this.value[0] != x || this.value[1] != y) {
        this.value[0] = x;
        this.value[1] = y;
        this.modified = true;
      }
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform2i(this.location, this.value[0], this.value[1]);
      this.modified = false;
    }
  }

  public static class Int3 extends Uniform {
    public Int3(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.INT3);
    }

    private final int[] value = new int[3];

    public Int3 setValue(int x, int y, int z) {
      if (this.value[0] != x || this.value[1] != y || this.value[2] != z) {
        this.value[0] = x;
        this.value[1] = y;
        this.value[2] = z;
        this.modified = true;
      }
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform3i(this.location, this.value[0], this.value[1], this.value[2]);
      this.modified = false;
    }
  }

  public static class Int4 extends Uniform {
    public Int4(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.INT4);
    }

    private final int[] value = new int[4];

    public Int4 setValue(int x, int y, int z, int w) {
      if (this.value[0] != x || this.value[1] != y || this.value[2] != z || this.value[3] != w) {
        this.value[0] = x;
        this.value[1] = y;
        this.value[2] = z;
        this.value[3] = w;
        this.modified = true;
      }
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform4i(
          this.location, this.value[0], this.value[1], this.value[2], this.value[3]);
      this.modified = false;
    }
  }

  public static class Boolean1 extends Uniform {
    public Boolean1(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.BOOLEAN1);
    }

    private boolean value = false;

    public Boolean1 setValue(boolean value) {
      if (this.value != value) {
        this.value = value;
        this.modified = true;
      }
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform1i(this.location, this.value ? 1 : 0);
      this.modified = false;
    }
  }

  public static class Boolean2 extends Uniform {
    public Boolean2(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.BOOLEAN2);
    }

    private final boolean[] value = new boolean[2];

    public Boolean2 setValue(boolean x, boolean y) {
      if (this.value[0] != x || this.value[1] != y) {
        this.value[0] = x;
        this.value[1] = y;
        this.modified = true;
      }
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform2i(this.location, this.value[0] ? 1 : 0, this.value[1] ? 1 : 0);
      this.modified = false;
    }
  }

  public static class Float1 extends Uniform {
    public Float1(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.FLOAT1);
    }

    private float value = 0f;

    public Float1 setValue(float value) {
      if (this.value != value) {
        this.value = value;
        this.modified = true;
      }
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform1f(this.location, this.value);
      this.modified = false;
    }
  }

  public static class Float2 extends Uniform {
    public Float2(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.FLOAT2);
    }

    private final float[] value = new float[2];

    public Float2 setValue(float x, float y) {
      if (this.value[0] != x || this.value[1] != y) {
        this.value[0] = x;
        this.value[1] = y;
        this.modified = true;
      }
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform2f(this.location, this.value[0], this.value[1]);
      this.modified = false;
    }
  }

  public static class Float3 extends Uniform {
    public Float3(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.FLOAT3);
    }

    private final float[] value = new float[3];

    public Float3 setValue(float x, float y, float z) {
      if (this.value[0] != x || this.value[1] != y || this.value[2] != z) {
        this.value[0] = x;
        this.value[1] = y;
        this.value[2] = z;
        this.modified = true;
      }
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform3f(this.location, this.value[0], this.value[1], this.value[2]);
      this.modified = false;
    }
  }

  public static class Float4 extends Uniform {
    public Float4(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.FLOAT4);
    }

    private final float[] value = new float[4];

    public Float4 setValue(float x, float y, float z, float w) {
      if (this.value[0] != x || this.value[1] != y || this.value[2] != z || this.value[3] != w) {
        this.value[0] = x;
        this.value[1] = y;
        this.value[2] = z;
        this.value[3] = w;
        this.modified = true;
      }
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform4f(
          this.location, this.value[0], this.value[1], this.value[2], this.value[3]);
      this.modified = false;
    }
  }

  public static class Int1Vec extends Uniform {
    public Int1Vec(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.INT1VEC);
    }

    private IntBuffer vIArray = null;

    public Int1Vec setValue(IntBuffer vIArray) {
      this.vIArray = vIArray;
      this.modified = true;
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform1iv(this.location, this.vIArray.capacity(), this.vIArray);
      this.modified = false;
    }
  }

  public static class Int2Vec extends Uniform {
    public Int2Vec(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.INT2VEC);
    }

    private IntBuffer vIArray = null;

    public Int2Vec setValue(IntBuffer vIArray) {
      this.vIArray = vIArray;
      this.modified = true;
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform2iv(this.location, this.vIArray.capacity() / 2, this.vIArray);
      this.modified = false;
    }
  }

  public static class Int3Vec extends Uniform {
    public Int3Vec(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.INT3VEC);
    }

    private IntBuffer vIArray = null;

    public Int3Vec setValue(IntBuffer vIArray) {
      this.vIArray = vIArray;
      this.modified = true;
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform3iv(this.location, this.vIArray.capacity() / 3, this.vIArray);
      this.modified = false;
    }
  }

  public static class Int4Vec extends Uniform {
    public Int4Vec(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.INT4VEC);
    }

    private IntBuffer vIArray = null;

    public Int4Vec setValue(IntBuffer vIArray) {
      this.vIArray = vIArray;
      this.modified = true;
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform4iv(this.location, this.vIArray.capacity() / 4, this.vIArray);
      this.modified = false;
    }
  }

  public static class Float1Vec extends Uniform {
    public Float1Vec(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.FLOAT1VEC);
    }

    private FloatBuffer vFArray = null;

    public Float1Vec setValue(FloatBuffer vFArray) {
      this.vFArray = vFArray;
      this.modified = true;
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform1fv(this.location, this.vFArray.capacity(), this.vFArray);
      this.modified = false;
    }
  }

  public static class Float2Vec extends Uniform {
    public Float2Vec(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.FLOAT2VEC);
    }

    private FloatBuffer vFArray = null;

    public Float2Vec setValue(FloatBuffer vFArray) {
      this.vFArray = vFArray;
      this.modified = true;
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform2fv(this.location, this.vFArray.capacity() / 2, this.vFArray);
      this.modified = false;
    }
  }

  public static class Float3Vec extends Uniform {
    public Float3Vec(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.FLOAT3VEC);
    }

    private FloatBuffer vFArray = null;

    public Float3Vec setValue(FloatBuffer vFArray) {
      this.vFArray = vFArray;
      this.modified = true;
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform3fv(this.location, this.vFArray.capacity() / 3, this.vFArray);
      this.modified = false;
    }
  }

  public static class Float4Vec extends Uniform {
    public Float4Vec(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.FLOAT4VEC);
    }

    private FloatBuffer vFArray = null;

    public Float4Vec setValue(FloatBuffer vFArray) {
      this.vFArray = vFArray;
      this.modified = true;
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniform4fv(this.location, this.vFArray.capacity() / 4, this.vFArray);
      this.modified = false;
    }
  }

  public static class Mat2 extends Uniform {
    public Mat2(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.MAT2);
    }

    private FloatBuffer vFArray = null;

    public Mat2 setValue(FloatBuffer vFArray) {
      this.vFArray = vFArray;
      this.modified = true;
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniformMatrix2fv(this.location, 1, true, this.vFArray);
      this.modified = false;
    }
  }

  public static class Mat3 extends Uniform {
    public Mat3(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.MAT3);
    }

    private FloatBuffer vFArray = null;

    public Mat3 setValue(FloatBuffer vFArray) {
      this.vFArray = vFArray;
      this.modified = true;
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniformMatrix3fv(this.location, 1, true, this.vFArray);
      this.modified = false;
    }
  }

  public static class Mat4 extends Uniform {
    public Mat4(GL4 gl4, String name, int location) {
      super(gl4, name, location, UniformType.MAT4);
    }

    private FloatBuffer vFArray = null;

    public Mat4 setValue(FloatBuffer vFArray) {
      this.vFArray = vFArray;
      this.modified = true;
      return this;
    }

    @Override
    public void update() {
      this.gl4.glUniformMatrix4fv(this.location, 1, true, this.vFArray);
      this.modified = false;
    }
  }

  public static class Sampler2D extends Uniform {
    public Sampler2D(GL4 gl4, String name, int location, int textureUnit) {
      super(gl4, name, location, UniformType.SAMPLER2D);
      this.textureUnit = textureUnit;
    }

    private final int textureUnit;

    private boolean isObject = false;
    private Texture texture = null;
    private int textureHandle = -1;

    public Sampler2D setValue(Texture texture) {
      this.texture = texture;
      this.isObject = true;
      this.modified = true;
      return this;
    }

    public Sampler2D setValue(int textureHandle) {
      this.textureHandle = textureHandle;
      this.isObject = false;
      this.modified = true;
      return this;
    }

    private boolean loggedOnce = false;

    @Override
    public void update() {
      if (this.textureHandle == -1 && !this.loggedOnce) {
        this.loggedOnce = true;
        LX.error("Missing texture '" + this.name + "' for unit " + this.textureUnit + ", this could be a bug...");
      }
      gl4.glActiveTexture(GL_TEXTURE0 + this.textureUnit);
      if (this.isObject) {
        this.texture.bind(gl4);
      } else {
        gl4.glBindTexture(GL_TEXTURE_2D, this.textureHandle);
      }
      gl4.glUniform1i(this.location, this.textureUnit);
    }

    public void unbind() {
      gl4.glActiveTexture(GL_TEXTURE0 + this.textureUnit);
      gl4.glBindTexture(GL_TEXTURE_2D, 0);
    }
  }
}
