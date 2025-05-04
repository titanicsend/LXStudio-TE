package titanicsend.pattern.yoffa.shader_engine;

import static com.jogamp.opengl.GL.*;

import Jama.Matrix;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.LXParameter;
import java.io.File;
import java.io.IOException;
import java.nio.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Technically we don't need to implement GLEventListener unless we plan on rendering on screen,
// but let's leave it for good practice.
public class NativeShader implements GLEventListener {

  // we need to draw an object with a vertex shader to put our fragment shader on
  // literally just create a rectangle that takes up the whole screen to paint on
  private static final float[] VERTICES = {
    1.0f, 1.0f, 0.0f,
    1.0f, -1.0f, 0.0f,
    -1.0f, -1.0f, 0.0f,
    -1.0f, 1.0f, 0.0f
  };

  // we are drawing with triangles, so we need two to make our rectangle
  private static final int[] INDICES = {
    0, 1, 2,
    2, 0, 3
  };

  private final FragmentShader fragmentShader;
  private final int width;
  private final int height;

  private final FloatBuffer vertexBuffer;
  private final IntBuffer indexBuffer;
  int[] geometryBufferHandles = new int[2];
  int[] audioTextureHandle = new int[1];
  private final Map<Integer, Texture> textures;
  private int textureKey;
  private ShaderProgram shaderProgram;
  ByteBuffer backBuffer;
  private PatternControlData controlData;
  private final int audioTextureWidth;
  private final int audioTextureHeight;
  FloatBuffer audioTextureData;

  private final List<Uniform> mutableUniforms = new ArrayList<>();
  public final List<Uniform> uniforms = Collections.unmodifiableList(this.mutableUniforms);
  // map of user created uniforms.
  protected final HashMap<String, Uniform> uniformMap = new HashMap<>();

  public NativeShader(FragmentShader fragmentShader, int width, int height) {
    this.width = width;
    this.height = height;
    this.fragmentShader = fragmentShader;
    this.vertexBuffer = Buffers.newDirectFloatBuffer(VERTICES.length);
    this.indexBuffer = Buffers.newDirectIntBuffer(INDICES.length);
    this.vertexBuffer.put(VERTICES);
    this.indexBuffer.put(INDICES);
    this.textures = new HashMap<>();
    this.textureKey = 1; // textureKey 0 reserved for audio texture.
    this.controlData = null;

    // gl-compatible buffer for reading offscreen surface to cpu memory
    this.backBuffer = GLBuffers.newDirectByteBuffer(width * height * 4);

    this.audioTextureWidth = 512;
    this.audioTextureHeight = 2;
    this.audioTextureData = GLBuffers.newDirectFloatBuffer(audioTextureHeight * audioTextureWidth);
  }

  private GL4 gl4;

  @Override
  public void init(GLAutoDrawable glAutoDrawable) {
    GLContext context = glAutoDrawable.getContext();
    context.makeCurrent();
    this.gl4 = glAutoDrawable.getGL().getGL4();

    if (!isInitialized()) {
      initShaderProgram(gl4);
      loadTextureFiles(fragmentShader);
      gl4.glUseProgram(shaderProgram.getProgramId());
    }
    context.release();
  }

  // needs to be called to release native resources when we dispose
  // this pattern.
  public void cleanupGLHandles(GL4 gl4) {
    gl4.glDeleteBuffers(2, geometryBufferHandles, 0);
    gl4.glDeleteTextures(1, audioTextureHandle, 0);
  }

  @Override
  public void display(GLAutoDrawable glAutoDrawable) {
    // switch to this shader's gl context
    glAutoDrawable.getContext().makeCurrent();
    GL4 gl4 = glAutoDrawable.getGL().getGL4();

    // set textureKey to first available texture object location
    // (1 because location 0 is reserved for the TE audio data texture)
    textureKey = 1;

    setUniforms(gl4);

    render(gl4);
    saveSnapshot(gl4, width, height);
  }

  private void saveSnapshot(GL4 gl4, int width, int height) {
    backBuffer.rewind();
    gl4.glReadBuffer(GL_BACK);

    // using BGRA byte order lets us read int values from the buffer and pass them
    // directly to LX as colors, without any additional work on the Java side.
    gl4.glReadPixels(0, 0, width, height, GL_BGRA, GL_UNSIGNED_BYTE, backBuffer);
  }

  /**
   * Preallocate GPU memory objects at initialization time
   *
   * @param gl4 - this pattern's GL context
   */
  private void allocateShaderBuffers(GL4 gl4) {
    // Allocate geometry buffer handles
    gl4.glGenBuffers(2, IntBuffer.wrap(geometryBufferHandles));

    // vertices
    vertexBuffer.rewind();
    gl4.glBindBuffer(GL_ARRAY_BUFFER, geometryBufferHandles[0]);
    gl4.glBufferData(
        GL_ARRAY_BUFFER,
        (long) vertexBuffer.capacity() * Float.BYTES,
        vertexBuffer,
        GL.GL_STATIC_DRAW);

    // geometry built from vertices (triangles!)
    indexBuffer.rewind();
    gl4.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, geometryBufferHandles[1]);
    gl4.glBufferData(
        GL_ELEMENT_ARRAY_BUFFER,
        (long) indexBuffer.capacity() * Integer.BYTES,
        indexBuffer,
        GL.GL_STATIC_DRAW);

    // Audio texture object - on id GL_TEXTURE0
    gl4.glActiveTexture(GL_TEXTURE0);
    gl4.glEnable(GL_TEXTURE_2D);
    gl4.glGenTextures(1, audioTextureHandle, 0);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, audioTextureHandle[0]);

    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D,
        0,
        GL4.GL_R32F,
        audioTextureWidth,
        audioTextureHeight,
        0,
        GL4.GL_RED,
        GL_FLOAT,
        audioTextureData);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
  }

  /**
   * Set up geometry at frame generation time
   *
   * @param gl4 - this pattern's GL context
   */
  private void render(GL4 gl4) {
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
    setUniform("iResolution", (float) width, (float) height);
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

  private void setUniforms(GL4 gl4) {

    // set uniforms for standard controls and audio information
    setStandardUniforms(controlData);

    // Add all preprocessed LX parameters from the shader code as uniforms
    for (LXParameter customParameter : fragmentShader.parameters) {
      setUniform(
          customParameter.getLabel() + Uniforms.LX_PARAMETER_SUFFIX, customParameter.getValuef());
    }

    // Set audio waveform and fft data as a 512x2 texture on the specified audio
    // channel if it's a shadertoy shader, or iChannel0 if it's a local shader.
    //
    // NOTE:  For improved performance the audio texture uniform, which must be
    // copied from the engine on every frame, bypasses the normal setUniform() mechanism.
    //
    // By Imperial Decree, the audio texture will heretofore always use the first texture
    // object slot, TextureId(GL_TEXTURE0).  Other texture uniforms will be automatically
    // assigned sequential ids starting with GL_TEXTURE5.
    //
    gl4.glActiveTexture(GL_TEXTURE0);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, audioTextureHandle[0]);

    // load frequency and waveform data into our texture, fft data in the first row,
    // normalized audio waveform data in the second.
    for (int n = 0; n < audioTextureWidth; n++) {
      audioTextureData.put(n, controlData.getFrequencyData(n));
      audioTextureData.put(n + audioTextureWidth, controlData.getWaveformData(n));
    }

    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D,
        0,
        GL4.GL_R32F,
        audioTextureWidth,
        audioTextureHeight,
        0,
        GL4.GL_RED,
        GL_FLOAT,
        audioTextureData);
    setUniform(Uniforms.AUDIO_CHANNEL, 0);

    // add shadertoy texture channels
    for (Map.Entry<Integer, Texture> textureInput : textures.entrySet()) {
      setUniform(Uniforms.CHANNEL + textureInput.getKey(), textureInput.getValue(), true);
    }

    // hand the complete uniform list to OpenGL
    updateUniforms();
  }

  private void initShaderProgram(GL4 gl4) {
    shaderProgram = new ShaderProgram(gl4, fragmentShader.getShaderName());

    allocateShaderBuffers(gl4);
  }

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

  @Override
  public void dispose(GLAutoDrawable glAutoDrawable) {
    GL4 gl4 = glAutoDrawable.getGL().getGL4();
    cleanupGLHandles(gl4);
    shaderProgram.dispose();
  }

  @Override
  public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) {
    // do nothing
  }

  public ByteBuffer getSnapshot() {
    return backBuffer;
  }

  public void updateControlInfo(PatternControlData ctlData) {
    this.controlData = ctlData;
  }

  public void reset() {
    // do nothing
  }

  public boolean isInitialized() {
    return shaderProgram != null;
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

  /*  // worker for adding user specified uniforms to our big list'o'uniforms
  protected void addUniform(String name, UniformType type, Object value) {
    // Note(jkb): Child setters now get called last, so values set by user code take priority.
    // This is the same outcome but a different mechanism than the previous code.
    // TODO - we'll have to be more sophisticated about this when we start retaining textures
    // TODO - and other large, invariant uniforms between frames.
    Uniform uniform = this.uniforms.get(name);
    if (uniform == null) {
      int location = this.gl4.glGetUniformLocation(this.shaderProgram.getProgramId(), name);
      uniform = new Uniform(location, type);
      this.uniforms.put(name, uniform);
    }

    // Stage the new uniform value and mark it as modified
    uniform.set(value);
  }*/

  // parse uniform list and create necessary GL objects
  // Note that array buffers passed in must be allocated to the exact appropriate size
  // you want. No allocating a big buffer, then partly filling it. GL is picky about this.
  private void updateUniforms() {
    for (Uniform uniform : this.uniforms) {
      if (uniform.hasUpdate()) {
        uniform.update();
      }
    }
  }

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

  public Uniform getUniform(String name, UniformType type) {
    Uniform uniform = this.uniformMap.get(name);
    if (uniform == null) {
      // First time accessing this uniform, create a new object.
      int location = this.gl4.glGetUniformLocation(this.shaderProgram.getProgramId(), name);
      // Special handling for Sampler2D, textureUnit will be set once in the constructor.
      // if (type == UniformType.SAMPLER2D || type == UniformType.SAMPLER2DSTATIC) {
      //   TODO: Looks like this class didn't handle texture units the same way as GLShader...
      //   uniform = Uniform.create(this.gl4, name, location, type, getTextureUnit(name));
      // } else {
      uniform = Uniform.create(this.gl4, name, location, type);
      // }
      this.uniformMap.put(name, uniform);
      this.mutableUniforms.add(uniform);
    }
    // else {
    //   We could double-check the type here, but let's keep it fast for a tight loop.
    //   If the type doesn't match it will blow up somewhere else.
    // }
    return uniform;
  }
}
