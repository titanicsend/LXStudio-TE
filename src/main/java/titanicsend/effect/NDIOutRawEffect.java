/**
 * Copyright 2020- Mark C. Slee, Heron Arts LLC
 *
 * <p>This file is part of the LX Studio software library. By using LX, you agree to the terms of
 * the LX Studio Software License and Distribution Agreement, available at: http://lx.studio/license
 *
 * <p>Please note that the LX license is not open-source. The license allows for free,
 * non-commercial use.
 *
 * <p>HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, AND SPECIFICALLY
 * DISCLAIMS ANY WARRANTY OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE,
 * WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */
package titanicsend.effect;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import me.walkerknapp.devolay.DevolayFrameFourCCType;
import me.walkerknapp.devolay.DevolaySender;
import me.walkerknapp.devolay.DevolayVideoFrame;
import titanicsend.pattern.glengine.GLShaderEffect;

import java.nio.ByteBuffer;

@LXCategory("Titanics End")
public class NDIOutRawEffect extends TEEffect {

  private boolean isInitialized = false;

  protected static final int width = 320;
  protected static final int height = 240;

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
        //buffer.putInt((k << 8) | (k >>> 24));
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
      ndiFrame.setResolution(height,width);
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
