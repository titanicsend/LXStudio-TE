package titanicsend.output;

import heronarts.lx.LX;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.mixer.LXAbstractChannel;
import heronarts.lx.mixer.LXChannel;
import heronarts.lx.model.LXPoint;
import heronarts.lx.output.LXOutput;
import heronarts.lx.pattern.LXPattern;
import titanicsend.app.GigglePixelBroadcaster;
import titanicsend.pattern.TEPattern;

import java.util.ArrayList;
import java.util.List;

public class GPOutput extends LXOutput {
  private GigglePixelBroadcaster broadcaster;

  public GPOutput(LX lx, GigglePixelBroadcaster broadcaster) {
    super(lx);
    this.broadcaster = broadcaster;
  }

  @Override
  protected void onSend(int[] colors, byte[][] glut, double brightness) {
    int channelIndex = 0; // TODO: Expose in the UI

    List<LXAbstractChannel> abstractChannels = this.lx.engine.mixer.channels;
    List<LXChannel> channels = new ArrayList<>();
    for (LXAbstractChannel ac : abstractChannels) {
      if (ac instanceof LXChannel) {
        channels.add((LXChannel) ac);
      }
    }

    List<Integer> gpColors = new ArrayList<>();
    for (LXDynamicColor dc : lx.engine.palette.swatch.colors) {
      gpColors.add(dc.getColor());
    }
    this.broadcaster.setColors(gpColors);

    if (channelIndex >= channels.size()) return;
    
    List<LXPoint> points = null;
    while (channelIndex < channels.size()) {
      LXChannel channel = channels.get(channelIndex);
      LXPattern pattern = channel.getActivePattern();
      if (pattern instanceof TEPattern) {
    	  // Found a reference pattern.  Any one will do, we're just using it for indexes
    	  points = ((TEPattern) pattern).getGigglePixelPoints();
    	  break;
      }
      channelIndex++;
    }

    // Default to palette colors if no TEPattern is running
    if (points == null || points.isEmpty()) return;

    // At this point you're discarding the palette colors
    gpColors = new ArrayList<>();
    for (LXPoint point : points) {
      gpColors.add(colors[point.index]);
    }
    this.broadcaster.setColors(gpColors);
  }
}
