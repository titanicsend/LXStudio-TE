package titanicsend.pattern.glengine;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import heronarts.lx.audio.GraphicMeter;
import heronarts.lx.model.LXModel;
import titanicsend.pattern.yoffa.shader_engine.ShaderUtils;
import titanicsend.util.TE;

import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL.*;

public class GLEngine extends LXComponent implements LXLoopTask, LX.Listener {
  public static final String PATH = "GLEngine";
  public static GLEngine current;

  private final int[] audioTextureHandle = new int[1];

  // rendering canvas size.  May be changed
  // via the startup command line.
  private static int xSize;
  private static int ySize;
  private static int maxPoints;
  private float aspectRatio;

  // Dimensions of mapped texture backbuffer.
  //
  // This buffer is never directly rendered by either the GPU or by Java
  // Instead, the pixel locations corresponding to the lx model's LEDs are
  // colored at frame time and the buffer is supplied as a read-only sampler
  // to the shader system.
  //
  // So increasing the size of this buffer affects memory usage but does
  // not affect rendering performance.  The default size is sufficient for
  // even very large models, but can be increased if necessary.
  // TODO - make this configurable per pattern or effect.
  private static final int mappedBufferWidth = 640;
  private static final int mappedBufferHeight = 640;

  // audio texture size and buffer
  private static final int audioTextureWidth = 512;
  private static final int audioTextureHeight = 2;
  private FloatBuffer audioTextureData;

  // audio data source & parameters
  private final GraphicMeter meter;
  private final float fftResampleFactor;

  // Texture cache management
  private TextureManager textureCache = null;
  private boolean modelChanged = false;

  private boolean isRunning = false;

  // Data and utility methods for the GL canvas/context.
  private GLAutoDrawable canvas = null;
  private GL4 gl4;

  private LXModel model;

  public LXModel getModel() {
    return model;
  }

  // Needed for housekeeping, during static-to-dynamic model transition
  // This lets various shader components know if they're running the static model
  // and need to swap the x and z axes.
  // TODO - remove when we move to dynamic model
  private final boolean isStatic;

  public boolean isStaticModel() {
    return isStatic;
  }

  public GLAutoDrawable getCanvas() {
    return canvas;
  }

  public static int getWidth() {
    return xSize;
  }

  public static int getHeight() {
    return ySize;
  }

  public float getAspectRatio() {
    return aspectRatio;
  }

  public static int getMappedBufferWidth() {
    return mappedBufferWidth;
  }

  public static int getMappedBufferHeight() {
    return mappedBufferHeight;
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
   * Copy a model's normalized coordinates into a special texture for use by shaders. Must be
   * called by the parent pattern or effect at least once before the first frame is rendered and
   * Should be called by the pattern's frametime run() function on every frame for full Chromatik view
   * support.
   *
   * @param model The model (view) to copy coordinates from
   * @return The texture unit number that the view's coordinate texture
   * is bound to.
   */
  public int useViewCoordinates(LXModel model) {
    return textureCache.useViewCoordinates(model);
  }

  // Load a static texture from a file and bind it to the next available texture unit
  // Returns the texture unit number. If the texture is already loaded, just increment
  // the ref count and return the existing texture unit number.
  public int useTexture(GL4 gl4, String textureName) {
    return textureCache.useTexture(gl4, textureName);
  }

  public void releaseTexture(String textureName) {
    textureCache.releaseTexture(textureName);
  }

  // Returns the next available texture unit number, either by reusing a released
  // texture unit number or by allocating a new one.
  public int getNextTextureUnit() {
    return textureCache.getNextTextureUnit();
  }

  public int releaseTextureUnit(int textureUnit) {
    return textureCache.releaseTextureUnit(textureUnit);
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

  /**
   * Construct texture to hold audio fft and waveform data and bind it to texture unit 0 for the
   * entire run. Once done, every shader pattern can access the audio data texture with minimal
   * per-pattern work.
   */
  private void initializeAudioTexture() {
    // allocate backing buffer in native memory
    this.audioTextureData = GLBuffers.newDirectFloatBuffer(audioTextureHeight * audioTextureWidth);

    // create texture and bind it to texture unit 0, where it will stay for the whole run
    gl4.glActiveTexture(GL_TEXTURE0);
    gl4.glEnable(GL_TEXTURE_2D);
    gl4.glGenTextures(1, audioTextureHandle, 0);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, audioTextureHandle[0]);

    // configure texture coordinate handling
    // TODO - would GL_LINEAR filtering look more interesting here?
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
  }

  /**
   * Update audio texture object with new fft and waveform data. This is called once per frame.
   */
  private void updateAudioTexture() {
    // load frequency and waveform data into our texture buffer, fft data
    // in the first row, normalized audio waveform data in the second.
    for (int n = 0; n < audioTextureWidth; n++) {
      audioTextureData.put(n, getFrequencyData(n));
      audioTextureData.put(n + audioTextureWidth, getWaveformData(n));
    }

    // update audio texture on the GPU from our buffer
    gl4.glActiveTexture(GL_TEXTURE0);
    gl4.glBindTexture(GL_TEXTURE_2D, audioTextureHandle[0]);

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
  }

  public GLEngine(LX lx, int width, int height, boolean isStaticModel) {
    current = this;
    // The shape the user gives us affects the rendered aspect ratio,
    // but what really matters is that it needs to have room for the
    // largest number of points we're going to encounter during a run.
    // TODO - adjust buffer size & shape dynamically as the model changes.
    // TODO - this will require a lot of GPU memory management, so is
    // TODO - a longer-term goal.
    this.xSize = width;
    this.ySize = height;

    this.maxPoints = xSize * ySize;
    aspectRatio = 1.0f;
    TE.log("GLEngine: Rendering canvas size: " + xSize + "x" + ySize + " = " + GLEngine.maxPoints + " total points");

    // register glEngine so we can access it from patterns.
    // and add it as an engine task for audio analysis and buffer management
    lx.engine.registerComponent(PATH, this);
    lx.engine.addLoopTask(this);

    this.isStatic = isStaticModel;
    this.model = lx.getModel();

    // set up audio fft and waveform handling
    // TODO - strongly consider expanding the number of FFT bands.
    // TODO - LX defaults to 16, but more bands would let us do more
    // TODO - interesting audio analysis.
    this.meter = lx.engine.audio.meter;
    fftResampleFactor = meter.bands.length / 512f;
  }

  @Override
  public void modelGenerationChanged(LX lx, LXModel model) {
    this.model = model;
    this.modelChanged = true;
  }

  public void loop(double deltaMs) {

    // On first frame...
    if (canvas == null) {
      // create and initialize offscreen drawable for gl rendering
      canvas = ShaderUtils.createGLSurface(xSize, ySize);
      canvas.display();
      gl4 = canvas.getGL().getGL4();

      // activate our context and do initialization tasks
      canvas.getContext().makeCurrent();

      // set up the per-frame audio info texture
      initializeAudioTexture();
      textureCache = new TextureManager(gl4);

      // start listening for model changes
      lx.addListener(this);

      // set running flag once initialization is complete
      isRunning = true;
    }

    // On every frame, after initial setup
    if (isRunning) {
      // activate our context and do per-frame tasks
      canvas.getContext().makeCurrent();
      updateAudioTexture();

      if (modelChanged) {
        // if the model has changed, discard all existing view coordinate textures
        textureCache.clearCoordinateTextures();
        modelChanged = false;
      }

    }
  }

  public void dispose() {
    // free other GPU resources that we directly allocated
    gl4.glDeleteTextures(audioTextureHandle.length, audioTextureHandle, 0);
  }
}
