package titanicsend.pattern.glengine;

import static com.jogamp.opengl.GL.GL_CLAMP_TO_EDGE;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_INVALID_ENUM;
import static com.jogamp.opengl.GL.GL_INVALID_FRAMEBUFFER_OPERATION;
import static com.jogamp.opengl.GL.GL_INVALID_OPERATION;
import static com.jogamp.opengl.GL.GL_INVALID_VALUE;
import static com.jogamp.opengl.GL.GL_NEAREST;
import static com.jogamp.opengl.GL.GL_NO_ERROR;
import static com.jogamp.opengl.GL.GL_OUT_OF_MEMORY;
import static com.jogamp.opengl.GL.GL_TEXTURE0;
import static com.jogamp.opengl.GL.GL_TEXTURE_2D;
import static com.jogamp.opengl.GL.GL_TEXTURE_MAG_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_MIN_FILTER;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_S;
import static com.jogamp.opengl.GL.GL_TEXTURE_WRAP_T;
import static titanicsend.pattern.glengine.GLShader.TEXTURE_UNIT_AUDIO;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.util.GLBuffers;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import heronarts.lx.audio.GraphicMeter;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXSwatch;
import heronarts.lx.parameter.BooleanParameter;
import java.nio.FloatBuffer;
import titanicsend.audio.AudioStems;
import titanicsend.pattern.glengine.mixer.GLMixer;
import titanicsend.pattern.yoffa.shader_engine.ShaderUtils;
import titanicsend.util.TE;
import titanicsend.util.TEMath;

public class GLEngine extends LXComponent implements LXLoopTask, LX.Listener {
  public static final String PATH = "GLEngine";
  public static GLEngine current;

  public final BooleanParameter gpuJavaEffects =
      new BooleanParameter("Java Fx in GPU Mode", true)
          .setDescription("Loop Java effects in GPU mode (on master channel only)");

  private final int[] audioTextureHandle = new int[1];
  private final int[] uniformBlockHandles = new int[2];

  // rendering canvas size.  May be changed
  // via the startup command line.
  private final int width;
  private final int height;

  // audio texture size and buffer
  private static final int audioTextureWidth = 512;
  private static final int audioTextureHeight = 2;
  private FloatBuffer audioTextureData;

  // audio data sources & parameters
  private final double AUDIO_LEVEL_MIN = 0.01;
  private final GraphicMeter meter;
  private final float fftResampleFactor;

  private static final TEMath.EMA avgVolume = new TEMath.EMA(0.5, .01);
  private static final TEMath.EMA avgBass = new TEMath.EMA(0.5, .01);
  private static final TEMath.EMA avgTreble = new TEMath.EMA(0.5, .01);

  private static double beat = 0.0;
  private static double sinPhaseBeat = 0.0;
  private static double volume = 0.0;
  private static double bassLevel = 0.0;
  private static double trebleLevel = 0.0;
  private static double volumeRatio = 0.0;
  private static double bassRatio = 0.0;
  private static double trebleRatio = 0.0;

  // audio and related uniform block buffer parameters
  private static final int MAX_AUDIO_STEMS = 5;
  private FloatBuffer perRunUniformBlock;
  private int perRunUniformBlockSize;
  private FloatBuffer perFrameUniformBlock;
  private int perFrameUniformBlockSize;

  public static final int perRunUniformBlockBinding = 0;
  public static final int perFrameUniformBlockBinding = 1;

  // Texture cache management
  public final TextureManager textureCache;

  // GPU Mixer Engine
  private GLMixer mixer;

  // Data and utility methods for the GL canvas/context.
  private GLAutoDrawable canvas = null;
  private GL4 gl4;

  public GLAutoDrawable getCanvas() {
    return canvas;
  }

  public int getWidth() {
    return this.width;
  }

  public int getHeight() {
    return this.height;
  }

  // Utility methods to give java patterns access to the audio texture
  // should they want it.
  public FloatBuffer getAudioTextureBuffer() {
    return audioTextureData;
  }

  public static int getAudioTextureWidth() {
    return audioTextureWidth;
  }

  public static int getAudioTextureHeight() {
    return audioTextureHeight;
  }

  /**
   * Retrieve a single sample of the current frame's fft data from the engine NOTE: 512 samples can
   * always be retrieved, regardless of how many bands the engine actually supplies. Data will be
   * properly distributed (but not smoothed or interpolated) across the full 512 sample range.
   *
   * @param index (0-511) of the sample to retrieve.
   * @return fft sample, normalized to range 0 to 1.
   */
  private float getFrequencyData(int index) {
    return meter.getBandf((int) Math.floor((float) index * fftResampleFactor));
  }

  /**
   * Retrieve a single sample of the current frame's waveform data from the engine
   *
   * @param index (0-511) of the sample to retrieve
   * @return waveform sample, range -1 to 1
   */
  private float getWaveformData(int index) {
    return meter.getSamples()[index];
  }

  public void bindAudioTexture() {
    bindTextureUnit(TEXTURE_UNIT_AUDIO, this.audioTextureHandle[0]);
  }

  /**
   * Construct texture to hold audio fft and waveform data and bind it to texture unit 0 for the
   * entire run. Once done, every shader pattern can access the audio data texture with minimal
   * per-pattern work.
   */
  private void initializeAudioTexture() {
    // allocate backing buffer in native memory
    this.audioTextureData = GLBuffers.newDirectFloatBuffer(audioTextureHeight * audioTextureWidth);

    // create texture and bind it to the dedicated texture unit
    gl4.glGenTextures(1, audioTextureHandle, 0);
    bindAudioTexture();

    // allocate storage once
    gl4.glTexImage2D(
        GL4.GL_TEXTURE_2D,
        0,
        GL4.GL_R32F,
        audioTextureWidth,
        audioTextureHeight,
        0,
        GL4.GL_RED,
        GL_FLOAT,
        null);

    // configure texture coordinate handling
    // TODO - would GL_LINEAR filtering look more interesting here?
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
  }

  /** Update audio texture object with new fft and waveform data. This is called once per frame. */
  private void updateAudioTexture() {
    // load frequency and waveform data into our texture buffer
    for (int n = 0; n < audioTextureWidth; n++) {
      // fft data in the first row
      audioTextureData.put(n, getFrequencyData(n));
      // normalized audio waveform data in the second row
      audioTextureData.put(n + audioTextureWidth, getWaveformData(n));
    }

    bindAudioTexture();

    // update audio texture on the GPU from our buffer, *without* re-allocating
    gl4.glTexSubImage2D(
        GL_TEXTURE_2D,
        0,
        0,
        0,
        audioTextureWidth,
        audioTextureHeight,
        GL4.GL_RED,
        GL_FLOAT,
        audioTextureData);
  }

  /**
   * Set the palette size and colors in the shader uniform block.
   *
   * @param uniformBuffer The uniform block buffer. When called, the current index must be at the
   *     start of the palette data area
   */
  public void setPaletteUniforms(FloatBuffer uniformBuffer) {
    float r, g, b;
    LXSwatch activeSwatch = getLX().engine.palette.swatch;
    int n = activeSwatch.colors.size();

    // store palette size (iPaletteSize)
    uniformBuffer.put((float) activeSwatch.colors.size());

    // Move to the next 4-byte boundary
    // NOTE: This is necessary to ensure that the palette colors array is properly
    // aligned.  If you add more elements to the per-frame uniform block, you may need to
    // adjust this padding to work with the new size.
    uniformBuffer.position(uniformBuffer.position() + 3);

    // store palette colors
    for (int i = 0; i < n; i++) {
      int color = activeSwatch.getColor(i).getColor();

      r = (float) (0xff & LXColor.red(color)) / 255f;
      uniformBuffer.put(r);
      g = (float) (0xff & LXColor.green(color)) / 255f;
      uniformBuffer.put(g);
      b = (float) (0xff & LXColor.blue(color)) / 255f;
      uniformBuffer.put(b);
      uniformBuffer.put(1.0f); // alpha (filler at this point)
    }
  }

  /**
   * Return a properly aligned size for a uniform block. This is necessary, particularly on macOS,
   * where the size of a uniform block must be a multiple of GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT.
   * (NVidia drivers on Windows and Linux are more forgiving. I always discover things like this the
   * hard way!)
   *
   * @param elements The number of 4-byte elements in the uniform block
   * @return The size of the uniform block in bytes, aligned to the
   *     GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT
   */
  private int getUBOAlignedSize(int elements) {
    int[] alignment = new int[1];
    gl4.glGetIntegerv(GL4.GL_UNIFORM_BUFFER_OFFSET_ALIGNMENT, alignment, 0);
    return ((elements * 4) + alignment[0] - 1) & ~(alignment[0] - 1);
  }

  /**
   * Initialize shared uniform blocks. These blocks let us centrally manage uniforms that are common
   * to all shaders and are only updated once per run, or once per frame. Keeping a single copy of
   * these uniforms in GPU memory saves space and reduces the number of uniform setup and data
   * transfer calls needed for each running shader.
   */
  private void initializeUniformBlocks() {

    // Allocate backing buffer for per run uniforms in native memory
    // First determine the size of the buffer.  Unfortunately, in Java this
    // needs to be done manually, as we can't directly get the size of the corresponding
    // c-language struct. Fortunately, it's not difficult, but the layout rules
    // must be followed exactly.   If you need to add elements to a uniform block,
    // see the spec at: http://www.opengl.org/registry/specs/ARB/uniform_buffer_object.txt
    //

    // At present,we need 6 floats for per run uniforms:
    // 4 floats for vec4 iMouse
    // 2 floats for vec2 iResolution
    //
    this.perRunUniformBlockSize = getUBOAlignedSize(6);
    this.perRunUniformBlock = GLBuffers.newDirectFloatBuffer(perRunUniformBlockSize / 4);

    // copy data to the perRunUniformBlock buffer
    // iMouse is not used, but is retained for Shadertoy compatibility.
    // We'll just zero it out.
    perRunUniformBlock.put(0, 0f);
    perRunUniformBlock.put(1, 0f);
    perRunUniformBlock.put(2, 0f);
    perRunUniformBlock.put(3, 0f);

    // iResolution is the size of the canvas
    perRunUniformBlock.put(4, (float) width);
    perRunUniformBlock.put(5, (float) height);

    // Do the same thing for per frame uniforms
    // The items in the block are, in order:
    // 1 float for beat
    // 1 float for sinPhaseBeat
    // 1 float for bassLevel
    // 1 float for trebleLevel
    // 1 float for bassRatio
    // 1 float for trebleRatio
    // 1 float for volumeRatio
    // 1 float for stemBass
    // 1 float for stemDrums
    // 1 float for stemVocals
    // 1 float for stemOther
    // 1 float for stemDrumHits
    // 1 float for iPaletteSize
    // 20 floats (5 x 4) for the palette
    // VERY IMPORTANT NOTE: whatever order this buffer is loaded in MUST be replicated exactly
    // in the shader framework code's uniform block declaration. Otherwise, the uniforms will not
    // have the correct values in the shader.
    this.perFrameUniformBlockSize = getUBOAlignedSize(36);
    this.perFrameUniformBlock = GLBuffers.newDirectFloatBuffer(perFrameUniformBlockSize / 4);

    // Generate the uniform block buffers
    gl4.glGenBuffers(2, uniformBlockHandles, 0);

    // Bind the per-run uniform block to the gl buffer object
    gl4.glBindBuffer(GL4.GL_UNIFORM_BUFFER, uniformBlockHandles[0]);

    // copy the per-run uniform block data to the buffer
    gl4.glBufferData(
        GL4.GL_UNIFORM_BUFFER, perRunUniformBlockSize, perRunUniformBlock, GL4.GL_DYNAMIC_DRAW);
    gl4.glBindBufferRange(
        GL4.GL_UNIFORM_BUFFER,
        perRunUniformBlockBinding,
        uniformBlockHandles[0],
        0,
        perRunUniformBlockSize);

    // Do the same for the per-frame uniform block and its initial data
    gl4.glBindBuffer(GL4.GL_UNIFORM_BUFFER, uniformBlockHandles[1]);
    gl4.glBufferData(
        GL4.GL_UNIFORM_BUFFER, perFrameUniformBlockSize, perFrameUniformBlock, GL4.GL_DYNAMIC_DRAW);
    gl4.glBindBufferRange(
        GL4.GL_UNIFORM_BUFFER,
        perFrameUniformBlockBinding,
        uniformBlockHandles[1],
        0,
        perFrameUniformBlockSize);
  }

  // Update once-per-frame audio data and all the calculated
  // values derived from it.
  private void updateAudioFrameData(double deltaMs) {
    beat = lx.engine.tempo.basis();
    sinPhaseBeat = 0.5 + 0.5 * Math.sin(Math.PI * beat);

    // current instantaneous levels of frequency ranges we're interested in
    volume = Math.max(AUDIO_LEVEL_MIN, meter.getNormalized());
    bassLevel = Math.max(AUDIO_LEVEL_MIN, meter.getAverage(0, 2));
    trebleLevel =
        Math.max(AUDIO_LEVEL_MIN, meter.getAverage(meter.numBands / 2, meter.numBands / 2));

    // Compute the ratios of current instantaneous levels
    // to their slow EMAs.  See TEAudioPattern.java for more info.
    volumeRatio = volume / avgVolume.update(volume, deltaMs);
    bassRatio = bassLevel / avgBass.update(bassLevel, deltaMs);
    trebleRatio = trebleLevel / avgTreble.update(trebleLevel, deltaMs);
  }

  // update the per-frame shared uniform block with current audio data
  private void updatePerFrameUniforms() {
    perFrameUniformBlock.put((float) beat); // beat
    perFrameUniformBlock.put((float) sinPhaseBeat); // sinPhaseBeat
    perFrameUniformBlock.put((float) bassLevel); // bassLevel
    perFrameUniformBlock.put((float) trebleLevel); // trebleLevel
    perFrameUniformBlock.put((float) bassRatio); // bassRatio
    perFrameUniformBlock.put((float) trebleRatio); // trebleRatio
    perFrameUniformBlock.put((float) volumeRatio); // volumeRatio

    int stemCount = Math.min(MAX_AUDIO_STEMS, AudioStems.get().stems.size());
    for (int i = 0; i < MAX_AUDIO_STEMS; i++) {
      perFrameUniformBlock.put(
          (i < stemCount) ? (float) AudioStems.get().stems.get(i).getValue() : 0f);
    }

    // set the palette size and colors
    setPaletteUniforms(perFrameUniformBlock);

    // update the GPU buffer with the new data
    perFrameUniformBlock.rewind();
    gl4.glBindBuffer(GL4.GL_UNIFORM_BUFFER, uniformBlockHandles[1]);
    gl4.glBufferSubData(GL4.GL_UNIFORM_BUFFER, 0, perFrameUniformBlockSize, perFrameUniformBlock);
  }

  /**
   * Check for an OpenGL error, which also resets the error code. Caution: an error detected here
   * could be left over from an earlier GL call instead of being from the most recent call. To be
   * certain, call it before and after your area of concern.
   */
  @SuppressWarnings("unused")
  public void checkGLError() {
    int err = this.gl4.glGetError();
    if (err != GL_NO_ERROR) {
      String errorString = getGLErrorString(err);
      LX.error("GL Error: " + err + " (" + errorString + ")");
    }
  }

  /** Helper method to convert GL error codes to readable strings */
  private String getGLErrorString(int err) {
    return switch (err) {
      case GL_INVALID_ENUM -> "GL_INVALID_ENUM";
      case GL_INVALID_VALUE -> "GL_INVALID_VALUE";
      case GL_INVALID_OPERATION -> "GL_INVALID_OPERATION";
      case GL4.GL_STACK_OVERFLOW -> "GL_STACK_OVERFLOW";
      case GL4.GL_STACK_UNDERFLOW -> "GL_STACK_UNDERFLOW";
      case GL_OUT_OF_MEMORY -> "GL_OUT_OF_MEMORY";
      case GL_INVALID_FRAMEBUFFER_OPERATION -> "GL_INVALID_FRAMEBUFFER_OPERATION";
      case GL4.GL_CONTEXT_LOST -> "GL_CONTEXT_LOST";
      default -> "Unknown error code: " + err;
    };
  }

  public GLEngine(LX lx, int width, int height) {
    current = this;

    addParameter("gpuJavaEffects", this.gpuJavaEffects);

    // The shape the user gives us affects the rendered aspect ratio,
    // but what really matters is that it needs to have room for the
    // largest number of points we're going to encounter during a run.
    // TODO - adjust buffer size & shape dynamically as the model changes.
    // TODO - this will require a lot of GPU memory management, so is
    // TODO - a longer-term goal.
    this.width = width;
    this.height = height;

    int maxPoints = this.width * this.height;
    TE.log(
        "GLEngine: Rendering canvas size: "
            + this.width
            + "x"
            + this.height
            + " = "
            + maxPoints
            + " total points");

    // register glEngine so we can access it from patterns.
    // and add it as an engine task for audio analysis and buffer management
    lx.engine.registerComponent(PATH, this);
    lx.engine.addTask(this::initialize);

    // Child engines
    this.textureCache = new TextureManager(lx, this);
    if (this.lx.engine.renderMode.gpu) {
      this.mixer = new GLMixer(lx, this);
    }

    // set up audio fft and waveform handling
    // TODO - strongly consider expanding the number of FFT bands.
    // TODO - LX defaults to 16, but more bands would let us do more
    // TODO - interesting audio analysis.
    this.meter = lx.engine.audio.meter;
    fftResampleFactor = meter.bands.length / 512f;
  }

  private void initialize() {
    // On first frame...
    // create and initialize offscreen drawable for gl rendering
    canvas = ShaderUtils.createGLSurface(width, height);
    canvas.display();
    gl4 = canvas.getGL().getGL4();

    // activate our context and do initialization tasks
    canvas.getContext().makeCurrent();

    // set up shared uniform blocks
    initializeUniformBlocks();

    // Initialize child engines
    this.textureCache.initialize(this.gl4);
    if (this.lx.engine.renderMode.gpu) {
      this.mixer.initialize(this.gl4);
    }

    // set up the per-frame audio info texture
    initializeAudioTexture();

    // Run on every frame. The first run will be before the first mixer loop.
    lx.engine.addLoopTask(this);
  }

  /**
   * Helper method to activate a texture unit and bind a texture to it. In higher OpenGL versions
   * (4.5+) this method is built-in.
   *
   * @param unit Texture unit to activate (0+)
   * @param textureHandle Texture handle that should be bound to the unit
   */
  public void bindTextureUnit(int unit, int textureHandle) {
    // Safety check that textureHandle was initialized
    if (textureHandle < 0) {
      throw new IllegalStateException(
          "Texture has not been initialized, can not bind to unit " + unit);
    }

    this.gl4.glActiveTexture(GL_TEXTURE0 + unit);
    this.gl4.glBindTexture(GL_TEXTURE_2D, textureHandle);
  }

  /**
   * Helper method to unbind the current texture from a texture unit
   *
   * @param unit Texture unit to bind to "0"
   */
  public void unbindTextureUnit(int unit) {
    bindTextureUnit(unit, 0);
  }

  public void loop(double deltaMs) {
    // activate our context and do per-frame tasks
    canvas.getContext().makeCurrent();
    updateAudioFrameData(deltaMs);
    updateAudioTexture();
    updatePerFrameUniforms();

    if (this.lx.engine.renderMode.gpu) {
      this.mixer.loop(deltaMs);
    }
  }

  @Override
  public void dispose() {
    this.textureCache.dispose();

    // free GPU resources that we directly allocated
    if (GLContext.getCurrent() != null) {
      gl4.glDeleteTextures(audioTextureHandle.length, audioTextureHandle, 0);
      gl4.glDeleteBuffers(uniformBlockHandles.length, uniformBlockHandles, 0);
    }

    ShaderUtils.disposeCompileVAO(this.gl4);

    if (this.lx.engine.renderMode.gpu) {
      this.mixer.dispose();
    }

    super.dispose();
  }

  // Static getters for per-frame audio parameters
  // TEPattern and derived pattern classes can access these, which are evaluated
  // only once per frame, instead of recalculating the values for every running pattern
  // instance.
  public static double getAvgVolume() {
    return avgVolume.getValue();
  }

  public static double getAvgBass() {
    return avgBass.getValue();
  }

  public static double getAvgTreble() {
    return avgTreble.getValue();
  }

  public static double getBeat() {
    return beat;
  }

  public static double getSinPhaseOnBeat() {
    return sinPhaseBeat;
  }

  public static double getVolume() {
    return volume;
  }

  public static double getBassLevel() {
    return bassLevel;
  }

  public static double getTrebleLevel() {
    return trebleLevel;
  }

  public static double getVolumeRatio() {
    return volumeRatio;
  }

  public static double getBassRatio() {
    return bassRatio;
  }

  public static double getTrebleRatio() {
    return trebleRatio;
  }
}
