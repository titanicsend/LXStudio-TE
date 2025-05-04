package titanicsend.pattern.glengine;

import static com.jogamp.opengl.GL.*;

import Jama.Matrix;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import heronarts.lx.LX;
import heronarts.lx.model.LXModel;
import heronarts.lx.parameter.LXParameter;
import java.io.File;
import java.nio.*;
import java.util.*;
import java.util.stream.Collectors;
import titanicsend.pattern.yoffa.shader_engine.*;
import titanicsend.util.TE;

/**
 * Everything to run a complete shader program with geometry, textures, uniforms, vertex and
 * fragment shaders.
 */
public class GLShader {

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
  private static final int[] INDICES = {
    0, 1, 2,
    2, 0, 3
  };

  // local copies of frequently used OpenGL objects
  private final GLEngine glEngine;
  private GLAutoDrawable canvas = null;
  private GL4 gl4 = null;

  private final FragmentShader fragmentShader;
  private final int width;
  private final int height;

  private ShaderProgram shaderProgram;

  // geometry buffers
  private final FloatBuffer vertexBuffer;
  private final IntBuffer indexBuffer;
  private final int[] geometryBufferHandles = new int[2];

  // texture buffers
  private class TextureInfo {
    public String name;
    public int channel;
    public int textureUnit;
    public int uniformLocation;
  }

  private boolean useMappedBuffer = false;

  private final ArrayList<TextureInfo> textures = new ArrayList<>();
  private final ByteBuffer backBuffer;
  private final int[] backbufferHandle = new int[1];

  // support for optional texture mapped (as opposed to the new linear format) buffer
  // containing the last rendered frame.
  private ByteBuffer mappedBuffer = null;
  private final int[] mappedBufferHandle = new int[1];
  private int mappedBufferUnit = -1;
  private int mappedBufferWidth = 640;
  private int mappedBufferHeight = 640;

  // the GL texture unit to which the current view model coordinate
  // texture is bound.
  private int modelCoordsTextureUnit = -1;

  // map of user created uniforms.
  protected final HashMap<String, Uniform> uniforms = new HashMap<>();

  // maps uniform names to GL texture units
  protected final HashMap<String, Integer> uniformTextureUnits = new HashMap<>();

  // list of LX control parameters from the shader code
  private final List<LXParameter> parameters = new ArrayList<>();

  // Callbacks used by the pattern or effect that owns the shader to set any custom uniforms.
  // Usually, these are the parameters associated with the TECommonControls.
  private final List<UniformSource> uniformSources = new ArrayList<>();

  private boolean initialized = false;

  // get the active GL profile so the calling entity can work with
  // GL textures and buffers if necessary.  (NDI support
  // requires this, for example.)
  public GLProfile getGLProfile() {
    return gl4.getGLProfile();
  }

  // get the active GL4 object for this shader.
  public GL4 getGL4() {
    return gl4;
  }

  // Welcome to the Land of 1000 Constructors!

  /**
   * Create new OpenGL shader effect
   *
   * @param lx LX instance
   * @param fragmentShader fragment shader object to use
   * @param frameBuf native (GL compatible) ByteBuffer to store render results for use in shaders
   *     that need to read the previous frame. If null, a buffer will be automatically allocated.
   * @param uniformSource callback that will set uniforms on this shader
   */
  public GLShader(
      LX lx, FragmentShader fragmentShader, ByteBuffer frameBuf, UniformSource uniformSource) {
    this(lx, fragmentShader, frameBuf, List.of(uniformSource));
  }

  /**
   * Create new OpenGL shader effect
   *
   * @param lx LX instance
   * @param fragmentShader fragment shader object to use
   * @param frameBuf native (GL compatible) ByteBuffer to store render results for use in shaders
   *     that need to read the previous frame. If null, a buffer will be automatically allocated.
   * @param uniformSources list of callbacks that will set uniforms on this shader
   */
  public GLShader(
      LX lx,
      FragmentShader fragmentShader,
      ByteBuffer frameBuf,
      List<UniformSource> uniformSources) {

    this.glEngine = (GLEngine) lx.engine.getChild(GLEngine.PATH);
    this.width = this.glEngine.getWidth();
    this.height = this.glEngine.getHeight();

    // initialization that can be done before the OpenGL context is available
    this.fragmentShader = Objects.requireNonNull(fragmentShader);
    this.parameters.addAll(fragmentShader.getParameters());
    this.vertexBuffer = Buffers.newDirectFloatBuffer(VERTICES.length);
    this.indexBuffer = Buffers.newDirectIntBuffer(INDICES.length);
    this.vertexBuffer.put(VERTICES);
    this.indexBuffer.put(INDICES);
    // allocate default buffer for reading offscreen surface to cpu memory
    this.backBuffer = frameBuf != null ? frameBuf : allocateBackBuffer();

    for (UniformSource uniformSource : uniformSources) {
      if (uniformSource != null) {
        addUniformSource(uniformSource);
      }
    }
  }

  /**
   * Create new shader object with default backbuffer
   *
   * @param lx LX instance
   * @param fragmentShader fragment shader object shader to use
   * @param uniformSource callback that will set uniforms on this shader
   */
  public GLShader(LX lx, FragmentShader fragmentShader, UniformSource uniformSource) {
    this(lx, fragmentShader, null, uniformSource);
  }

  /**
   * Creates new shader object with additional texture support
   *
   * @param lx LX instance
   * @param shaderFilename shader to use
   * @param frameBuf native (GL compatible) ByteBuffer to store render results for use in shaders
   *     that need to read the previous frame. If null, a buffer will be automatically allocated. *
   * @param uniformSource callback that will set uniforms on this shader
   * @param textureFilenames (optional) texture files to load
   */
  public GLShader(
      LX lx,
      String shaderFilename,
      UniformSource uniformSource,
      ByteBuffer frameBuf,
      String... textureFilenames) {
    this(lx, shaderFilename, List.of(uniformSource), frameBuf, textureFilenames);
  }

  /**
   * Creates new shader object with additional texture support
   *
   * @param lx LX instance
   * @param shaderFilename shader to use
   * @param frameBuf native (GL compatible) ByteBuffer to store render results for use in shaders
   *     that need to read the previous frame. If null, a buffer will be automatically allocated. *
   * @param uniformSources list of callbacks that will set uniforms on this shader
   * @param textureFilenames (optional) texture files to load
   */
  public GLShader(
      LX lx,
      String shaderFilename,
      List<UniformSource> uniformSources,
      ByteBuffer frameBuf,
      String... textureFilenames) {
    this(
        lx,
        new FragmentShader(
            new File("resources/shaders/" + shaderFilename),
            Arrays.stream(textureFilenames)
                .map(x -> new File("resources/shaders/textures/" + x))
                .collect(Collectors.toList())),
        frameBuf,
        uniformSources);
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
    this(lx, shaderFilename, uniformSource, null, textureFilenames);
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
    this(lx, shaderFilename, uniformSources, null, textureFilenames);
  }

  protected GLShader addUniformSource(UniformSource uniformSource) {
    if (uniformSource == null) {
      throw new IllegalArgumentException("UniformSource cannot be null");
    }
    this.uniformSources.add(uniformSource);
    return this;
  }

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

  /**
   * Set the buffer to be used as a rectangular texture backbuffer for this shader. (As opposed to
   * iBackbuffer, which is a linear list of colors corresponding to LX 3D model points and can't be
   * used for algorithms that need the ability to access neighboring pixels.) NOTE: MUST BE CALLED
   * BEFORE THE SHADER IS INITIALIZED. (i.e. in the pattern's constructor.) TODO - at present, this
   * buffer is only used by shader effects. It should eventually TODO - be optional for shader
   * patterns as well.
   *
   * @param buffer previously allocated ByteBuffer of sufficient size to hold the desired texture
   */
  public void setMappedBuffer(ByteBuffer buffer, int width, int height) {
    this.mappedBufferWidth = width;
    this.mappedBufferHeight = height;
    this.mappedBuffer = buffer;
    this.useMappedBuffer = true;
  }

  /** Activate this shader for rendering in the current context */
  public void useProgram() {
    gl4.glUseProgram(shaderProgram.getProgramId());
  }

  public boolean isInitialized() {
    return this.initialized;
  }

  /** Shader initialization that requires the OpenGL context. Call once. */
  public void init() {
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
    loadTextureFiles();
  }

  /**
   * Copy LXPoints' normalized coordinates into textures for use by shaders. Must be called by the
   * parent pattern or effect at least once before the first frame is rendered. And should be called
   * by the pattern's frametime run() function if the model has changed since the last frame.
   */
  public void useViewCoordinates(LXModel model) {
    this.modelCoordsTextureUnit = glEngine.getCoordinatesTexture(model);
  }

  // Releases native resources allocated by this shader.
  // Should be called by the pattern's dispose() function
  // when the pattern is unloaded. (Not when just
  // deactivated.)
  public void dispose() {
    // if we've been fully initialized, we need to release all
    // OpenGL GPU resources we've allocated.
    if (this.initialized) {

      // delete GPU buffers we directly allocated
      gl4.glDeleteBuffers(2, geometryBufferHandles, 0);
      gl4.glDeleteTextures(1, backbufferHandle, 0);

      if (useMappedBuffer) {
        gl4.glDeleteTextures(1, mappedBufferHandle, 0);
        glEngine.releaseTextureUnit(mappedBufferUnit);
      }

      // free any textures on ShaderToy channels
      for (TextureInfo ti : textures) {
        glEngine.releaseTexture(ti.name);
      }
      this.shaderProgram.dispose();
    }
  }

  public void run() {
    canvas.getContext().makeCurrent();
    render();
    saveSnapshot();
  }

  private void saveSnapshot() {
    backBuffer.rewind();
    gl4.glReadBuffer(GL_BACK);

    // using BGRA byte order lets us read int values from the buffer and pass them
    // directly to LX as colors, without any additional work on the Java side.
    gl4.glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_BYTE, backBuffer);
  }

  /**
   * Called at pattern initialization time to allocate and configure GPU buffers that are common to
   * all shaders.
   */
  private void allocateShaderBuffers() {
    // storage for geometry buffer handles
    gl4.glGenBuffers(2, IntBuffer.wrap(geometryBufferHandles));

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

    // backbuffer texture object
    gl4.glActiveTexture(GL_TEXTURE2);
    gl4.glEnable(GL_TEXTURE_2D);
    gl4.glGenTextures(1, backbufferHandle, 0);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, backbufferHandle[0]);

    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    gl4.glBindTexture(GL_TEXTURE_2D, 0);

    // create mapped buffer texture object, if needed
    if (useMappedBuffer) {
      this.mappedBufferUnit = glEngine.getNextTextureUnit();

      gl4.glActiveTexture(GL_TEXTURE0 + mappedBufferUnit);
      gl4.glEnable(GL_TEXTURE_2D);
      gl4.glGenTextures(1, mappedBufferHandle, 0);
      gl4.glBindTexture(GL4.GL_TEXTURE_2D, mappedBufferHandle[0]);

      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

      gl4.glBindTexture(GL_TEXTURE_2D, 0);
    }

    // assign shared uniform blocks to the shader's binding points
    int perRunBlockIndex = gl4.glGetUniformBlockIndex(shaderProgram.getProgramId(), "PerRunBlock");
    gl4.glUniformBlockBinding(
        shaderProgram.getProgramId(), perRunBlockIndex, GLEngine.perRunUniformBlockBinding);

    int perFrameBlockIndex =
        gl4.glGetUniformBlockIndex(shaderProgram.getProgramId(), "PerFrameBlock");
    gl4.glUniformBlockBinding(
        shaderProgram.getProgramId(), perFrameBlockIndex, GLEngine.perFrameUniformBlockBinding);
  }

  /** Set up geometry at frame generation time */
  private void render() {
    // set uniforms
    setUniforms();

    // set up geometry
    int position = shaderProgram.getShaderAttributeLocation(ShaderAttribute.POSITION);

    // TODO(jkb): move these lines to init, just use glBindVertexArray() in render loop?
    gl4.glBindBuffer(GL_ARRAY_BUFFER, geometryBufferHandles[0]);
    gl4.glVertexAttribPointer(position, 3, GL4.GL_FLOAT, false, 0, 0);
    gl4.glEnableVertexAttribArray(position);
    gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, geometryBufferHandles[1]);

    // render a frame
    gl4.glDrawElements(GL2.GL_TRIANGLES, INDICES.length, GL2.GL_UNSIGNED_INT, 0);
    gl4.glDisableVertexAttribArray(position);
  }

  private void setUniforms() {

    // set uniforms for the pattern or effect controls this shader uses
    for (UniformSource uniformSource : this.uniformSources) {
      uniformSource.setUniforms(this);
    }

    // Add all preprocessed LX parameters from the shader code as uniforms
    for (LXParameter customParameter : fragmentShader.getParameters()) {
      setUniform(customParameter.getLabel() + Uniforms.CUSTOM_SUFFIX, customParameter.getValuef());
    }

    // Set audio waveform and fft data as a 512x2 texture on the specified audio
    // channel if it's a shadertoy shader, or iChannel0 if it's a local shader.

    // By Imperial Decree, the audio texture will heretofore always use texture unit
    // GL_TEXTURE0, the model coordinate array will use GL_TEXTURE1 and the backbuffer
    // texture will use GL_TEXTURE2. Other (shader-specific) textures will be automatically
    // bound to sequential ids starting with GL_TEXTURE3.
    //
    // The audio texture can be used by all shaders, and stays bound to texture
    // unit 0 throughout the Chromatik run. All we have to do to use it is add the uniform.
    setUniform(Uniforms.AUDIO_CHANNEL, 0);

    // use the current view's model coordinates texture which
    // has already been loaded to the GPU by the texture cache manager.
    // All we need to do is point at the right GL texture unit.
    setUniform("lxModelCoords", modelCoordsTextureUnit);

    // Update backbuffer texture data. This buffer contains the result of the
    // previous render pass.  It is always bound to texture unit 2.
    gl4.glActiveTexture(GL_TEXTURE2);
    gl4.glEnable(GL_TEXTURE_2D);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, backbufferHandle[0]);

    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D,
        0,
        GL4.GL_RGBA,
        width,
        height,
        0,
        GL4.GL_BGRA,
        GL_UNSIGNED_BYTE,
        backBuffer);

    setUniform("iBackbuffer", 2);

    // if necessary, update the mapped buffer texture data
    if (useMappedBuffer) {
      gl4.glActiveTexture(GL_TEXTURE0 + mappedBufferUnit);
      gl4.glEnable(GL_TEXTURE_2D);
      gl4.glBindTexture(GL4.GL_TEXTURE_2D, mappedBufferHandle[0]);

      gl4.glTexImage2D(
          GL4.GL_TEXTURE_2D,
          0,
          GL4.GL_RGBA,
          mappedBufferWidth,
          mappedBufferHeight,
          0,
          GL4.GL_BGRA,
          GL_UNSIGNED_BYTE,
          mappedBuffer);

      setUniform("iMappedBuffer", mappedBufferUnit);
    }

    // add shadertoy texture channels. These textures already statically bound to
    // texture units so all we have to do is tell the shader which texture unit to use.
    for (TextureInfo ti : textures) {
      setUniform(Uniforms.CHANNEL + ti.channel, ti.textureUnit);
      gl4.glUniform1i(ti.uniformLocation, ti.textureUnit);
    }

    // hand the complete uniform list to OpenGL
    updateUniforms(gl4);
  }

  private void initShaderProgram() {
    this.shaderProgram = new ShaderProgram(gl4, fragmentShader.getShaderName());
  }

  private void loadTextureFiles() {
    for (Map.Entry<Integer, String> textureInput :
        fragmentShader.getChannelToTexture().entrySet()) {

      TextureInfo ti = new TextureInfo();
      ti.textureUnit = glEngine.useTexture(gl4, textureInput.getValue());
      ti.name = textureInput.getValue();
      ti.channel = textureInput.getKey();
      ti.uniformLocation =
          gl4.glGetUniformLocation(shaderProgram.getProgramId(), "iChannel" + ti.channel);

      textures.add(ti);
    }
  }

  public ByteBuffer getImageBuffer() {
    return backBuffer;
  }

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

  public List<ShaderConfiguration> getShaderConfig() {
    return fragmentShader.getShaderConfig();
  }

  public List<LXParameter> getParameters() {
    return parameters;
  }

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

  // worker for adding user specified uniforms to our big list'o'uniforms
  protected void addUniform(String name, UniformType type, Object value) {
    // The first instance of a uniform wins. Subsequent
    // attempts to (re)set it are ignored.  This makes it so control uniforms
    // can be set from user code without being overridden by the automatic
    // setter, which is called right before frame generation.
    // TODO - we'll have to be more sophisticated about this when we start retaining textures
    // TODO - and other large, invariant uniforms between frames.
    if (!uniforms.containsKey(name)) {
      uniforms.put(name, new Uniform(type, value));
    }
  }

  // get a texture id for a uniform either from uniformTextureUnits or by allocating a new one
  protected int getTextureUnit(String name) {
    if (uniformTextureUnits.containsKey(name)) {
      return uniformTextureUnits.get(name);
    } else {
      int unit = glEngine.getNextTextureUnit();
      uniformTextureUnits.put(name, unit);
      return unit;
    }
  }

  // parse uniform list and create necessary GL objects
  // Note that array buffers passed in must be allocated to the exact appropriate size
  // you want. No allocating a big buffer, then partly filling it. GL is picky about this.
  protected void updateUniforms(GL4 gl4) {
    int[] v;
    float[] vf;
    IntBuffer vIArray;
    FloatBuffer vFArray;

    for (Map.Entry<String, Uniform> entry : uniforms.entrySet()) {
      String name = entry.getKey();
      Uniform uniform = entry.getValue();

      int loc = gl4.glGetUniformLocation(shaderProgram.getProgramId(), name);
      if (loc == -1) {
        // LX.log("No uniform \"" + name + "\"  found in shader");
        continue;
      }

      switch (uniform.type) {
        case INT1:
          v = ((int[]) uniform.value);
          gl4.glUniform1i(loc, v[0]);
          break;
        case INT2:
          v = ((int[]) uniform.value);
          gl4.glUniform2i(loc, v[0], v[1]);
          break;
        case INT3:
          v = ((int[]) uniform.value);
          gl4.glUniform3i(loc, v[0], v[1], v[2]);
          break;
        case INT4:
          v = ((int[]) uniform.value);
          gl4.glUniform4i(loc, v[0], v[1], v[2], v[3]);
          break;
        case FLOAT1:
          vf = ((float[]) uniform.value);
          gl4.glUniform1f(loc, vf[0]);
          break;
        case FLOAT2:
          vf = ((float[]) uniform.value);
          gl4.glUniform2f(loc, vf[0], vf[1]);
          break;
        case FLOAT3:
          vf = ((float[]) uniform.value);
          gl4.glUniform3f(loc, vf[0], vf[1], vf[2]);
          break;
        case FLOAT4:
          vf = ((float[]) uniform.value);
          gl4.glUniform4f(loc, vf[0], vf[1], vf[2], vf[3]);
          break;
        case INT1VEC:
          vIArray = ((IntBuffer) uniform.value);
          gl4.glUniform1iv(loc, vIArray.capacity(), vIArray);
          break;
        case INT2VEC:
          vIArray = ((IntBuffer) uniform.value);
          gl4.glUniform2iv(loc, vIArray.capacity() / 2, vIArray);
          break;
        case INT3VEC:
          vIArray = ((IntBuffer) uniform.value);
          gl4.glUniform3iv(loc, vIArray.capacity() / 3, vIArray);
          break;
        case INT4VEC:
          vIArray = ((IntBuffer) uniform.value);
          gl4.glUniform4iv(loc, vIArray.capacity() / 4, vIArray);
          break;
        case FLOAT1VEC:
          vFArray = ((FloatBuffer) uniform.value);
          gl4.glUniform1fv(loc, vFArray.capacity(), vFArray);
          break;
        case FLOAT2VEC:
          vFArray = ((FloatBuffer) uniform.value);
          gl4.glUniform2fv(loc, vFArray.capacity() / 2, vFArray);
          break;
        case FLOAT3VEC:
          vFArray = ((FloatBuffer) uniform.value);
          gl4.glUniform3fv(loc, vFArray.capacity() / 3, vFArray);
          break;
        case FLOAT4VEC:
          vFArray = ((FloatBuffer) uniform.value);
          gl4.glUniform4fv(loc, vFArray.capacity() / 4, vFArray);
          break;
        case MAT2:
          vFArray = ((FloatBuffer) uniform.value);
          gl4.glUniformMatrix2fv(loc, 1, true, vFArray);
          break;
        case MAT3:
          vFArray = ((FloatBuffer) uniform.value);
          gl4.glUniformMatrix3fv(loc, 1, true, vFArray);
          break;
        case MAT4:
          vFArray = ((FloatBuffer) uniform.value);
          gl4.glUniformMatrix4fv(loc, 1, true, vFArray);
          break;
        case SAMPLER2DSTATIC:
        case SAMPLER2D:
          Texture tex = ((Texture) uniform.value);
          int unit = getTextureUnit(name);
          gl4.glActiveTexture(GL_TEXTURE0 + unit);
          tex.enable(gl4);
          tex.bind(gl4);
          gl4.glUniform1i(loc, unit);
          break;
        default:
          LX.log("Unsupported uniform type");
          break;
      }
    }
    uniforms.clear();
  }

  /** setter -- single int */
  public void setUniform(String name, int x) {
    addUniform(name, UniformType.INT1, new int[] {x});
  }

  /** 2 element int array or ivec2 */
  public void setUniform(String name, int x, int y) {
    addUniform(name, UniformType.INT2, new int[] {x, y});
  }

  /** 3 element int array or ivec3 */
  public void setUniform(String name, int x, int y, int z) {
    addUniform(name, UniformType.INT3, new int[] {x, y, z});
  }

  /** 4 element int array or ivec4 */
  public void setUniform(String name, int x, int y, int z, int w) {
    addUniform(name, UniformType.INT4, new int[] {x, y, z, w});
  }

  /** single float */
  public void setUniform(String name, float x) {
    addUniform(name, UniformType.FLOAT1, new float[] {x});
  }

  /** 2 element float array or vec2 */
  public void setUniform(String name, float x, float y) {
    addUniform(name, UniformType.FLOAT2, new float[] {x, y});
  }

  /** 3 element float array or vec3 */
  public void setUniform(String name, float x, float y, float z) {
    addUniform(name, UniformType.FLOAT3, new float[] {x, y, z});
  }

  /** 4 element float array or vec4 */
  public void setUniform(String name, float x, float y, float z, float w) {
    addUniform(name, UniformType.FLOAT4, new float[] {x, y, z, w});
  }

  public void setUniform(String name, boolean x) {
    addUniform(name, UniformType.INT1, new int[] {(x) ? 1 : 0});
  }

  public void setUniform(String name, boolean x, boolean y) {
    addUniform(name, UniformType.INT2, new int[] {(x) ? 1 : 0, (y) ? 1 : 0});
  }

  /**
   * Create SAMPLER2D uniform from jogl Texture object - this prototype supports both dynamic and
   * static textures. If you know your texture will be changed on every frame, use setUniform(String
   * name, Texture tex) instead. TODO - STATIC TEXTURES NOT YET IMPLEMENTED
   */
  public void setUniform(String name, Texture tex, boolean isStatic) {
    addUniform(name, UniformType.SAMPLER2D, tex);
  }

  /**
   * Create SAMPLER2D uniform from jogl Texture object - for dynamic textures (that change every
   * frame). For static textures, use setUniform(String name, Texture tex,boolean isStatic) instead.
   * TODO - static/dynamic textures not yet implemented. All textures are treated TODO - as dynamic
   * and reloaded on every frame.
   */
  public void setUniform(String name, Texture tex) {
    addUniform(name, UniformType.SAMPLER2D, tex);
  }

  /**
   * @param columns number of coordinates per element, max 4
   */
  public void setUniform(String name, IntBuffer vec, int columns) {
    switch (columns) {
      case 1:
        addUniform(name, UniformType.INT1VEC, vec);
        break;
      case 2:
        addUniform(name, UniformType.INT2VEC, vec);
        break;
      case 3:
        addUniform(name, UniformType.INT3VEC, vec);
        break;
      case 4:
        addUniform(name, UniformType.INT4VEC, vec);
        break;
      default:
        // TE.log("SetUniform(%s): %d coords specified, maximum 4 allowed", name, columns);
        break;
    }
  }

  public void setUniform(String name, FloatBuffer vec, int columns) {
    switch (columns) {
      case 1:
        addUniform(name, UniformType.FLOAT1VEC, vec);
        break;
      case 2:
        addUniform(name, UniformType.FLOAT2VEC, vec);
        break;
      case 3:
        addUniform(name, UniformType.FLOAT3VEC, vec);
        break;
      case 4:
        addUniform(name, UniformType.FLOAT4VEC, vec);
        break;
      default:
        // TE.log("SetUniform(%s): %d coords specified, maximum 4 allowed", name, columns);
        break;
    }
  }

  /**
   * Internal - Creates a uniform for a square floating point matrix, of size 2x2, 3x3 or 4x4
   *
   * @param name of uniform
   * @param vec Floating point matrix data, in row major order
   * @param sz Size of matrix (Number of rows & columns. 2,3 or 4)
   */
  public void setUniformMatrix(String name, FloatBuffer vec, int sz) {
    switch (sz) {
      case 2:
        addUniform(name, UniformType.MAT2, vec);
        break;
      case 3:
        addUniform(name, UniformType.MAT3, vec);
        break;
      case 4:
        addUniform(name, UniformType.MAT4, vec);
        break;
      default:
        // TE.log("SetUniformMatrix(%s): %d incorrect matrix size specified", name, columns);
        break;
    }
  }

  /**
   * Sets a uniform for a square floating point matrix, of type MAT2(2x2), MAT3(3x3) or MAT4(4x4).
   * Input matrix must be in row major order.
   */
  public void setUniform(String name, float[][] matrix) {
    // requires a square matrix of dimension 2x2,3x3 or 4x4
    int dim = matrix.length;
    if (dim < 2 || dim > 4) {
      TE.log("SetUniform(%s): %d incorrect matrix size specified", name, dim);
      return;
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
    setUniformMatrix(name, buf, dim);
  }

  /**
   * Sets a uniform for a square floating point matrix, of type MAT2(2x2), MAT3(3x3) or MAT4(4x4)
   */
  public void setUniform(String name, Matrix matrix) {
    // requires a square matrix of dimension 2x2,3x3 or 4x4
    int dim = matrix.getRowDimension();
    if (dim < 2 || dim > 4 || dim != matrix.getColumnDimension()) {
      TE.log("SetUniform(%s): %d invalid matrix dimension specified.", name);
      return;
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
    setUniformMatrix(name, buf, dim);
  }
}
