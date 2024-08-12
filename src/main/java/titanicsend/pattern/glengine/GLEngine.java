package titanicsend.pattern.glengine;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import heronarts.lx.audio.GraphicMeter;
import heronarts.lx.model.LXModel;
import heronarts.lx.model.LXPoint;
import titanicsend.pattern.yoffa.shader_engine.ShaderUtils;
import titanicsend.util.TE;

import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.jogamp.opengl.GL.*;

public class GLEngine extends LXComponent implements LXLoopTask, LX.Listener {
  public static final String PATH = "GLEngine";

  private static final int TEXTURE_HANDLE_AUDIO = 0;
  private static final int TEXTURE_HANDLE_XN = 1;
  private static final int TEXTURE_HANDLE_YN = 2;
  private static final int TEXTURE_HANDLE_ZN = 3;
  private static final int TEXTURE_HANDLE_EXISTS = 4;
  private final int[] textureHandles = {
          TEXTURE_HANDLE_AUDIO,
          TEXTURE_HANDLE_XN,
          TEXTURE_HANDLE_YN,
          TEXTURE_HANDLE_ZN,
          TEXTURE_HANDLE_EXISTS
  };

  // rendering canvas size.  May be changed
  // via the startup command line.
  private static int xSize;
  private static int ySize;
  private static int resolution;

  // Location textures
  private FloatBuffer xn;
  private FloatBuffer yn;
  private FloatBuffer zn;
  private FloatBuffer exists;

  // audio texture size and buffer
  private static final int audioTextureWidth = 512;
  private static final int audioTextureHeight = 2;
  private FloatBuffer audioTextureData;

  // audio data source & parameters
  private final GraphicMeter meter;
  private final float fftResampleFactor;

  // texture unit management
  private class TextureUnit {
    private Texture texture;
    private int textureUnit;
    private int refCount;
  }

  private int nextTextureUnit = 6; //2;

  // staticTextures is a map of texture names to texture unit numbers.
  private final HashMap<String, TextureUnit> staticTextures = new HashMap<>();

  // a list of that we can use to keep track of released texture unit numbers
  private final ArrayList<Integer> releasedTextureUnits = new ArrayList<>();

  private boolean isRunning = false;

  // Data and utility methods for the GL canvas/context.
  private GLAutoDrawable canvas = null;
  private GL4 gl4;

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

  // Returns the next available texture unit number, either by reusing a released
  // texture unit number or by allocating a new one.
  public int getNextTextureUnit() {
    if (releasedTextureUnits.size() > 0) {
      return releasedTextureUnits.remove(0);
    } else {
      return nextTextureUnit++;
    }
  }

  // Add a released (ref count 0) texture unit number to the list of available
  // texture units
  private void recycleTextureUnit(TextureUnit t) {
    if (!releasedTextureUnits.contains(t.textureUnit)) {
      releasedTextureUnits.add(t.textureUnit);
    }
  }

  public int useTexture(GL4 gl4, String textureName) {
    TextureUnit t = new TextureUnit();

    try {
      // if the texture is already loaded, just increment the ref count
      if (staticTextures.containsKey(textureName)) {
        t = staticTextures.get(textureName);
        t.refCount++;
      } else {
        // otherwise, load the texture its file and bind it to the next available texture unit
        File file = new File(textureName);
        t.texture = TextureIO.newTexture(file, false);
        t.textureUnit = getNextTextureUnit();
        t.refCount = 1;
        staticTextures.put(textureName, t);

        gl4.glActiveTexture(GL_TEXTURE0 + t.textureUnit);
        gl4.glEnable(GL_TEXTURE_2D);
        t.texture.enable(gl4);
        t.texture.bind(gl4);
      }
      // return the texture unit number
      return t.textureUnit;

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void releaseTexture(String textureName) {
    TextureUnit t = staticTextures.get(textureName);
    if (t == null) {
      throw new RuntimeException("Attempt to release texture that was never used: " + textureName);
    }
    t.refCount--;
    if (t.refCount <= 0) {
      t.texture.destroy(gl4);
      recycleTextureUnit(t);
      staticTextures.remove(textureName);
    }
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
    gl4.glGenTextures(1, textureHandles, TEXTURE_HANDLE_AUDIO);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, textureHandles[TEXTURE_HANDLE_AUDIO]);

    // configure texture coordinate handling
    // TODO - would GL_LINEAR filtering look more interesting here?
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
  }

  private void initializeLocationTextures() {
    this.xn = initializeLocationTexture(GL_TEXTURE1, TEXTURE_HANDLE_XN);
    this.yn = initializeLocationTexture(GL_TEXTURE2, TEXTURE_HANDLE_YN);
    this.zn = initializeLocationTexture(GL_TEXTURE3, TEXTURE_HANDLE_ZN);
    this.exists = initializeLocationTexture(GL_TEXTURE4, TEXTURE_HANDLE_EXISTS);
  }

  private FloatBuffer initializeLocationTexture(int gl_texture, int index) {
    // allocate backing buffer in native memory
    FloatBuffer buffer = GLBuffers.newDirectFloatBuffer(xSize * ySize);

    // create texture and bind it to texture unit 0, where it will stay for the whole run
    gl4.glActiveTexture(gl_texture);
    gl4.glEnable(GL_TEXTURE_2D);
    gl4.glGenTextures(1, textureHandles, index);
    gl4.glBindTexture(GL4.GL_TEXTURE_2D, textureHandles[index]);

    // configure texture coordinate handling
    // TODO - would GL_LINEAR filtering look more interesting here?
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

    return buffer;
  }

  /** Update audio texture object with new fft and waveform data. This is called once per frame. */
  private void updateAudioTexture() {
    // load frequency and waveform data into our texture buffer, fft data
    // in the first row, normalized audio waveform data in the second.
    for (int n = 0; n < audioTextureWidth; n++) {
      audioTextureData.put(n, getFrequencyData(n));
      audioTextureData.put(n + audioTextureWidth, getWaveformData(n));
    }

    // update audio texture on the GPU from our buffer
    gl4.glActiveTexture(GL_TEXTURE0);
    gl4.glBindTexture(GL_TEXTURE_2D, textureHandles[TEXTURE_HANDLE_AUDIO]);

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

  /**
   * Copy LXPoints' normalized coordinates into textures for use by shaders.
   */
  private void updateLocationTextures() {
    this.modelChanged = false;
    final int numPoints = this.model.points.length;

    for (int i = 0; i < resolution; i++) {
      if (i < numPoints) {
        this.xn.put(i, this.model.points[i].xn);
        this.yn.put(i, this.model.points[i].yn);
        this.zn.put(i, this.model.points[i].zn);
        this.exists.put(i, 1);
      } else {
        this.xn.put(i, 0);
        this.yn.put(i, 0);
        this.zn.put(i, 0);
        this.exists.put(i, 0);
      }
    }

    // update location textures on the GPU from our buffer
    // x
    gl4.glActiveTexture(GL_TEXTURE1);
    gl4.glBindTexture(GL_TEXTURE_2D, textureHandles[TEXTURE_HANDLE_XN]);
    gl4.glTexImage2D(
            GL4.GL_TEXTURE_2D,
            0,
            GL4.GL_R32F,
            xSize,
            ySize,
            0,
            GL4.GL_RED,
            GL_FLOAT,
            xn);
    // y
    gl4.glActiveTexture(GL_TEXTURE2);
    gl4.glBindTexture(GL_TEXTURE_2D, textureHandles[TEXTURE_HANDLE_YN]);
    gl4.glTexImage2D(
            GL4.GL_TEXTURE_2D,
            0,
            GL4.GL_R32F,
            xSize,
            ySize,
            0,
            GL4.GL_RED,
            GL_FLOAT,
            yn);
    // z
    gl4.glActiveTexture(GL_TEXTURE3);
    gl4.glBindTexture(GL_TEXTURE_2D, textureHandles[TEXTURE_HANDLE_ZN]);
    gl4.glTexImage2D(
            GL4.GL_TEXTURE_2D,
            0,
            GL4.GL_R32F,
            xSize,
            ySize,
            0,
            GL4.GL_RED,
            GL_FLOAT,
            zn);
    // exists
    gl4.glActiveTexture(GL_TEXTURE4);
    gl4.glBindTexture(GL_TEXTURE_2D, textureHandles[TEXTURE_HANDLE_EXISTS]);
    gl4.glTexImage2D(
            GL4.GL_TEXTURE_2D,
            0,
            GL4.GL_R32F,
            xSize,
            ySize,
            0,
            GL4.GL_RED,
            GL_FLOAT,
            exists);
    LX.log("GLEngine refreshed location data for " + numPoints + " points");

    if (numPoints > resolution) {
      LX.error("GLEngine resolution (" + resolution
              + ") too small for number of points in the model (" + numPoints + ")");
    }
  }

  private LXModel model;

  public GLEngine(LX lx, int width, int height,  boolean isStaticModel) {
    // save rendering canvas dimensions
    xSize = width;
    ySize = height;
    resolution = width * height;
    TE.log("GLEngine: Rendering canvas size: " + xSize + "x" + ySize + " = " + resolution + " total points");

    // register glEngine so we can access it from patterns.
    // and add it as an engine task for audio analysis and buffer management
    lx.engine.registerComponent(PATH, this);
    lx.engine.addLoopTask(this);

    this.isStatic = isStaticModel;
    this.model = lx.getModel();
    lx.addListener(this);

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
    modelChanged = true;
  }

  private boolean modelChanged = true;

  public void loop(double deltaMs) {

    // On first frame...
    if (canvas == null) {
      // create and initialize offscreen drawable for gl rendering
      canvas = ShaderUtils.createGLSurface(xSize, ySize);
      canvas.display();
      gl4 = canvas.getGL().getGL4();

      // activate our context and do initialization tasks
      canvas.getContext().makeCurrent();

      initializeLocationTextures();
      initializeAudioTexture();

      // set running flag once initialization is complete
      isRunning = true;
    }

    // On every frame, after initial setup
    if (isRunning) {

      // activate our context and do per-frame tasks
      canvas.getContext().makeCurrent();

      if (this.modelChanged) {
        updateLocationTextures();
      }
      updateAudioTexture();
    }
  }

  public void dispose() {
    this.lx.removeListener(this);
  }
}
