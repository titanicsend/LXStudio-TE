package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import me.walkerknapp.devolay.DevolaySender;
import me.walkerknapp.devolay.DevolayVideoFrame;
import titanicsend.pattern.jon.ModelFileWriter;

@LXCategory("NDI")
public class NDIOutLaserEffect extends TEEffect {

    private boolean isInitialized = false;

    private final String channelName = "TE Laser";

    // output frame size
    private static final int width = 40;
    private static final int height = 40;

    private DevolaySender ndiSender;
    private DevolayVideoFrame ndiFrame;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

    private final LXPoint[] laser_points;

    public NDIOutLaserEffect(LX lx) {
        super(lx);
        laser_points =
                ModelFileWriter.interpolateVerticesToPoints(ModelFileWriter.getLaserEdgesProgression().get(0), 10);
    }

    @Override
    protected void run(double deltaMs, double enabledAmount) {
        if (!isInitialized) {
            return;
        }

        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.rewind();
        for (LXPoint p : laser_points) {
            buffer.putInt(colors[p.index]);
        }
        buffer.flip();
        ndiSender.sendVideoFrame(ndiFrame);
    }

    @Override
    protected void onEnable() {
        super.onEnable();

        if (!isInitialized) {
            ndiSender = new DevolaySender(channelName);
            ndiFrame = new DevolayVideoFrame();
            ndiFrame.setResolution(width, height);
            ndiFrame.setFourCCType(DevolayFrameFourCCType.BGRA);
            ndiFrame.setData(buffer);
            ndiFrame.setFrameRate(60, 1);
            ndiFrame.setAspectRatio(1);
            isInitialized = true;
        }
    }

    @Override
    protected void onDisable() {
        super.onDisable();
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}