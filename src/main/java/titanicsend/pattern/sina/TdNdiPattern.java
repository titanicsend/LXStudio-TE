package titanicsend.pattern.sina;

import static me.walkerknapp.devolay.DevolayReceiver.RECEIVE_BANDWIDTH_HIGHEST;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import me.walkerknapp.devolay.DevolayFrameType;
import me.walkerknapp.devolay.DevolayReceiver;
import me.walkerknapp.devolay.DevolayVideoFrame;
import titanicsend.ndi.NDIEngine;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

/**
 * Test class for NDIReceiver: displays video frames, and adds a second shader for a simple edge
 * detection effect (controlled by WOW2)
 */
@LXCategory("AAA")
public class TdNdiPattern extends TEPerformancePattern {

  protected NDIEngine ndiEngine;

  protected DevolayVideoFrame videoFrame;
  protected DevolayReceiver receiver = null;
  protected int frameWidth;
  protected int frameHeight;

  protected boolean lastConnectState = false;
  protected long connectTimer = 0;
  protected String channel_name = "TD_Mapped_TE";

  public ByteBuffer buffer; // DO NOT SUBMIT: Figure out what to do with this
  public TextureData textureData = null; // DO NOT SUBMIT: Figure out what to do with this too
  public Texture texture = null; // DO NOT SUBMIT: Figure out what to do with this too
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

  public TdNdiPattern(LX lx) {
    this(lx, TEShaderView.ALL_POINTS);
  }

  public TdNdiPattern(LX lx, TEShaderView view) {
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

    changeChannel();
  }

  protected void changeChannel() {
    if (receiver != null) {
      lastConnectState = ndiEngine.connectByName(channel_name, receiver);
    }
  }

  public void runTEAudioPattern(double deltaMs) {
    // Periodically try to connect if we weren't able to
    // establish an initial connection to this pattern's
    // desired NDI source. This makes things work
    // without manual intervention if the source isn't
    // available at startup.  Once the source becomes available,
    // the connection will be automatically established.
    if (!lastConnectState) {
      if (System.currentTimeMillis() - connectTimer > 1000) {
        connectTimer = System.currentTimeMillis();
        lastConnectState = ndiEngine.connectByName(channel_name, receiver);
      }
    }

    // if we have
    if (DevolayFrameType.VIDEO == receiver.receiveCapture(videoFrame, null, null, 0)) {
      // get pixel data from video frame
      ByteBuffer frameData = videoFrame.getData();
      frameData.rewind();
      frameData.order(ByteOrder.LITTLE_ENDIAN);

      for (LXPoint p : this.model.points) {
        int i = p.index * 4;

        byte r = frameData.get(i + 2);
        byte g = frameData.get(i + 1);
        byte b = frameData.get(i);

        colors[p.index] = LXColor.rgb(r, g, b);
      }
    }
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
      lastConnectState = ndiEngine.connectByName(channel_name, receiver);
    }
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
