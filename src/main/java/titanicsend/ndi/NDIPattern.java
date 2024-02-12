package titanicsend.ndi;

import static me.walkerknapp.devolay.DevolayReceiver.RECEIVE_BANDWIDTH_HIGHEST;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;
import heronarts.lx.LX;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import java.nio.ByteBuffer;
import me.walkerknapp.devolay.DevolayFrameType;
import me.walkerknapp.devolay.DevolayReceiver;
import me.walkerknapp.devolay.DevolayVideoFrame;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

public class NDIPattern extends GLShaderPattern {

  protected NDIEngine ndiEngine;

  protected DevolayVideoFrame videoFrame;
  protected DevolayReceiver receiver = null;
  protected int sourceIndex = 0;
  protected int frameWidth;
  protected int frameHeight;

  protected boolean lastConnectState = false;
  protected long connectTimer = 0;

  protected ByteBuffer buffer;
  protected TextureData textureData = null;
  protected Texture texture = null;
  protected GL4 gl4;

  protected final LXListenableNormalizedParameter source;

  public LXNormalizedParameter getSourceControl() {
    return source;
  }

  protected final LXListenableNormalizedParameter gain;

  public LXNormalizedParameter getGainControl() {
    return gain;
  }

  protected ByteBuffer getImageBuffer() {
    return buffer;
  }

  public NDIPattern(LX lx) {
    this(lx, TEShaderView.ALL_POINTS);
  }

  public NDIPattern(LX lx, TEShaderView view) {
    super(lx, view);
    ndiEngine = NDIEngine.get();

    // Create frame objects to handle incoming video stream
    // (note that we are omitting audio and metadata frames for now)
    videoFrame = new DevolayVideoFrame();

    source =
        new CompoundParameter("Source", 0, 0, 10)
            .setDescription("NDI Source")
            .setUnits(LXParameter.Units.INTEGER);

    gain = new CompoundParameter("Gain", 1, 0.5, 2).setDescription("Video gain");

    // default common controls settings.  Note that these aren't committed
    // until the pattern calls addCommonControls(), so patterns can
    // override these settings if they need to.

    // set scale control to something that works for video.
    controls.setRange(TEControlTag.SIZE, 1, 5, 0.1);

    // allocate a backbuffer for all the shaders to share
    buffer = GLShader.allocateBackBuffer();

    // add the primary shader, which handles mapping incoming video frames
    // from the NDI source to the shared buffer.
    addPrimaryShader();
  }

  protected void changeChannel(int channel) {
    sourceIndex = channel;
    if (receiver != null) {
      lastConnectState = ndiEngine.connectByIndex(sourceIndex, receiver);
    }
  }

  /**
   * Create a texture (and associated Java objects) appropriately dimensioned the current incoming
   * video frame size. This is called on initial connection, and whenever the frame size changes.
   */
  public void createTextureForFrame() {

    // free the previous GPU texture if it
    // exists
    if (texture != null) {
      texture.destroy(gl4);
    }

    // save dimensions of the current frame
    frameWidth = videoFrame.getXResolution();
    frameHeight = videoFrame.getYResolution();

    int format = GL.GL_RGBA;
    int type = GL.GL_UNSIGNED_BYTE;
    textureData =
        new TextureData(
            gl4.getGLProfile(),
            format,
            frameWidth,
            frameHeight,
            0,
            GL.GL_BGRA,
            type,
            false,
            false,
            false,
            videoFrame.getData(),
            null);
    texture = TextureIO.newTexture(textureData);
  }

  /**
   * The primary shader handles incoming video frames from the NDI source. Your pattern can override
   * this method if you need something other than the default behavior. (Xpos/Ypos, Spin, Angle,
   * Size, Brightness, Explode work as expected, Wow1 controls the full color vs. palette color mix)
   */
  public void addPrimaryShader() {
    GLShader shader = new GLShader(lx, "ndidefault.fs", getControlData(), buffer);
    addShader(
        shader,
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {

            int ch = (int) source.getValue();
            if (ch != sourceIndex) {
              changeChannel(ch);
            }

            // Periodically try to connect if we weren't able to
            // establish an initial connection to this pattern's
            // desired NDI source. This makes things work
            // without manual intervention if the source isn't
            // available at startup.  Once the source becomes available,
            // the connection will be automatically established.
            if (!lastConnectState) {
              if (System.currentTimeMillis() - connectTimer > 1000) {
                connectTimer = System.currentTimeMillis();
                lastConnectState = ndiEngine.connectByIndex(sourceIndex, receiver);
              }
            }

            // if we have
            if (DevolayFrameType.VIDEO == receiver.receiveCapture(videoFrame, null, null, 0)) {
              // get pixel data from video frame
              ByteBuffer frameData = videoFrame.getData();

              // if it's the first frame, or if the frame dimensions changed,
              // build our texture data object. Otherwise, just replace the
              // buffer with the current frame's pixel data.
              if (textureData == null
                  || frameWidth != videoFrame.getXResolution()
                  || frameHeight != videoFrame.getYResolution()) {
                gl4 = s.getGL4();
                createTextureForFrame();
              } else {
                textureData.setBuffer(frameData);
                texture.updateImage(gl4, textureData);
              }
              // pass the video frame texture to the shader
              s.setUniform("gain", gain.getValuef());
              s.setUniform("ndivideo", texture);
            }
          }
        });
  }

  @Override
  public void onActive() {
    super.onActive();
    // if no receiver yet, create one, then connect it to
    // its saved source if possible.
    if (receiver == null) {
      receiver =
          new DevolayReceiver(
              DevolayReceiver.ColorFormat.BGRX_BGRA, RECEIVE_BANDWIDTH_HIGHEST, true, "TE");
    }
    lastConnectState = ndiEngine.connectByIndex(sourceIndex, receiver);
  }

  @Override
  public void onInactive() {
    // disconnect receiver from all sources
    receiver.connect(null);
    super.onInactive();
  }

  @Override
  public void dispose() {
    // shut down receiver and free everything
    // we allocated.
    if (videoFrame != null) {
      videoFrame.close();
    }
    if (receiver != null) {
      receiver.connect(null);
    }

    if (texture != null) texture.destroy(gl4);

    super.dispose();
  }
}
