package titanicsend.pattern.sinas;

import static me.walkerknapp.devolay.DevolayReceiver.RECEIVE_BANDWIDTH_HIGHEST;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
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
 * This pattern receives data from TouchDesigner over NDI. The NDI data is supposed to be already
 * mapped to the TE pixels.
 *
 * <p>This class assumes the model of the car is already written out by
 * titanicsend.pattern.jon.ModelFileWriter and the model is used in TouchDesigner to map the texture
 * to the pixels.
 *
 * <p>This class is listening to the NDI Channel Name: "TD_Mapped_TE" for data.
 */
@LXCategory("NDI")
public class TdNdiPattern extends TEPerformancePattern {

  protected NDIEngine ndiEngine;

  protected DevolayVideoFrame videoFrame;
  protected DevolayReceiver receiver = null;
  private ByteBuffer frameData;

  protected boolean lastConnectState = false;
  protected long connectTimer = 0;
  protected String channel_name = "TE_TD_Mapped";

  LXPoint[] points = null;

  public TdNdiPattern(LX lx) {
    this(lx, TEShaderView.ALL_POINTS);
  }

  public TdNdiPattern(LX lx, TEShaderView view) {
    super(lx, view);
    this.ndiEngine = NDIEngine.get();

    // Create frame objects to handle incoming video stream
    // (note that we are omitting audio and metadata frames for now)
    this.videoFrame = new DevolayVideoFrame();

    // default common controls settings.  Note that these aren't committed
    // until the pattern calls addCommonControls(), so patterns can
    // override these settings if they need to.
    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.SPEED));
    controls.markUnused(controls.getLXControl(TEControlTag.XPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.YPOS));
    controls.markUnused(controls.getLXControl(TEControlTag.SIZE));
    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
    controls.markUnused(controls.getLXControl(TEControlTag.SPIN));
    controls.markUnused(controls.getLXControl(TEControlTag.BRIGHTNESS));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));
    controls.markUnused(controls.getLXControl(TEControlTag.ANGLE));
    addCommonControls();

    changeChannel();
  }

  protected void changeChannel() {
    if (this.receiver != null) {
      this.lastConnectState = this.ndiEngine.connectByName(this.channel_name, this.receiver);
    }
  }

  public void runTEAudioPattern(double deltaMs) {
    // Periodically try to connect if we weren't able to
    // establish an initial connection to this pattern's
    // desired NDI source. This makes things work
    // without manual intervention if the source isn't
    // available at startup.  Once the source becomes available,
    // the connection will be automatically established.
    if (!this.lastConnectState) {
      if (System.currentTimeMillis() - this.connectTimer > 1000) {
        this.connectTimer = System.currentTimeMillis();
        this.lastConnectState = this.ndiEngine.connectByName(this.channel_name, this.receiver);
      }
    }

    if (DevolayFrameType.VIDEO == this.receiver.receiveCapture(this.videoFrame, null, null, 0)) {
      // get pixel data from video frame
      this.frameData = this.videoFrame.getData();
      this.frameData.rewind();
      this.frameData.order(ByteOrder.LITTLE_ENDIAN);

      for (LXPoint p : lx.getModel().points) {
        int i = p.index * 4;
        colors[p.index] =
                LXColor.rgb(
                        this.frameData.get(i + 2), this.frameData.get(i + 1), this.frameData.get(i));
      }
    } else if (this.frameData != null) {
      // If no data was received since last frame, just show the last frame again on the car
      // to avoid flickers. This can cause a frozen frame being shown on the car instead of
      // a complete off screen when NDI is not being transferred on a good network.
      this.frameData.rewind();
      this.frameData.order(ByteOrder.LITTLE_ENDIAN);
      for (LXPoint p : lx.getModel().points) {
        int i = p.index * 4;
        colors[p.index] =
                LXColor.rgb(
                        this.frameData.get(i + 2), this.frameData.get(i + 1), this.frameData.get(i));
      }
    }
  }

  @Override
  public void onActive() {
    super.onActive();
    // if no receiver yet, create one, then connect it to
    // its saved source if possible.
    if (this.receiver == null) {
      this.receiver =
              new DevolayReceiver(
                      DevolayReceiver.ColorFormat.BGRX_BGRA, RECEIVE_BANDWIDTH_HIGHEST, true, "TE");
    }
    this.lastConnectState = this.ndiEngine.connectByName(this.channel_name, this.receiver);
  }

  @Override
  public void onInactive() {
    // disconnect receiver from all sources
    this.receiver.connect(null);
    super.onInactive();
  }

  @Override
  public void dispose() {
    // shut down receiver and free everything
    // we allocated.
    if (this.videoFrame != null) {
      this.videoFrame.close();
    }
    if (this.receiver != null) {
      this.receiver.connect(null);
    }

    super.dispose();
  }
}