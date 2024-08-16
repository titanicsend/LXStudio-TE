package titanicsend.pattern.sinas;

import static me.walkerknapp.devolay.DevolayReceiver.RECEIVE_BANDWIDTH_HIGHEST;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import me.walkerknapp.devolay.DevolayFrameType;
import me.walkerknapp.devolay.DevolayReceiver;
import me.walkerknapp.devolay.DevolayVideoFrame;
import titanicsend.ndi.NDIEngine;
import titanicsend.osc.CrutchOSC;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TE;

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
public class TdStableDiffusionPattern extends TEPerformancePattern {

    private class FocusedChannelListener implements LXParameterListener {
        @Override
        public void onParameterChanged(LXParameter parameter) {
            TE.log("Focused Channel Changed: " + parameter.getLabel());
        }
    }

    private class FocusedChannelAuxListener implements LXParameterListener {
        @Override
        public void onParameterChanged(LXParameter parameter) {
            TE.log("Focused Channel Aux Changed: " + parameter.getLabel());
        }
    }

    private static int reset_signal = -1;
    private final String SD_CHANNELS_RESET = "/lx/control/reset_channels";

    private final String PRIMARY_BASE_ADDRESS = "/lx/pattern";
    private final String SD_CHANNEL_NAME = "/sd_channel";
    private final String SD_PATTERN_NAME = "/sd_pattern";

    public final String SD_ACTIVE_CHANNEL = PRIMARY_BASE_ADDRESS + SD_CHANNEL_NAME;
    public final String SD_ACTIVE_PATTERN = PRIMARY_BASE_ADDRESS + SD_PATTERN_NAME;

    private final float NOT_ACTIVE = -1;
    private int channelNumber = -1;
    private int patternNumber = -1;

    private final String SD_FOCUSED_CHANNEL = "/lx/pattern/stable_diffusion_channel";
    private final String SD_FOCUSED_PATTERN = "/lx/pattern/stable_diffusion_pattern";

    protected NDIEngine ndiEngine;

    protected DevolayVideoFrame videoFrame;
    protected DevolayReceiver receiver = null;
    private ByteBuffer frameData;

    protected boolean lastConnectState = false;
    protected long connectTimer = 0;
    protected String channel_name = "TE_TD_Mapped";

    private CrutchOSC osc = null;

    LXPoint[] points = null;

    public TdStableDiffusionPattern(LX lx) {
        this(lx, TEShaderView.ALL_POINTS);
    }

    public TdStableDiffusionPattern(LX lx, TEShaderView view) {
        super(lx, view);

        // Listen and fire immediately
        lx.engine.mixer.focusedChannel.addListener(new FocusedChannelListener(), true);
        lx.engine.mixer.focusedChannelAux.addListener(new FocusedChannelAuxListener(), true);

        ndiEngine = NDIEngine.get();

        // Create frame objects to handle incoming video stream
        // (note that we are omitting audio and metadata frames for now)
        videoFrame = new DevolayVideoFrame();

        // default common controls settings.  Note that these aren't committed
        // until the pattern calls addCommonControls(), so patterns can
        // override these settings if they need to.
        addCommonControls();

        osc = CrutchOSC.get();

        // Firstly, pulse the reset signal for touchdesigner's state.
        osc.sendOscMessage(SD_CHANNELS_RESET, reset_signal++);

        // Then send OSC to say this pattern is disabled, and we will update the value when the pattern
        // is active.
        osc.sendOscMessage(SD_ACTIVE_CHANNEL, NOT_ACTIVE);
        osc.sendOscMessage(SD_ACTIVE_PATTERN, NOT_ACTIVE);

        changeChannel();
    }

    protected void initChannelAndPatternNumbers() {
        if (channelNumber != -1 && patternNumber != -1) return;
        String oscAddress = getOscAddress(); // E.g., /lx/mixer/channel/3/pattern/2

        // Extract numbers using regular expressions
        String[] numbers = oscAddress.split("[^0-9]+");
        // Convert the extracted numbers to integers
        channelNumber = Integer.parseInt(numbers[1]);
        patternNumber = Integer.parseInt(numbers[2]);
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

            for (LXPoint p : lx.getModel().points) {
                int i = p.index * 4;
                colors[p.index] = LXColor.rgb(frameData.get(i + 2), frameData.get(i + 1), frameData.get(i));
            }
        } else if (frameData != null) {
            // If no data was received since last frame, just show the last frame again on the car
            // to avoid flickers. This can cause a frozen frame being shown on the car instead of
            // a complete off screen when NDI is not being transferred on a good network.
            frameData.rewind();
            frameData.order(ByteOrder.LITTLE_ENDIAN);
            for (LXPoint p : lx.getModel().points) {
                int i = p.index * 4;
                colors[p.index] = LXColor.rgb(frameData.get(i + 2), frameData.get(i + 1), frameData.get(i));
            }
        }
    }

    @Override
    public void onActive() {
        super.onActive();
        initChannelAndPatternNumbers();

        osc.sendOscMessage(SD_ACTIVE_CHANNEL, channelNumber);
        osc.sendOscMessage(SD_ACTIVE_PATTERN, patternNumber);

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
        super.onInactive();

        // Communicate to outside world that this pattern is not selected anymore.
        osc.sendOscMessage(SD_ACTIVE_CHANNEL, NOT_ACTIVE);
        osc.sendOscMessage(SD_ACTIVE_PATTERN, NOT_ACTIVE);

        // disconnect receiver from all sources
        receiver.connect(null);
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

        // On dispose, pulse the reset signal.
        osc.sendOscMessage(SD_CHANNELS_RESET, reset_signal++);

        super.dispose();
    }
}
