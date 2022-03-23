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
    LXChannel channel = channels.get(channelIndex);
    LXPattern pattern = channel.getActivePattern();
    if (!(pattern instanceof TEPattern)) return;
    TEPattern tePattern = (TEPattern) pattern;
    List<LXPoint> points = tePattern.getGigglePixelPoints();
    if (points.isEmpty()) return;

    gpColors = new ArrayList<>();
    for (LXPoint point : points) {
      gpColors.add(colors[point.index]);
    }
    this.broadcaster.setColors(gpColors);
  }
}
