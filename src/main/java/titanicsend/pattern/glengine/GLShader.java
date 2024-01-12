package titanicsend.pattern.glengine;

import static com.jogamp.opengl.GL.*;
import static titanicsend.pattern.yoffa.shader_engine.UniformTypes.*;

import Jama.Matrix;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import java.io.File;
import java.io.IOException;
import java.nio.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.shader_engine.*;
import titanicsend.util.TE;

/**
 * Everything to run a complete shader program with geometry, textures, uniforms, vertex and
 * fragment shaders.
 */
public class GLShader {

  // Vertices for default geometry - a rectangle that covers the entire canvas
  private static final float[] VERTICES = {
    1.0f, 1.0f, 0.0f,
    1.0f, -1.0f, 0.0f,
    -1.0f, -1.0f, 0.0f,
    -1.0f, 1.0f, 0.0f
  };

  // Vertex index list for default geometry. we are drawing with triangles,
  // so we need two to make our rectangle
  private static final int[] INDICES = {
    0, 1, 2,
    2, 0, 3
  };

  // local copies of frequently used OpenGL objects
  private GLEngine glEngine = null;
  private GLAutoDrawable canvas = null;
  private GL4 gl4 = null;

  private FragmentShader fragmentShader;
  private int xResolution;
  private int yResolution;

  private ShaderProgram shaderProgram;

  private List<LXParameter> parameters;

  private TEPerformancePattern pattern;
  private PatternControlData controlData;

  // geometry buffers
  private FloatBuffer vertexBuffer;
  private IntBuffer indexBuffer;
  int[] geometryBufferHandles = new int[2];

  // texture buffers
  private Map<Integer, Texture> textures;
  private int textureKey;
  ByteBuffer backBuffer;
  int[] backbufferHandle = new int[1];

  // map of user created uniforms.
  protected HashMap<String, UniformTypes> uniforms = null;

  /**
   * Create new native shader effect
   *
   * @param lx LX instance
   * @param fragmentShader fragment shader object shader to use
   * @param frameBuf native (GL compatible) ByteBuffer to store render results for use in shaders
   *     that need to read the previous frame. If null, a buffer will be automatically allocated.
   * @param pattern Pattern associated w/this shader *
   */
  public GLShader(
      LX lx, FragmentShader fragmentShader, ByteBuffer frameBuf, TEPerformancePattern pattern) {
    this.pattern = pattern;
    this.controlData = new PatternControlData(pattern);
    this.backBuffer = frameBuf;

    if (glEngine == null) {
      this.glEngine = (GLEngine) lx.engine.getChild(GLEngine.PATH);
      // TE.log("Shader: Retrieved GLEngine object from LX");
    }

    if (fragmentShader != null) {
      this.fragmentShader = fragmentShader;
      this.parameters = fragmentShader.getParameters();
      createShaderProgram(fragmentShader, GLEngine.getWidth(), GLEngine.getHeight());
    } else {
      this.parameters = null;
    }
  }

  /**
   * Create new native shader effect with default backbuffer
   *
   * @param lx LX instance
   * @param fragmentShader fragment shader object shader to use
   * @param pattern Pattern associated w/this shader *
   */
  public GLShader(LX lx, FragmentShader fragmentShader, TEPerformancePattern pattern) {
    this(lx, fragmentShader, null, pattern);
  }

  /**
   * Creates new native shader effect with additional texture support
   *
   * @param lx LX instance
   * @param shaderFilename shader to use
   * @param frameBuf native (GL compatible) ByteBuffer to store render results for use in shaders
   *     that need to read the previous frame. If null, a buffer will be automatically allocated. *
   * @param pattern Pattern associated w/this shader
   * @param textureFilenames (optional) texture files to load
   */
  public GLShader(
      LX lx,
      String shaderFilename,
      TEPerformancePattern pattern,
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
        pattern);
  }

  /**
   * Creates new native shader effect with additional texture support
   *
   * @param lx LX instance
   * @param shaderFilename shader to use
   * @param pattern Pattern associated w/this shader
   * @param textureFilenames (optional) texture files to load
   */
  public GLShader(
      LX lx, String shaderFilename, TEPerformancePattern pattern, String... textureFilenames) {
    this(lx, shaderFilename, pattern, null, textureFilenames);
  }

  /**
   * Create appropriately sized gl-compatible buffer for reading offscreen surface to cpu memory.
   * This is the preferred way to allocate the backbuffer if your pattern runs multiple shaders
   * which need to share the same buffer.
   * @return ByteBuffer
   */
  public static ByteBuffer allocateBackBuffer() {
    return GLBuffers.newDirectByteBuffer(GLEngine.getWidth() * GLEngine.getHeight() * 4);
  }

  /**
   Activate this shader for rendering in the current context
   */
  public void useProgram() {
    gl4.glUseProgram(shaderProgram.getProgramId());
  }

  public void createShaderProgram(FragmentShader fragmentShader, int xResolution, int yResolution) {
    this.xResolution = xResolution;
    this.yResolution = yResolution;
    this.fragmentShader = fragmentShader;
    this.vertexBuffer = Buffers.newDirectFloatBuffer(VERTICES.length);
    this.indexBuffer = Buffers.newDirectIntBuffer(INDICES.length);
    this.vertexBuffer.put(VERTICES);
    this.indexBuffer.put(INDICES);
    this.textures = new HashMap<>();
    this.textureKey = 2; // textureKey 0-1 reserved.

    // allocate default buffer for reading offscreen surface to cpu memory
    if (this.backBuffer == null) this.backBuffer = allocateBackBuffer();
  }

  public void init() {
    // The LX engine thread should have been initialized by now, so
    // we can safely retrieve our OpenGL canvas and context from the
    // glEngine task.
    if (canvas == null) {
      this.canvas = glEngine.getCanvas();
      this.gl4 = canvas.getGL().getGL4();
    }

    // it's likely the context is already current, but
    // let's make sure.
    canvas.getContext().makeCurrent();

    // uncomment to enable debug output
    // context.getGL().getGL4().glEnable(GL_DEBUG_OUTPUT);

    // complete the initialization of the shader program if necessary,
    // then activate it.
    if (!isInitialized()) {
      initShaderProgram(gl4);
      loadTextureFiles(fragmentShader);
    }
    gl4.glUseProgram(shaderProgram.getProgramId());
  }

  // needs to be called to release native resources when we dispose
  // this pattern.
  public void cleanupGLHandles(GL4 gl4) {
    gl4.glDeleteBuffers(2, geometryBufferHandles, 0);
    gl4.glDeleteTextures(1, backbufferHandle, 0);
  }

  public void display() {
    // switch to this shader's gl context
    canvas.getContext().makeCurrent();
    render();
    saveSnapshot();
  }

  private void saveSnapshot() {
    backBuffer.rewind();
    gl4.glReadBuffer(GL_BACK);

    // using BGRA byte order lets us read int values from the buffer and pass them
    // directly to LX as colors, without any additional work on the Java side.
    gl4.glReadPixels(0, 0, xResolution, yResolution, GL_BGRA, GL_UNSIGNED_BYTE, backBuffer);
  }

  /**
   * Called at pattern initialization time to allocate and configure GPU buffers that are common to
   * all patterns.
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
    gl4.glActiveTexture(GL_TEXTURE1);
    gl4.glEnable(GL_TEXTURE_2D);
    gl4.glGenTextures(1, backbufferHandle, 0);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, backbufferHandle[0]);

    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    // copy the current backbuffer contents into the new texture
    // TODO - do we really have to do this?
    gl4.glTexImage2D(
      GL4.GL_TEXTURE_2D,
      0,
      GL4.GL_RGBA,
      xResolution,
      yResolution,
      0,
      GL4.GL_BGRA,
      GL_UNSIGNED_BYTE,
      backBuffer);


    gl4.glBindTexture(GL_TEXTURE_2D, 0);
  }

  /** Set up geometry at frame generation time */
  private void render() {
    // set uniforms
    setUniforms();

    // set up geometry
    int position = shaderProgram.getShaderAttributeLocation(ShaderAttribute.POSITION);

    gl4.glBindBuffer(GL_ARRAY_BUFFER, geometryBufferHandles[0]);
    gl4.glVertexAttribPointer(position, 3, GL4.GL_FLOAT, false, 0, 0);
    gl4.glEnableVertexAttribArray(position);
    gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, geometryBufferHandles[1]);

    // render a frame
    gl4.glDrawElements(GL2.GL_TRIANGLES, INDICES.length, GL2.GL_UNSIGNED_INT, 0);
    gl4.glDisableVertexAttribArray(position);
  }

  private void setColorUniform(String rgbName, String hsvName, int color) {
    float x, y, z;

    x = (float) (0xff & LXColor.red(color)) / 255f;
    y = (float) (0xff & LXColor.green(color)) / 255f;
    z = (float) (0xff & LXColor.blue(color)) / 255f;
    setUniform(rgbName, x, y, z);

    x = LXColor.h(color) / 360f;
    y = LXColor.s(color) / 100f;
    z = LXColor.b(color) / 100f;
    setUniform(hsvName, x, y, z);
  }

  private void setStandardUniforms(PatternControlData ctl) {

    // set standard shadertoy-style uniforms
    setUniform("iTime", (float) ctl.getTime());
    setUniform("iResolution", (float) xResolution, (float) yResolution);
    // setUniform("iMouse", 0f, 0f, 0f, 0f);

    // TE standard audio uniforms
    setUniform("beat", (float) ctl.getBeat());
    setUniform("sinPhaseBeat", (float) ctl.getSinePhaseOnBeat());
    setUniform("bassLevel", (float) ctl.getBassLevel());
    setUniform("trebleLevel", (float) ctl.getTrebleLevel());

    // added by @look
    setUniform("bassRatio", (float) ctl.getBassRatio());
    setUniform("trebleRatio", (float) ctl.getTrebleRatio());
    setUniform("volumeRatio", ctl.getVolumeRatiof());

    // color-related uniforms
    setColorUniform("iColorRGB", "iColorHSB", ctl.calcColor());
    setColorUniform("iColor2RGB", "iColor2HSB", ctl.calcColor2());

    // uniforms for common controls
    setUniform("iSpeed", (float) ctl.getSpeed());
    setUniform("iScale", (float) ctl.getSize());
    setUniform("iQuantity", (float) ctl.getQuantity());
    setUniform("iTranslate", (float) ctl.getXPos(), (float) ctl.getYPos());
    setUniform("iSpin", (float) ctl.getSpin());
    setUniform("iRotationAngle", (float) ctl.getRotationAngleFromSpin());
    setUniform("iBrightness", (float) ctl.getBrightness());
    setUniform("iWow1", (float) ctl.getWow1());
    setUniform("iWow2", (float) ctl.getWow2());
    setUniform("iWowTrigger", ctl.getWowTrigger());
  }

  private void setUniforms() {

    // set textureKey to first available texture object location.
    // (2 because location 0 is reserved for the TE audio data texture
    //  and location 1 is reserved for a backbuffer containing the
    // previous rendered frame.)
    textureKey = 2;

    // set uniforms for standard controls and audio information
    setStandardUniforms(controlData);

    // Add all preprocessed LX parameters from the shader code as uniforms
    for (LXParameter customParameter : fragmentShader.getParameters()) {
      setUniform(customParameter.getLabel() + Uniforms.CUSTOM_SUFFIX, customParameter.getValuef());
    }

    // Set audio waveform and fft data as a 512x2 texture on the specified audio
    // channel if it's a shadertoy shader, or iChannel0 if it's a local shader.

    // By Imperial Decree, the audio texture will heretofore always use texture unit
    // GL_TEXTURE0 and the backbuffer texture will use GL_TEXTURE1. Other textures
    // will be automatically bound to sequential ids starting with GL_TEXTURE2.
    //
    // The audio texture can be used by all patterns, and stays bound to texture
    // unit 0 throughout the Chromatik run. All we have to do to use it is add the uniform.
    setUniform(Uniforms.AUDIO_CHANNEL, 0);

    // Update backbuffer texture data. This buffer contains the result of the
    // previous render pass.  It is always bound to texture unit 1.
    gl4.glActiveTexture(GL_TEXTURE1);
    gl4.glEnable(GL_TEXTURE_2D);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, backbufferHandle[0]);

    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D,
        0,
        GL4.GL_RGBA,
        xResolution,
        yResolution,
        0,
        GL4.GL_BGRA,
        GL_UNSIGNED_BYTE,
        backBuffer);

    setUniform("iBackbuffer", 1);

    // add shadertoy texture channels
    for (Map.Entry<Integer, Texture> textureInput : textures.entrySet()) {
      setUniform(Uniforms.CHANNEL + textureInput.getKey(), textureInput.getValue(), true);
    }

    // hand the complete uniform list to OpenGL
    updateUniforms(gl4);
  }

  private void initShaderProgram(GL4 gl4) {
    shaderProgram = new ShaderProgram();
    shaderProgram.init(gl4, fragmentShader.getShaderName());
    allocateShaderBuffers();
  }

  // TODO - make sure user assignable textures always start with unit 2
  private void loadTextureFiles(FragmentShader fragmentShader) {
    for (Map.Entry<Integer, String> textureInput :
        fragmentShader.getChannelToTexture().entrySet()) {
      try {
        File file = new File(textureInput.getValue());
        // TE.log("File Texture %s", textureInput.getValue());
        textures.put(textureInput.getKey(), TextureIO.newTexture(file, false));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  public void dispose(GLAutoDrawable glAutoDrawable) {
    GL4 gl4 = glAutoDrawable.getGL().getGL4();
    cleanupGLHandles(gl4);
    shaderProgram.dispose(gl4);
  }

  public ByteBuffer getSnapshot() {
    return backBuffer;
  }

  public void updateControlInfo(PatternControlData ctlData) {
    this.controlData = ctlData;
  }

  public boolean isInitialized() {
    return (shaderProgram != null) && (shaderProgram.isInitialized());
  }

  public void onActive() {
    System.out.println("GLShader.onActive");
    init();
  }

  public void onInactive() {
    System.out.println("GLShader.onInactive");
  }

  public ByteBuffer getFrame(PatternControlData ctlInfo) {
    updateControlInfo(ctlInfo);
    display();

    return getSnapshot();
  }

  /**
   * Called after a frame has been generated, this function samples the OpenGL backbuffer to set
   * color at the specified points.
   *
   * @param points list of points to paint
   * @param image backbuffer containing image for this frame
   * @param xSize x resolution of image
   * @param ySize y resolution of image
   */
  public void paint(List<LXPoint> points, ByteBuffer image, int xSize, int ySize) {
    int xMax = xSize - 1;
    int yMax = ySize - 1;
    int[] colors = pattern.getColors();

    for (LXPoint point : points) {
      float zn = (1f - point.zn);
      float yn = point.yn;

      // use normalized point coordinates to calculate x/y coordinates and then the
      // proper index in the image buffer.  the 'z' dimension of TE corresponds
      // with 'x' dimension of the image based on the side that we're painting.
      int xi = Math.round(zn * xMax);
      int yi = Math.round(yn * yMax);

      int index = 4 * ((yi * xSize) + xi);

      colors[point.index] = image.getInt(index);
    }
  }

  public void run(double deltaMs) {
    ByteBuffer image = getFrame(controlData);
    paint(this.pattern.getModel().getPoints(), image, GLEngine.getWidth(), GLEngine.getHeight());
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
  protected void addUniform(String name, int type, Object value) {
    if (uniforms == null) {
      uniforms = new HashMap<>();
    }
    // The first instance of a uniform wins. Subsequent
    // attempts to (re)set it are ignored.  This makes it so control uniforms
    // can be set from user pattern code without being overridden by the automatic
    // setter, which is called right before frame generation.
    // TODO - we'll have to be more sophisticated about this when we start retaining textures
    // TODO - and other large, invariant uniforms between frames.
    if (!uniforms.containsKey(name)) {
      uniforms.put(name, new UniformTypes(type, value));
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
    if (uniforms != null && !uniforms.isEmpty()) {
      for (String name : uniforms.keySet()) {
        int loc = gl4.glGetUniformLocation(shaderProgram.getProgramId(), name);

        if (loc == -1) {
          // LX.log("No uniform \"" + name + "\"  found in shader");
          continue;
        }
        UniformTypes val = uniforms.get(name);

        switch (val.type) {
          case INT1:
            v = ((int[]) val.value);
            gl4.glUniform1i(loc, v[0]);
            break;
          case INT2:
            v = ((int[]) val.value);
            gl4.glUniform2i(loc, v[0], v[1]);
            break;
          case INT3:
            v = ((int[]) val.value);
            gl4.glUniform3i(loc, v[0], v[1], v[2]);
            break;
          case INT4:
            v = ((int[]) val.value);
            gl4.glUniform4i(loc, v[0], v[1], v[2], v[3]);
            break;
          case FLOAT1:
            vf = ((float[]) val.value);
            gl4.glUniform1f(loc, vf[0]);
            break;
          case FLOAT2:
            vf = ((float[]) val.value);
            gl4.glUniform2f(loc, vf[0], vf[1]);
            break;
          case FLOAT3:
            vf = ((float[]) val.value);
            gl4.glUniform3f(loc, vf[0], vf[1], vf[2]);
            break;
          case FLOAT4:
            vf = ((float[]) val.value);
            gl4.glUniform4f(loc, vf[0], vf[1], vf[2], vf[3]);
            break;
          case INT1VEC:
            vIArray = ((IntBuffer) val.value);
            gl4.glUniform1iv(loc, vIArray.capacity(), vIArray);
            break;
          case INT2VEC:
            vIArray = ((IntBuffer) val.value);
            gl4.glUniform2iv(loc, vIArray.capacity() / 2, vIArray);
            break;
          case INT3VEC:
            vIArray = ((IntBuffer) val.value);
            gl4.glUniform3iv(loc, vIArray.capacity() / 3, vIArray);
            break;
          case INT4VEC:
            vIArray = ((IntBuffer) val.value);
            gl4.glUniform4iv(loc, vIArray.capacity() / 4, vIArray);
            break;
          case FLOAT1VEC:
            vFArray = ((FloatBuffer) val.value);
            gl4.glUniform1fv(loc, vFArray.capacity(), vFArray);
            break;
          case FLOAT2VEC:
            vFArray = ((FloatBuffer) val.value);
            gl4.glUniform2fv(loc, vFArray.capacity() / 2, vFArray);
            break;
          case FLOAT3VEC:
            vFArray = ((FloatBuffer) val.value);
            gl4.glUniform3fv(loc, vFArray.capacity() / 3, vFArray);
            break;
          case FLOAT4VEC:
            vFArray = ((FloatBuffer) val.value);
            gl4.glUniform4fv(loc, vFArray.capacity() / 4, vFArray);
            break;
          case MAT2:
            vFArray = ((FloatBuffer) val.value);
            gl4.glUniformMatrix2fv(loc, 1, true, vFArray);
            break;
          case MAT3:
            vFArray = ((FloatBuffer) val.value);
            gl4.glUniformMatrix3fv(loc, 1, true, vFArray);
            break;
          case MAT4:
            vFArray = ((FloatBuffer) val.value);
            gl4.glUniformMatrix4fv(loc, 1, true, vFArray);
            break;
          case SAMPLER2DSTATIC:
          case SAMPLER2D:
            Texture tex = ((Texture) val.value);
            gl4.glActiveTexture(GL_TEXTURE0 + textureKey);
            tex.enable(gl4);
            tex.bind(gl4);
            gl4.glUniform1i(loc, textureKey);
            tex.disable(gl4);
            textureKey++;
            break;
          default:
            LX.log("Unsupported uniform type");
            break;
        }
      }
      uniforms.clear();
    }
  }

  /** setter -- single int */
  public void setUniform(String name, int x) {
    addUniform(name, INT1, new int[] {x});
  }

  /** 2 element int array or ivec2 */
  public void setUniform(String name, int x, int y) {
    addUniform(name, INT2, new int[] {x, y});
  }

  /** 3 element int array or ivec3 */
  public void setUniform(String name, int x, int y, int z) {
    addUniform(name, INT3, new int[] {x, y, z});
  }

  /** 4 element int array or ivec4 */
  public void setUniform(String name, int x, int y, int z, int w) {
    addUniform(name, UniformTypes.INT4, new int[] {x, y, z, w});
  }

  /** single float */
  public void setUniform(String name, float x) {
    addUniform(name, UniformTypes.FLOAT1, new float[] {x});
  }

  /** 2 element float array or vec2 */
  public void setUniform(String name, float x, float y) {
    addUniform(name, UniformTypes.FLOAT2, new float[] {x, y});
  }

  /** 3 element float array or vec3 */
  public void setUniform(String name, float x, float y, float z) {
    addUniform(name, UniformTypes.FLOAT3, new float[] {x, y, z});
  }

  /** 4 element float array or vec4 */
  public void setUniform(String name, float x, float y, float z, float w) {
    addUniform(name, UniformTypes.FLOAT4, new float[] {x, y, z, w});
  }

  public void setUniform(String name, boolean x) {
    addUniform(name, INT1, new int[] {(x) ? 1 : 0});
  }

  public void setUniform(String name, boolean x, boolean y) {
    addUniform(name, INT2, new int[] {(x) ? 1 : 0, (y) ? 1 : 0});
  }

  /**
   * Create SAMPLER2D uniform from jogl Texture object - this prototype supports both dynamic and
   * static textures. If you know your texture will be changed on every frame, use setUniform(String
   * name, Texture tex) instead. TODO - STATIC TEXTURES NOT YET IMPLEMENTED
   */
  public void setUniform(String name, Texture tex, boolean isStatic) {
    addUniform(name, UniformTypes.SAMPLER2D, tex);
  }

  /**
   * Create SAMPLER2D uniform from jogl Texture object - for dynamic textures (that change every
   * frame). For static textures, use setUniform(String name, Texture tex,boolean isStatic) instead.
   * TODO - static/dynamic textures not yet implemented. All textures are treated TODO - as dynamic
   * and reloaded on every frame.
   */
  public void setUniform(String name, Texture tex) {
    addUniform(name, UniformTypes.SAMPLER2D, tex);
  }

  /**
   * @param columns number of coordinates per element, max 4
   */
  public void setUniform(String name, IntBuffer vec, int columns) {
    switch (columns) {
      case 1:
        addUniform(name, UniformTypes.INT1VEC, vec);
        break;
      case 2:
        addUniform(name, UniformTypes.INT2VEC, vec);
        break;
      case 3:
        addUniform(name, UniformTypes.INT3VEC, vec);
        break;
      case 4:
        addUniform(name, UniformTypes.INT4VEC, vec);
        break;
      default:
        // TE.log("SetUniform(%s): %d coords specified, maximum 4 allowed", name, columns);
        break;
    }
  }

  public void setUniform(String name, FloatBuffer vec, int columns) {
    switch (columns) {
      case 1:
        addUniform(name, UniformTypes.FLOAT1VEC, vec);
        break;
      case 2:
        addUniform(name, UniformTypes.FLOAT2VEC, vec);
        break;
      case 3:
        addUniform(name, UniformTypes.FLOAT3VEC, vec);
        break;
      case 4:
        addUniform(name, UniformTypes.FLOAT4VEC, vec);
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
        addUniform(name, UniformTypes.MAT2, vec);
        break;
      case 3:
        addUniform(name, UniformTypes.MAT3, vec);
        break;
      case 4:
        addUniform(name, UniformTypes.MAT4, vec);
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
