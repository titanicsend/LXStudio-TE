package titanicsend.pattern.glengine;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;

import Jama.Matrix;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import heronarts.lx.LX;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import titanicsend.pattern.yoffa.shader_engine.FragmentShader;
import titanicsend.pattern.yoffa.shader_engine.ShaderAttribute;
import titanicsend.pattern.yoffa.shader_engine.ShaderProgram;
import titanicsend.pattern.yoffa.shader_engine.Uniform;
import titanicsend.pattern.yoffa.shader_engine.UniformType;

/** Shader program components that are currently common across shader classes. */
public abstract class GLShader {

  /**
   * Callback interface to set any uniforms that have been modified since the last frame. Can be
   * implemented by child classes or provided to the constructor.
   */
  public interface UniformSource {
    /** Called once per frame. Set uniforms on the shader here. */
    void setUniforms(GLShader s);
  }

  // Vertices for default geometry - a rectangle that covers the entire canvas
  private static final float[] VERTICES = {
    1.0f, 1.0f, 0.0f,
    1.0f, -1.0f, 0.0f,
    -1.0f, -1.0f, 0.0f,
    -1.0f, 1.0f, 0.0f
  };

  // Vertex index list for default geometry. Since we're drawing
  // with triangles, we need two to make our rectangle!
  protected static final int[] INDICES = {
    0, 1, 2,
    2, 0, 3
  };

  // Local copies of frequently used OpenGL objects
  protected final LX lx;
  protected final GLEngine glEngine;
  private GLAutoDrawable canvas = null;
  protected GL4 gl4 = null;

  protected ShaderProgram shaderProgram;
  protected final FragmentShader fragmentShader;
  protected final int width;
  protected final int height;

  // Geometry buffers
  private final FloatBuffer vertexBuffer;
  private final IntBuffer indexBuffer;
  private final int[] vaoHandles = new int[1];
  private final int[] geometryBufferHandles = new int[2];

  // Uniforms
  private final HashMap<String, Uniform> uniformMap = new HashMap<>();
  private final List<Uniform> mutableUniforms = new ArrayList<>();
  public final List<Uniform> uniforms = Collections.unmodifiableList(this.mutableUniforms);

  // Map of uniform names to GL texture units
  protected final HashMap<String, Integer> uniformTextureUnits = new HashMap<>();

  // Callbacks used by the pattern or effect that owns the shader to set any custom uniforms.
  private final List<UniformSource> uniformSources = new ArrayList<>();

  private boolean initialized = false;

  /** Helper to construct a FragmentShader inline */
  protected static FragmentShader newFragmentShader(
      String shaderFilename, String... textureFilenames) {
    return new FragmentShader(
        new File("resources/shaders/" + shaderFilename),
        Arrays.stream(textureFilenames)
            .map(x -> new File("resources/shaders/textures/" + x))
            .collect(Collectors.toList()));
  }

  // Welcome to the Land of 1000 Constructors!

  public GLShader(LX lx, FragmentShader fragmentShader) {
    this.lx = lx;
    this.glEngine = (GLEngine) lx.engine.getChild(GLEngine.PATH);
    this.width = this.glEngine.getWidth();
    this.height = this.glEngine.getHeight();

    // initialization that can be done before the OpenGL context is available
    this.fragmentShader = Objects.requireNonNull(fragmentShader);
    this.vertexBuffer = Buffers.newDirectFloatBuffer(VERTICES.length);
    this.indexBuffer = Buffers.newDirectIntBuffer(INDICES.length);
    this.vertexBuffer.put(VERTICES);
    this.indexBuffer.put(INDICES);
  }

  /**
   * Create new OpenGL shader effect
   *
   * @param lx LX instance
   * @param fragmentShader fragment shader object to use that need to read the previous frame. If
   *     null, a buffer will be automatically allocated.
   * @param uniformSources list of callbacks that will set uniforms on this shader
   */
  public GLShader(LX lx, FragmentShader fragmentShader, List<UniformSource> uniformSources) {
    this(lx, fragmentShader);

    for (UniformSource uniformSource : uniformSources) {
      if (uniformSource != null) {
        addUniformSource(uniformSource);
      }
    }
  }

  /**
   * Create new OpenGL shader effect
   *
   * @param lx LX instance
   * @param fragmentShader fragment shader object to use
   * @param uniformSource callback that will set uniforms on this shader
   */
  public GLShader(LX lx, FragmentShader fragmentShader, UniformSource uniformSource) {
    this(lx, fragmentShader, List.of(uniformSource));
  }

  /**
   * Creates new shader object with additional texture support
   *
   * @param lx LX instance
   * @param shaderFilename shader to use
   * @param uniformSources list of callbacks that will set uniforms on this shader
   * @param textureFilenames (optional) texture files to load
   */
  public GLShader(
      LX lx,
      String shaderFilename,
      List<UniformSource> uniformSources,
      String... textureFilenames) {
    this(lx, newFragmentShader(shaderFilename, textureFilenames), uniformSources);
  }

  /**
   * Creates new shader object with additional texture support
   *
   * @param lx LX instance
   * @param shaderFilename shader to use
   * @param uniformSource callback that will set uniforms on this shader
   * @param textureFilenames (optional) texture files to load
   */
  public GLShader(
      LX lx, String shaderFilename, UniformSource uniformSource, String... textureFilenames) {
    this(lx, shaderFilename, List.of(uniformSource), textureFilenames);
  }

  // Buffers

  /**
   * Create appropriately sized gl-compatible buffer for reading offscreen surface to cpu memory.
   * This is the preferred way to allocate the backbuffer if your pattern runs multiple shaders
   * which need to share the same buffer.
   *
   * @return ByteBuffer
   */
  public static ByteBuffer allocateBackBuffer() {
    return GLBuffers.newDirectByteBuffer(
        GLEngine.current.getWidth() * GLEngine.current.getHeight() * 4);
  }

  /**
   * Create appropriately sized buffer to contain a 2D texture mapped version of the last rendered
   * frame.
   *
   * @return ByteBuffer
   */
  public static ByteBuffer allocateMappedBuffer(int width, int height) {
    return GLBuffers.newDirectByteBuffer(width * height * 4);
  }

  // Setup

  protected GLShader addUniformSource(UniformSource uniformSource) {
    if (uniformSource == null) {
      throw new IllegalArgumentException("UniformSource cannot be null");
    }
    this.uniformSources.add(uniformSource);
    return this;
  }

  // get the active GL4 object for this shader.
  public GL4 getGL4() {
    return gl4;
  }

  // Initialization

  public void onActive() {
    if (!initialized) {
      init();
    }
  }

  public void onInactive() {
    // TODO - anything we can temporarily release here?
    // for the moment, we're leaving the shader program intact
    // so it is very fast to reactivate.
  }

  public boolean isInitialized() {
    return this.initialized;
  }

  /** Shader initialization that requires the OpenGL context. Call once. */
  public final void init() {
    if (this.initialized) {
      throw new IllegalStateException("Shader already initialized");
    }
    this.initialized = true;

    // The LX engine thread should have been initialized by now, so
    // we can safely retrieve our OpenGL canvas and context from the
    // glEngine task.
    this.canvas = this.glEngine.getCanvas();
    this.gl4 = this.canvas.getGL().getGL4();

    // uncomment to enable OpenGL debug output
    // context.getGL().getGL4().glEnable(GL_DEBUG_OUTPUT);

    // complete the initialization of the shader program
    this.canvas.getContext().makeCurrent();
    initShaderProgram();
    allocateShaderBuffers();
  }

  private void initShaderProgram() {
    this.shaderProgram = new ShaderProgram(this.gl4, this.fragmentShader.getShaderName());
  }

  protected void allocateShaderBuffers() {
    // Shader geometry is the same across all our shader classes, for now.

    // storage for geometry buffer handles
    gl4.glGenVertexArrays(1, this.vaoHandles, 0);
    gl4.glGenBuffers(2, IntBuffer.wrap(geometryBufferHandles));

    // bind Vertex Array Object first
    gl4.glBindVertexArray(this.vaoHandles[0]);

    // vertices
    vertexBuffer.rewind();
    gl4.glBindBuffer(GL_ARRAY_BUFFER, geometryBufferHandles[0]);
    gl4.glBufferData(
        GL_ARRAY_BUFFER,
        (long) vertexBuffer.capacity() * Float.BYTES,
        vertexBuffer,
        GL.GL_STATIC_DRAW);

    // geometry (triangles built from vertices)
    indexBuffer.rewind();
    gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, geometryBufferHandles[1]);
    gl4.glBufferData(
        GL_ELEMENT_ARRAY_BUFFER,
        (long) indexBuffer.capacity() * Integer.BYTES,
        indexBuffer,
        GL.GL_STATIC_DRAW);

    int position = gl4.glGetAttribLocation(shaderProgram.id, ShaderAttribute.POSITION);
    gl4.glVertexAttribPointer(position, 3, GL4.GL_FLOAT, false, 0, 0);
    gl4.glEnableVertexAttribArray(position);

    // finished with VAO
    gl4.glBindVertexArray(0);
  }

  // Run loop

  public void run() {
    this.canvas.getContext().makeCurrent();
    useProgram();
    // Stage updates to uniforms
    setUniforms();
    // hand the complete uniform list to OpenGL
    updateUniforms();
    render();
  }

  /** Activate this shader for rendering in the current context */
  private void useProgram() {
    this.gl4.glUseProgram(this.shaderProgram.id);
  }

  private void setUniforms() {
    for (UniformSource uniformSource : this.uniformSources) {
      uniformSource.setUniforms(this);
    }
  }

  /** Pass all modified (and new) uniform values to OpenGL */
  private void updateUniforms() {
    for (Uniform uniform : this.uniforms) {
      if (uniform.hasUpdate()) {
        uniform.update();
      }
    }
  }

  /** Child classes should run GL draw commands here */
  protected abstract void render();

  protected int getVaoHandle() {
    return this.vaoHandles[0];
  }

  // Staging Uniforms

  /*
  Generic shader uniform handler - based on processing.opengl.PShader (www.processing.org)
  The idea is that this mechanism can handle our normal shadertoy-like and LX engine generated
  uniforms, plus any other uniforms the user wants to use.  This makes it easier to do things like
  sending the current foreground color to the shader as a vec3.

  When doing this, THE PATTERN/SHADER AUTHOR IS RESPONSIBLE for seeing that the uniform names match
  in the Java pattern code and the shader.  Otherwise... nothing ... will happen.

  Sigh... most of this code consists of ten zillion overloaded methods to set uniforms of all the supported
  types. Oh yeah, and a couple of worker methods to manage the uniform list we're building.
  */

  /** setter -- single int */
  public Uniform.Int1 setUniform(String name, int x) {
    return ((Uniform.Int1) getUniform(name, UniformType.INT1)).setValue(x);
  }

  /** 2 element int array or ivec2 */
  public Uniform.Int2 setUniform(String name, int x, int y) {
    return ((Uniform.Int2) getUniform(name, UniformType.INT2)).setValue(x, y);
  }

  /** 3 element int array or ivec3 */
  public Uniform.Int3 setUniform(String name, int x, int y, int z) {
    return ((Uniform.Int3) getUniform(name, UniformType.INT3)).setValue(x, y, z);
  }

  /** 4 element int array or ivec4 */
  public Uniform.Int4 setUniform(String name, int x, int y, int z, int w) {
    return ((Uniform.Int4) getUniform(name, UniformType.INT4)).setValue(x, y, z, w);
  }

  /** single float */
  public Uniform.Float1 setUniform(String name, float x) {
    return ((Uniform.Float1) getUniform(name, UniformType.FLOAT1)).setValue(x);
  }

  /** 2 element float array or vec2 */
  public Uniform.Float2 setUniform(String name, float x, float y) {
    return ((Uniform.Float2) getUniform(name, UniformType.FLOAT2)).setValue(x, y);
  }

  /** 3 element float array or vec3 */
  public Uniform.Float3 setUniform(String name, float x, float y, float z) {
    return ((Uniform.Float3) getUniform(name, UniformType.FLOAT3)).setValue(x, y, z);
  }

  /** 4 element float array or vec4 */
  public Uniform.Float4 setUniform(String name, float x, float y, float z, float w) {
    return ((Uniform.Float4) getUniform(name, UniformType.FLOAT4)).setValue(x, y, z, w);
  }

  public Uniform.Boolean1 setUniform(String name, boolean x) {
    return ((Uniform.Boolean1) getUniform(name, UniformType.BOOLEAN1)).setValue(x);
  }

  public Uniform.Boolean2 setUniform(String name, boolean x, boolean y) {
    return ((Uniform.Boolean2) getUniform(name, UniformType.BOOLEAN2)).setValue(x, y);
  }

  /**
   * Create SAMPLER2D uniform from jogl Texture object - this prototype supports both dynamic and
   * static textures. If you know your texture will be changed on every frame, use setUniform(String
   * name, Texture tex) instead. TODO - STATIC TEXTURES NOT YET IMPLEMENTED
   */
  public Uniform.Sampler2D setUniform(String name, Texture tex, boolean isStatic) {
    return ((Uniform.Sampler2D) getUniform(name, UniformType.SAMPLER2D)).setValue(tex);
  }

  /**
   * Create SAMPLER2D uniform from jogl Texture object - for dynamic textures (that change every
   * frame). For static textures, use setUniform(String name, Texture tex,boolean isStatic) instead.
   * TODO - static/dynamic textures not yet implemented. All textures are treated TODO - as dynamic
   * and reloaded on every frame.
   */
  public Uniform.Sampler2D setUniform(String name, Texture tex) {
    return ((Uniform.Sampler2D) getUniform(name, UniformType.SAMPLER2DSTATIC)).setValue(tex);
  }

  /**
   * @param columns number of coordinates per element, max 4
   */
  public Uniform setUniform(String name, IntBuffer vec, int columns) {
    return switch (columns) {
      case 1 -> ((Uniform.Int1Vec) getUniform(name, UniformType.INT1VEC)).setValue(vec);
      case 2 -> ((Uniform.Int2Vec) getUniform(name, UniformType.INT2VEC)).setValue(vec);
      case 3 -> ((Uniform.Int3Vec) getUniform(name, UniformType.INT3VEC)).setValue(vec);
      case 4 -> ((Uniform.Int4Vec) getUniform(name, UniformType.INT4VEC)).setValue(vec);
      default -> throw new IllegalArgumentException("Invalid number of columns: " + columns);
    };
  }

  public Uniform setUniform(String name, FloatBuffer vec, int columns) {
    return switch (columns) {
      case 1 -> ((Uniform.Float1Vec) getUniform(name, UniformType.FLOAT1VEC)).setValue(vec);
      case 2 -> ((Uniform.Float2Vec) getUniform(name, UniformType.FLOAT2VEC)).setValue(vec);
      case 3 -> ((Uniform.Float3Vec) getUniform(name, UniformType.FLOAT3VEC)).setValue(vec);
      case 4 -> ((Uniform.Float4Vec) getUniform(name, UniformType.FLOAT4VEC)).setValue(vec);
      default -> throw new IllegalArgumentException("Invalid number of columns: " + columns);
    };
  }

  /**
   * Internal - Creates a uniform for a square floating point matrix, of size 2x2, 3x3 or 4x4
   *
   * @param name of uniform
   * @param vec Floating point matrix data, in row major order
   * @param sz Size of matrix (Number of rows & columns. 2,3 or 4)
   */
  public Uniform setUniformMatrix(String name, FloatBuffer vec, int sz) {
    return switch (sz) {
      case 2 -> ((Uniform.Mat2) getUniform(name, UniformType.MAT2)).setValue(vec);
      case 3 -> ((Uniform.Mat3) getUniform(name, UniformType.MAT3)).setValue(vec);
      case 4 -> ((Uniform.Mat4) getUniform(name, UniformType.MAT4)).setValue(vec);
      default -> throw new IllegalArgumentException("Invalid matrix size: " + sz);
    };
  }

  /**
   * Sets a uniform for a square floating point matrix, of type MAT2(2x2), MAT3(3x3) or MAT4(4x4).
   * Input matrix must be in row major order.
   */
  public Uniform setUniform(String name, float[][] matrix) {
    // requires a square matrix of dimension 2x2,3x3 or 4x4
    int dim = matrix.length;
    if (dim < 2 || dim > 4) {
      throw new IllegalArgumentException(
          "Invalid matrix dimension " + dim + " for uniform " + name);
    }

    FloatBuffer buf = Buffers.newDirectFloatBuffer(dim);

    // load matrix into buffer in row major order
    for (int r = 0; r < dim; r++) {
      for (int c = 0; c < dim; c++) {
        buf.put(matrix[r][c]);
      }
    }

    // and set the uniform object
    buf.rewind();
    return setUniformMatrix(name, buf, dim);
  }

  /**
   * Sets a uniform for a square floating point matrix, of type MAT2(2x2), MAT3(3x3) or MAT4(4x4)
   */
  public Uniform setUniform(String name, Matrix matrix) {
    // requires a square matrix of dimension 2x2,3x3 or 4x4
    int dim = matrix.getRowDimension();
    if (dim < 2 || dim > 4 || dim != matrix.getColumnDimension()) {
      throw new IllegalArgumentException(
          "Invalid matrix dimension " + dim + " for uniform " + name);
    }

    // allocate buffer to send to OpenGl
    FloatBuffer buf = Buffers.newDirectFloatBuffer(dim);

    // load matrix into buffer in row major order
    for (int r = 0; r < dim; r++) {
      for (int c = 0; c < dim; c++) {
        buf.put((float) matrix.get(r, c));
      }
    }

    // and set the uniform object
    buf.rewind();
    return setUniformMatrix(name, buf, dim);
  }

  public Uniform.Int1 getUniformInt1(String name) {
    return (Uniform.Int1) getUniform(name, UniformType.INT1);
  }

  public Uniform.Int2 getUniformInt2(String name) {
    return (Uniform.Int2) getUniform(name, UniformType.INT2);
  }

  public Uniform.Int3 getUniformInt3(String name) {
    return (Uniform.Int3) getUniform(name, UniformType.INT3);
  }

  public Uniform.Int4 getUniformInt4(String name) {
    return (Uniform.Int4) getUniform(name, UniformType.INT4);
  }

  public Uniform.Float1 getUniformFloat1(String name) {
    return (Uniform.Float1) getUniform(name, UniformType.FLOAT1);
  }

  public Uniform.Float2 getUniformFloat2(String name) {
    return (Uniform.Float2) getUniform(name, UniformType.FLOAT2);
  }

  public Uniform.Float3 getUniformFloat3(String name) {
    return (Uniform.Float3) getUniform(name, UniformType.FLOAT3);
  }

  public Uniform.Float4 getUniformFloat4(String name) {
    return (Uniform.Float4) getUniform(name, UniformType.FLOAT4);
  }

  public Uniform.Boolean1 getUniformBoolean1(String name) {
    return (Uniform.Boolean1) getUniform(name, UniformType.BOOLEAN1);
  }

  public Uniform.Boolean2 getUniformBoolean2(String name) {
    return (Uniform.Boolean2) getUniform(name, UniformType.BOOLEAN2);
  }

  public Uniform.Int1Vec getUniformInt1Vec(String name) {
    return (Uniform.Int1Vec) getUniform(name, UniformType.INT1VEC);
  }

  public Uniform.Int2Vec getUniformInt2Vec(String name) {
    return (Uniform.Int2Vec) getUniform(name, UniformType.INT2VEC);
  }

  public Uniform.Int3Vec getUniformInt3Vec(String name) {
    return (Uniform.Int3Vec) getUniform(name, UniformType.INT3VEC);
  }

  public Uniform.Int4Vec getUniformInt4Vec(String name) {
    return (Uniform.Int4Vec) getUniform(name, UniformType.INT4VEC);
  }

  public Uniform.Float1Vec getUniformFloat1Vec(String name) {
    return (Uniform.Float1Vec) getUniform(name, UniformType.FLOAT1VEC);
  }

  public Uniform.Float2Vec getUniformFloat2Vec(String name) {
    return (Uniform.Float2Vec) getUniform(name, UniformType.FLOAT2VEC);
  }

  public Uniform.Float3Vec getUniformFloat3Vec(String name) {
    return (Uniform.Float3Vec) getUniform(name, UniformType.FLOAT3VEC);
  }

  public Uniform.Float4Vec getUniformFloat4Vec(String name) {
    return (Uniform.Float4Vec) getUniform(name, UniformType.FLOAT4VEC);
  }

  public Uniform.Mat2 getUniformMat2(String name) {
    return (Uniform.Mat2) getUniform(name, UniformType.MAT2);
  }

  public Uniform.Mat3 getUniformMat3(String name) {
    return (Uniform.Mat3) getUniform(name, UniformType.MAT3);
  }

  public Uniform.Mat4 getUniformMat4(String name) {
    return (Uniform.Mat4) getUniform(name, UniformType.MAT4);
  }

  public Uniform.Sampler2D getUniformSampler2D(String name) {
    return (Uniform.Sampler2D) getUniform(name, UniformType.SAMPLER2D);
  }

  public Uniform.Sampler2D getUniformSampler2DStatic(String name) {
    return (Uniform.Sampler2D) getUniform(name, UniformType.SAMPLER2DSTATIC);
  }

  /**
   * Retrieve a Uniform by name and type. Creates a uniform for this shader instance if it does not
   * exist.
   */
  public Uniform getUniform(String name, UniformType type) {
    Uniform uniform = this.uniformMap.get(name);
    if (uniform == null) {
      // First time accessing this uniform, create a new object.
      int location = this.gl4.glGetUniformLocation(this.shaderProgram.id, name);
      // Special handling for Sampler2D, textureUnit will be set once in the constructor.
      if (type == UniformType.SAMPLER2D || type == UniformType.SAMPLER2DSTATIC) {
        uniform = Uniform.create(this.gl4, name, location, type, getTextureUnit(name));
      } else {
        uniform = Uniform.create(this.gl4, name, location, type);
      }
      this.uniformMap.put(name, uniform);
      this.mutableUniforms.add(uniform);
    }
    // else {
    //   We could double-check the type here, but let's keep it fast for a tight loop.
    //   If the type doesn't match it will blow up somewhere else.
    // }
    return uniform;
  }

  // get a texture unit for a uniform either from uniformTextureUnits or by allocating a new one
  protected int getTextureUnit(String name) {
    if (uniformTextureUnits.containsKey(name)) {
      return uniformTextureUnits.get(name);
    } else {
      int unit = getNextTextureUnit();
      uniformTextureUnits.put(name, unit);
      return unit;
    }
  }

  /** Tracks texture units within the context of this shader */
  private int nextTextureUnit = TextureManager.FIRST_UNRESERVED_TEXTURE_UNIT;

  protected int getNextTextureUnit() {
    return this.nextTextureUnit++;
  }

  public void dispose() {
    // Release references to uniform objects
    this.uniformMap.clear();
    this.mutableUniforms.clear();

    if (this.initialized) {
      // delete GPU buffers we directly allocated
      gl4.glDeleteBuffers(2, geometryBufferHandles, 0);
      gl4.glDeleteVertexArrays(1, vaoHandles, 0);

      this.shaderProgram.dispose();
    }
  }
}
