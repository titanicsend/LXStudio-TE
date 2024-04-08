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

    public TdNdiPattern(LX lx) {
        this(lx, TEShaderView.ALL_POINTS);
    }

    public TdNdiPattern(LX lx, TEShaderView view) {
        super(lx, view);
        ndiEngine = NDIEngine.get();

        // Create frame objects to handle incoming video stream
        // (note that we are omitting audio and metadata frames for now)
        videoFrame = new DevolayVideoFrame();

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

        if (DevolayFrameType.VIDEO == receiver.receiveCapture(videoFrame, null, null, 0)) {
            // get pixel data from video frame
            frameData = videoFrame.getData();
            frameData.rewind();
            frameData.order(ByteOrder.LITTLE_ENDIAN);

            for (LXPoint p : this.model.points) {
                int i = p.index * 4;
                colors[p.index] = LXColor.rgb(frameData.get(i + 2), frameData.get(i + 1), frameData.get(i));
            }
        } else if (frameData != null) {
            // If no data was received since last frame, just show the last frame again on the car
            // to avoid flickers. This can cause a frozen frame being shown on the car instead of
            // a complete off screen when NDI is not being transferred on a good network.
            frameData.rewind();
            frameData.order(ByteOrder.LITTLE_ENDIAN);
            for (LXPoint p : this.model.points) {
                int i = p.index * 4;
                colors[p.index] = LXColor.rgb(frameData.get(i + 2), frameData.get(i + 1), frameData.get(i));
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
        }
        lastConnectState = ndiEngine.connectByName(channel_name, receiver);
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

        super.dispose();
    }
}
