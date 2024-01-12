package titanicsend.pattern.glengine;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import heronarts.lx.audio.GraphicMeter;
import titanicsend.pattern.yoffa.shader_engine.ShaderUtils;

import java.nio.FloatBuffer;

import static com.jogamp.opengl.GL.*;

public class GLEngine extends LXComponent implements LXLoopTask {
  public static final String PATH = "GLEngine";

  protected final LX lx;

  // default canvas size
  private static final int xSize = 640;
  private static final int ySize = 480;

  // audio texture size and buffer
  private static final int audioTextureWidth = 512;
  private static final int audioTextureHeight = 2;
  FloatBuffer audioTextureData;
  int[] audioTextureHandle = new int[1];

  // audio data source & parameters
  GraphicMeter meter;
  float fftResampleFactor;

  boolean isRunning = false;

  // Data and utility methods for the GL canvas/context.
  private GLAutoDrawable canvas = null;

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
  public FloatBuffer getAudioTextureBuffer() { return audioTextureData; }

  public static int getAudioTextureWidth() { return audioTextureWidth; }

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

  public GLEngine(LX lx) {
    this.lx = lx;

    // register glEngine so we can access it from patterns.
    // and add it as an engine task for audio analysis and buffer management
    lx.engine.registerComponent(PATH, this);
    lx.engine.addLoopTask(this);

    // set up audio fft and waveform handling
    // TODO - strongly consider expanding the number of FFT bands.
    // TODO - LX defaults to 16, but more bands would let us do more
    // TODO - interesting audio analysis.
    this.meter = lx.engine.audio.meter;
    fftResampleFactor = meter.bands.length / 512f;
  }

  public void loop(double deltaMs) {

    // Things to be done once, on first frame
    if (canvas == null) {
      // create and initialize offscreen drawable for gl rendering
      canvas = ShaderUtils.createGLSurface(xSize, ySize);
      canvas.display();

      // set up buffer for audio texture data
      this.audioTextureData =
          GLBuffers.newDirectFloatBuffer(audioTextureHeight * audioTextureWidth);

      // build audio texture - we're just creating the texture object and
      // binding it to texture unit 0 here. Texture data will be
      // loaded in the per-frame tasks section below.
      GL4 gl4 = canvas.getGL().getGL4();
      canvas.getContext().makeCurrent();

      gl4.glActiveTexture(GL_TEXTURE0);
      gl4.glEnable(GL_TEXTURE_2D);
      gl4.glGenTextures(1, audioTextureHandle, 0);
      gl4.glBindTexture(GL4.GL_TEXTURE_2D, audioTextureHandle[0]);

      // TODO - would GL_LINEAR filtering look more interesting here?
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

      isRunning = true;
    }

    // Things to be done on every frame, after initial setup
    if (isRunning) {
      // load frequency and waveform data into our texture buffer, fft data
      // in the first row, normalized audio waveform data in the second.
      for (int n = 0; n < audioTextureWidth; n++) {
        audioTextureData.put(n, getFrequencyData(n));
        audioTextureData.put(n + audioTextureWidth, getWaveformData(n));
      }

      // update gl texture object with new audio data and bind it to texture unit 0
      // once done, every shader pattern will have access to the audio data texture
      // with minimal per-pattern work.
      GL4 gl4 = canvas.getGL().getGL4();
      canvas.getContext().makeCurrent();

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
  }


}
