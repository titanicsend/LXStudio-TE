package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import java.nio.ByteBuffer;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import me.walkerknapp.devolay.DevolaySender;
import me.walkerknapp.devolay.DevolayVideoFrame;

@LXCategory("Titanics End")
public class NDIOutRawEffect extends TEEffect {

  private boolean isInitialized = false;

  // output frame size
  private static final int width = 320;
  private static final int height = 240;

  private DevolaySender ndiSender;
  private DevolayVideoFrame ndiFrame;
  private final ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);

  public NDIOutRawEffect(LX lx) {
    super(lx);
  }

  @Override
  protected void run(double deltaMs, double enabledAmount) {
      if ( !isInitialized ) {
        return;
      }

      buffer.rewind();
      for (LXPoint p : this.model.points) {
        buffer.putInt(colors[p.index] );
      }
      buffer.flip();
      ndiSender.sendVideoFrame(ndiFrame);
  }

  @Override
  protected void onEnable() {
    super.onEnable();

    if (!isInitialized) {
      ndiSender = new DevolaySender("TitanicsEnd");
      ndiFrame = new DevolayVideoFrame();
      ndiFrame.setResolution(width,height);
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
