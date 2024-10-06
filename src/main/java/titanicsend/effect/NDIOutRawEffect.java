package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import java.nio.ByteBuffer;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import me.walkerknapp.devolay.DevolaySender;
import me.walkerknapp.devolay.DevolayVideoFrame;
import titanicsend.pattern.glengine.GLEngine;

@LXCategory("Titanics End")
public class NDIOutRawEffect extends TEEffect {

  private boolean isInitialized = false;

  // output frame size
  private static final int width = GLEngine.getWidth();
  private static final int height = GLEngine.getHeight();

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
        // Move alpha channel to the low order byte so we wind up with ARGB
        // data that NDI can use.
        int k = colors[p.index];
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
