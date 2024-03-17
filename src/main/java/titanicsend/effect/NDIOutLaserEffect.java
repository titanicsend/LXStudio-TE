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

@LXCategory("Titanics End")
public class NDIOutLaserEffect extends TEEffect {

  private boolean isInitialized = false;

  // output frame size
  private static final int width = 40;
  private static final int height = 40;

  private DevolaySender ndiSender;
  private DevolayVideoFrame ndiFrame;
  private final ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

  private LXPoint[] laser_points;

  public NDIOutLaserEffect(LX lx) {
    super(lx);
    laser_points =
        ModelFileWriter.get_points_along_the_path(ModelFileWriter.getFullGraphEdges().get(0));
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
      ndiSender = new DevolaySender("TE Laser");
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
