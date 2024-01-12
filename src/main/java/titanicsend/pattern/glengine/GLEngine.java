package titanicsend.pattern.glengine;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.GLBuffers;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import heronarts.lx.audio.GraphicMeter;
import titanicsend.pattern.yoffa.shader_engine.ShaderUtils;
import titanicsend.util.TE;

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

  // audio data parameters
  GraphicMeter meter;
  float fftResampleFactor;

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

  public FloatBuffer getAudioTextureBuffer() {
    return audioTextureData;
  }

  public int[] getAudioTextureHandle() { return audioTextureHandle; }

  public int getAudioTextureWidth() {
    return audioTextureWidth;
  }

  public int getAudioTextureHeight() {
    return audioTextureHeight;
  }

  public GLEngine(LX lx) {
    this.lx = lx;

    // register glEngine so we can access it from patterns.
    // and add it as an engine task for audio analysis and buffer management
    lx.engine.registerComponent(PATH, this);
    lx.engine.addLoopTask(this);

    // set up audio fft and waveform handling
    this.meter = lx.engine.audio.meter;
    fftResampleFactor = meter.bands.length / 512f;
  }

  public void loop(double deltaMs) {
    // TEST-TEST-TEST
    // Create an offscreen drawable and context on the engine thread.
    // TODO - get rid of the default FBO and let the pattern init code specify render buffers
    //
    // TODO - fix ConstructedPattern so we can use it w/all TE pattern classes w/o the
    // additional overhead it currently adds.  The gateway to this problem is the doodad
    // in TEApp that scans all the classes in a file looking for things to register.
    // We do need to be sure it works with the java FragmentShaderEffect patterns
    // (and should also add a once-per-frame function to those patterns).
    //
    // We can run everything on a single context assuming that all loop tasks
    // and patterns run sequentially on the engine thread.
    //
    // On memory: If the number
    // of contexts is the limiting factor, simply keeping all these shader programs
    // around shouldn't be a problem even on a 16gb Mac.  Otherwise...
    // TODO - figure out how to allocate and free all GL resources on pattern activate/deactivate.
    // TODO - save shader binary in GLShader so we don't have to hit the disk every time.
    // TODO - determine what happens to existing native resources on project load.
    //
    if (canvas == null) {
      canvas = ShaderUtils.createGLSurface(xSize, ySize);
      canvas.display();
      TE.log("GLEngine: Created main offscreen drawable & context");

      this.audioTextureData =
          GLBuffers.newDirectFloatBuffer(audioTextureHeight * audioTextureWidth);

      // Audio texture object - we're just creating the texture object and
      // binding it to texture unit 0 here.
      // For now, data is loaded by patterns.
      // TODO - this data should be loaded as part of the GL Engine loop task.
      GL4 gl4 = canvas.getGL().getGL4();
      gl4.glEnable(GL_TEXTURE_2D);
      gl4.glGenTextures(1, audioTextureHandle, 0);
      gl4.glBindTexture(GL4.GL_TEXTURE_2D, audioTextureHandle[0]);

      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
      gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);

      gl4.glActiveTexture(GL_TEXTURE0);
      gl4.glEnable(GL_TEXTURE_2D);
      gl4.glBindTexture(GL4.GL_TEXTURE_2D, audioTextureHandle[0]);
    }

    // load frequency and waveform data into our texture, fft data in the first row,
    // normalized audio waveform data in the second.
    for (int n = 0; n < audioTextureWidth; n++) {
      audioTextureData.put(n, getFrequencyData(n));
      audioTextureData.put(n + audioTextureWidth, getWaveformData(n));
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
  public float getFrequencyData(int index) {
    return meter.getBandf((int) Math.floor((float) index * fftResampleFactor));
  }

  /**
   * Retrieve a single sample of the current frame's waveform data from the engine
   *
   * @param index (0-511) of the sample to retrieve
   * @return waveform sample, range -1 to 1
   */
  public float getWaveformData(int index) {
    return meter.getSamples()[index];
  }

}
