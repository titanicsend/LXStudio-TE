package titanicsend.pattern.glengine;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_BGRA;
import static com.jogamp.opengl.GL.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL.GL_COLOR_ATTACHMENT0;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import static com.jogamp.opengl.GL.GL_ELEMENT_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_FRAMEBUFFER;
import static com.jogamp.opengl.GL.GL_FRAMEBUFFER_COMPLETE;
import static com.jogamp.opengl.GL.GL_LINEAR;
import static com.jogamp.opengl.GL.GL_RGBA8;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;

import Jama.Matrix;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import heronarts.lx.LX;
import heronarts.lx.utils.LXUtils;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.lwjgl.BufferUtils;
import titanicsend.pattern.yoffa.shader_engine.FragmentShader;
import titanicsend.pattern.yoffa.shader_engine.ShaderAttribute;
import titanicsend.pattern.yoffa.shader_engine.ShaderProgram;
import titanicsend.pattern.yoffa.shader_engine.Uniform;
import titanicsend.pattern.yoffa.shader_engine.UniformNames;
import titanicsend.pattern.yoffa.shader_engine.UniformType;

/** Shader program components that are currently common across shader classes. */
public abstract class GLShader {

  // Reserved texture unit assignments in our context
  public static final int TEXTURE_UNIT_AUDIO = 0;
  public static final int TEXTURE_UNIT_COORDS = 1;
  public static final int TEXTURE_UNIT_BACKBUFFER = 2;
  public static final int FIRST_UNRESERVED_TEXTURE_UNIT = 3;

  /**
   * Callback interface to set any uniforms that have been modified since the last frame. Can be
   * implemented by child classes or provided to the constructor.
   */
  public interface UniformSource {
    /** Called once per frame. Set uniforms on the shader here. */
    void setUniforms(GLShader s);

    /** Called once per frame after render(). Unbind any Sampler2D uniforms. */
    default void unbindTextures() {}
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

  /** Tracks texture units within the context of this shader */
  private int nextTextureUnit = FIRST_UNRESERVED_TEXTURE_UNIT;

  /** Helper class to handle a variety of constructor parameters. */
  public static class Config {

    private final LX lx;
    private String shaderFilename;
    private final List<String> textureFilenames = new ArrayList<>();
    private final List<UniformSource> uniformSources = new ArrayList<>();
    private ByteBuffer legacyBackBuffer;

    public Config(LX lx) {
      this.lx = lx;
    }

    public Config withFilename(String shaderFilename) {
      if (this.shaderFilename != null) {
        throw new IllegalStateException("Shader filename can only  be set once");
      }
      this.shaderFilename = Objects.requireNonNull(shaderFilename);
      return this;
    }

    public Config withTextures(String... textureFilenames) {
      for (String textureFilename : textureFilenames) {
        if (LXUtils.isEmpty(textureFilename)) {
          throw new IllegalArgumentException("Texture filename cannot be null or empty");
        }
        this.textureFilenames.add(textureFilename);
      }
      return this;
    }

    public Config withUniformSource(UniformSource... uniformSources) {
      for (UniformSource uniformSource : uniformSources) {
        if (uniformSource == null) {
          throw new IllegalArgumentException("UniformSource cannot be null");
        }
        this.uniformSources.add(uniformSource);
      }
      return this;
    }

    public Config withLegacyBackBuffer(ByteBuffer legacyBackBuffer) {
      this.legacyBackBuffer = Objects.requireNonNull(legacyBackBuffer);
      return this;
    }

    public String getShaderFilename() {
      return this.shaderFilename;
    }

    public List<String> getTextureFilenames() {
      return this.textureFilenames;
    }

    public List<UniformSource> getUniformSources() {
      return this.uniformSources;
    }

    public ByteBuffer getLegacyBackBuffer() {
      return this.legacyBackBuffer;
    }
  }

  /** Create a new set of constructor parameters for GLShader */
  public static Config config(LX lx) {
    return new Config(lx);
  }

  private final List<UniformSource> configUniformSources;

  public GLShader(Config config) {
    this.lx = config.lx;
    this.glEngine = (GLEngine) lx.engine.getChild(GLEngine.PATH);
    this.width = this.glEngine.getWidth();
    this.height = this.glEngine.getHeight();

    // Fragment Shader
    if (LXUtils.isEmpty(config.getShaderFilename())) {
      throw new IllegalArgumentException("Shader filename cannot be null or empty");
    }
    File shaderFile = new File("resources/shaders/" + config.getShaderFilename());
    List<File> textureFiles =
        config.getTextureFilenames().stream()
            .map(x -> new File("resources/shaders/textures/" + x))
            .toList();
    this.fragmentShader = new FragmentShader(shaderFile, textureFiles);

    // Wonky... uniformSources added from child constructors need to go *before* configs
    this.configUniformSources = config.getUniformSources();

    // initialization that can be done before the OpenGL context is available
    this.vertexBuffer = Buffers.newDirectFloatBuffer(VERTICES.length);
    this.indexBuffer = Buffers.newDirectIntBuffer(INDICES.length);
    this.vertexBuffer.put(VERTICES);
    this.indexBuffer.put(INDICES);

    // Reserved texture units
    this.uniformTextureUnits.put(UniformNames.BACK_BUFFER, TEXTURE_UNIT_BACKBUFFER);
    this.uniformTextureUnits.put(UniformNames.LX_MODEL_COORDS, TEXTURE_UNIT_COORDS);
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
    return this.gl4;
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

    // Add config uniformSources now, they will be after those added from child constructors...
    for (UniformSource uniformSource : this.configUniformSources) {
      addUniformSource(uniformSource);
    }

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
    this.shaderProgram =
        new ShaderProgram(this.gl4, this.fragmentShader.getShaderName(), useTEPreProcess());
  }

  /** Subclasses can override to suppress TE shader pre-processing */
  protected boolean useTEPreProcess() {
    return true;
  }

  protected void allocateShaderBuffers() {
    // Shader geometry is the same across all our shader classes, for now.

    // storage for geometry buffer handles
    this.gl4.glGenVertexArrays(1, this.vaoHandles, 0);
    this.gl4.glGenBuffers(2, IntBuffer.wrap(geometryBufferHandles));

    // bind Vertex Array Object first
    this.gl4.glBindVertexArray(this.vaoHandles[0]);

    // vertices
    vertexBuffer.rewind();
    this.gl4.glBindBuffer(GL_ARRAY_BUFFER, geometryBufferHandles[0]);
    this.gl4.glBufferData(
        GL_ARRAY_BUFFER,
        (long) vertexBuffer.capacity() * Float.BYTES,
        vertexBuffer,
        GL.GL_STATIC_DRAW);

    // geometry (triangles built from vertices)
    indexBuffer.rewind();
    this.gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, geometryBufferHandles[1]);
    this.gl4.glBufferData(
        GL_ELEMENT_ARRAY_BUFFER,
        (long) indexBuffer.capacity() * Integer.BYTES,
        indexBuffer,
        GL.GL_STATIC_DRAW);

    // vertex attributes
    int position = this.gl4.glGetAttribLocation(shaderProgram.id, ShaderAttribute.POSITION);
    this.gl4.glVertexAttribPointer(position, 3, GL4.GL_FLOAT, false, 0, 0);
    this.gl4.glEnableVertexAttribArray(position);

    // Unbind VAO to prevent subsequent setup from modifying it by mistake
    this.gl4.glBindVertexArray(0);
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
    unbindTextures();
    activateDefaultTextureUnit();
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

  private void unbindTextures() {
    for (UniformSource uniformSource : this.uniformSources) {
      uniformSource.unbindTextures();
    }
  }

  /** Activates texture unit 0. */
  protected void activateDefaultTextureUnit() {
    this.gl4.glActiveTexture(GL_TEXTURE0);
  }

  /**
   * Helper method to activate a texture unit and bind a texture to it. In higher OpenGL versions
   * (4.5+) this method is built-in.
   *
   * @param unit Texture unit to activate (0+)
   * @param textureHandle Texture handle that should be bound to the unit
   */
  protected void bindTextureUnit(int unit, int textureHandle) {
    this.glEngine.bindTextureUnit(unit, textureHandle);
  }

  /**
   * Helper method to unbind the current texture from a texture unit
   *
   * @param unit Texture unit to bind to "0"
   */
  protected void unbindTextureUnit(int unit) {
    this.glEngine.unbindTextureUnit(unit);
  }

  /** Bind the vertex array object */
  protected void bindVAO() {
    this.gl4.glBindVertexArray(this.vaoHandles[0]);
  }

  protected void drawElements() {
    this.gl4.glDrawElements(GL2.GL_TRIANGLES, INDICES.length, GL2.GL_UNSIGNED_INT, 0);
  }

  /** Debug tool. Prints the first values in a texture to the console. */
  public void debugTexture(String label, int textureId) {
    bindTextureUnit(0, textureId);

    ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);

    this.gl4.glGetTexImage(GL_TEXTURE_2D, 0, GL_BGRA, GL4.GL_UNSIGNED_BYTE, pixels);

    StringBuilder sb = new StringBuilder();
    pixels.rewind();
    sb.append(label).append(": ");
    for (int i = 0; i < 30 && pixels.hasRemaining(); i++) {
      sb.append(byteToThreeCharString(pixels.get()));
      if (i < 29 && pixels.hasRemaining()) {
        sb.append(" ");
      }
    }
    System.out.println(sb);

    this.gl4.glBindTexture(GL_TEXTURE_2D, 0);
  }

  private static String byteToThreeCharString(byte b) {
    int unsignedValue = b & 0xFF;
    String result = String.valueOf(unsignedValue);
    while (result.length() < 3) {
      result = " " + result;
    }
    return result;
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

  /**
   * Retrieve the next unreserved texture unit for this shader. The GL context must be set before
   * calling this method, as the unit will be enabled.
   *
   * @return the assigned texture unit
   */
  protected int getNextTextureUnit() {
    return this.nextTextureUnit++;
  }

  public void dispose() {
    // Release references to uniform objects
    this.uniformMap.clear();
    this.mutableUniforms.clear();

    if (this.initialized) {
      // delete GPU buffers we directly allocated
      this.gl4.glDeleteBuffers(2, geometryBufferHandles, 0);
      this.gl4.glDeleteVertexArrays(1, vaoHandles, 0);

      this.shaderProgram.dispose();
    }
  }

  // Helper classes for GL buffers

  /** Framebuffer Object */
  protected class FBO {

    final int[] textureHandles = new int[1];
    final int[] fboHandles = new int[1];

    public FBO() {
      // Generate handles
      gl4.glGenTextures(1, this.textureHandles, 0);
      gl4.glGenFramebuffers(1, this.fboHandles, 0);

      // Texture
      gl4.glBindTexture(GL_TEXTURE_2D, this.textureHandles[0]);
      gl4.glTexImage2D(
          GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_BGRA, GL_UNSIGNED_BYTE, null);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

      // Framebuffer
      gl4.glBindFramebuffer(GL_FRAMEBUFFER, this.fboHandles[0]);
      gl4.glFramebufferTexture2D(
          GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, this.textureHandles[0], 0);

      // Check framebuffer status
      int status = gl4.glCheckFramebufferStatus(GL_FRAMEBUFFER);
      if (status != GL_FRAMEBUFFER_COMPLETE) {
        throw new IllegalStateException("FBO failed to initialize: " + status);
      }

      // Initialize the texture to black
      gl4.glViewport(0, 0, width, height);
      gl4.glClearColor(0f, 0f, 0f, 1f);
      gl4.glClear(GL_COLOR_BUFFER_BIT);

      // Clean up - unbind texture and framebuffer
      gl4.glBindTexture(GL_TEXTURE_2D, 0);
      gl4.glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    public int getTextureHandle() {
      return this.textureHandles[0];
    }

    public int getFboHandle() {
      return this.fboHandles[0];
    }

    public void bind() {
      bind(false);
    }

    public void bind(boolean zeroAlpha) {
      gl4.glBindFramebuffer(GL_FRAMEBUFFER, this.fboHandles[0]);
      gl4.glViewport(0, 0, width, height);
      gl4.glClearColor(0.0f, 0.0f, 0.0f, zeroAlpha ? 0f : 1.0f);
      gl4.glClear(GL_COLOR_BUFFER_BIT);
    }

    public void dispose() {
      gl4.glDeleteFramebuffers(1, this.fboHandles, 0);
      gl4.glDeleteTextures(1, this.textureHandles, 0);
    }
  }

  protected class PingPongFBO {
    /** FBO currently being rendered to */
    FBO render = new FBO();

    /** Older FBO available for reading */
    FBO copy = new FBO();

    protected PingPongFBO() {}

    void swap() {
      FBO temp = this.copy;
      this.copy = render;
      this.render = temp;
    }

    void dispose() {
      this.copy.dispose();
      this.render.dispose();
    }
  }

  /** Pixel Pack Buffer */
  protected class PBO {
    private final int[] handles = new int[1];

    public PBO() {
      gl4.glGenBuffers(1, handles, 0);
      gl4.glBindBuffer(GL4.GL_PIXEL_PACK_BUFFER, this.handles[0]);
      gl4.glBufferData(
          GL4.GL_PIXEL_PACK_BUFFER, (long) width * height * 4, null, GL4.GL_STREAM_READ);
      gl4.glBindBuffer(GL4.GL_PIXEL_PACK_BUFFER, 0);
    }

    public int getHandle() {
      return handles[0];
    }

    public void bind() {
      gl4.glBindBuffer(GL4.GL_PIXEL_PACK_BUFFER, this.handles[0]);
    }

    public void unbind() {
      gl4.glBindBuffer(GL4.GL_PIXEL_PACK_BUFFER, 0);
    }

    public void dispose() {
      gl4.glDeleteBuffers(1, handles, 0);
    }
  }

  protected class PingPongPBO {
    // PBO currently being rendered to
    public PBO render = new PBO();

    // Older PBO available for reading
    public PBO copy = new PBO();

    public PingPongPBO() {}

    public void swap() {
      PBO temp = this.render;
      this.render = copy;
      this.copy = temp;
    }

    public void dispose() {
      this.render.dispose();
      this.copy.dispose();
    }
  }
}
