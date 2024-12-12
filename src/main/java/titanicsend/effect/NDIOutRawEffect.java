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

      // TODO - make frame size adapt automatically to the size of the
      // TODO - current model.
      // Make sure we don't overrun the frame buffer if the model is too large
      int nPoints = Math.min(width * height, this.model.points.length);

      buffer.rewind();
      for (int i = 0; i < nPoints; i++) {
        // Move alpha channel to the low order byte so we wind up with ARGB
        // data that NDI can use.
        int k = colors[this.model.points[i].index];
        k = ((k >> 24) & 0xFF) | (k << 8);
        buffer.putInt(k);
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
      ndiFrame.setFourCCType(DevolayFrameFourCCType.RGBA);
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
    if (isInitialized) {
      ndiSender.close();
    }
    super.dispose();
  }
}
