package titanicsend.output;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import heronarts.lx.output.ArtNetDatagram;
import titanicsend.model.TEEdgeModel;
import titanicsend.model.TEPanelModel;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.lang.Math.min;

public class ChromatechSocket implements Comparable<ChromatechSocket> {
  public static final int MAX_PIXELS_PER_CHANNEL = 510;
  public static final int PIXELS_PER_UNIVERSE = 170;

  InetAddress ip;
  int channelNum;
  private boolean activated;
  private List<EdgeLink> edgeLinks;
  private TEPanelModel panel;
  private int firstPanelPixel;
  private int lastPanelPixel;

  @Override
  public int hashCode() {
    return ip.hashCode() * 100 + channelNum;
  }

  @Override
  public boolean equals(Object obj) {
    ChromatechSocket that = (ChromatechSocket) obj;
    return this.ip == that.ip && this.channelNum == that.channelNum;
  }

  @Override
  public int compareTo(ChromatechSocket that) {
    byte[] thisIp = this.ip.getAddress();
    byte[] thatIp = that.ip.getAddress();
    for (int i = 0; i < 4; i++) {
      if (thisIp[i] != thatIp[i]) return thisIp[i] - thatIp[i];
    }
    return this.channelNum - that.channelNum;
  }

  public ChromatechSocket(InetAddress ip, int channelNum) {
    assert channelNum >= 1;
    assert channelNum <= 8;

    this.ip = ip;
    this.channelNum = channelNum;
    this.activated = false;
    this.edgeLinks = new ArrayList<>();
    this.panel = null;
    this.firstPanelPixel = -1;
    this.lastPanelPixel = -1;
  }

  private static class EdgeLink {
    TEEdgeModel edge;
    int strandOffset;
    boolean fwd;
    private EdgeLink(TEEdgeModel edge, int strandOffset, boolean fwd) {
      this.edge = edge;
      this.strandOffset = strandOffset;
      this.fwd = fwd;
    }
  }

  public void addPanel(TEPanelModel panel, int firstPanelPixel, int lastPanelPixel) {
    assert !this.activated;

    if (this.panel != null) {
      throw new IllegalArgumentException("This slot already has a panel mapped");
    }
    if (!this.edgeLinks.isEmpty()) {
      throw new IllegalArgumentException("This slot already has edges mapped");
    }
    this.panel = panel;
    this.firstPanelPixel = firstPanelPixel;
    this.lastPanelPixel = lastPanelPixel;
  }

  public void addEdge(TEEdgeModel edge, int strandOffset, boolean fwd) {
    assert !this.activated;

    if (this.panel != null) {
      throw new IllegalArgumentException("This slot already has a panel mapped");
    }
    int noobFirstPixel = strandOffset;
    int noobLastPixel = strandOffset + edge.size - 1;
    for (EdgeLink edgeLink : this.edgeLinks) {
      int existingFirstPixel = edgeLink.strandOffset;
      int existingLastPixel = existingFirstPixel + edgeLink.edge.size;
      if (existingFirstPixel <= noobLastPixel &&
              existingLastPixel >= noobFirstPixel)
        throw new IllegalArgumentException(edge.getId() + " overlaps " +
                edgeLink.edge.getId());
    }
    this.edgeLinks.add(new EdgeLink(edge, strandOffset, fwd));
  }

  private void registerUniverses(LX lx, List<Integer> multiUniverseIndexBuffer) {
    assert multiUniverseIndexBuffer.size() == MAX_PIXELS_PER_CHANNEL;
    assert MAX_PIXELS_PER_CHANNEL == PIXELS_PER_UNIVERSE * 3;

    for (int segment = 0; segment <= 2; segment++) {
      int universe = this.channelNum * 10 + segment;
      int first = segment * PIXELS_PER_UNIVERSE;
      int lastPlusOne = first + PIXELS_PER_UNIVERSE;
      int[] ib = multiUniverseIndexBuffer.subList(first, lastPlusOne)
              .stream().mapToInt(i -> i).toArray();
      ArtNetDatagram outputDevice = new ArtNetDatagram(lx, ib, universe);
      outputDevice.setAddress(this.ip);
      lx.addOutput(outputDevice);
    }
  }

  private static class SortEdgeLinks implements Comparator<EdgeLink> {
    public int compare(EdgeLink a, EdgeLink b) {
      return a.strandOffset - b.strandOffset;
    }
  }

  public void activate(LX lx, int gapPointIndex) {
    assert !this.activated;
    this.activated = true;

    StringBuilder logString = new StringBuilder(
            "ArtNet " + this.ip.getHostAddress() + "#" + this.channelNum);
    boolean isPanel = this.panel != null;
    boolean isEdgeLinks = !this.edgeLinks.isEmpty();
    List<Integer> multiUniverseIndexBuffer = new ArrayList<>();
    if (isPanel && !isEdgeLinks) {
      int i;
      int numPixels = this.lastPanelPixel - this.firstPanelPixel + 1;
      assert numPixels <= MAX_PIXELS_PER_CHANNEL;
      logString.append(" " + this.panel.repr() + "[" + this.firstPanelPixel + "-" +
              this.lastPanelPixel + "]");
      for (i = 0; i < numPixels; i++) {
        LXPoint point = this.panel.points[this.firstPanelPixel + i];
        multiUniverseIndexBuffer.add(point.index);
      }
      for (; i < MAX_PIXELS_PER_CHANNEL; i++) multiUniverseIndexBuffer.add(gapPointIndex);
    } else if (isEdgeLinks && !isPanel) {
      this.edgeLinks.sort(new SortEdgeLinks());
      for (EdgeLink edgeLink : this.edgeLinks) {
        // Add gaps until we reach this edge's spot in the strand
        int gap = 0;
        while (edgeLink.strandOffset < multiUniverseIndexBuffer.size()) {
          multiUniverseIndexBuffer.add(gapPointIndex);
          gap++;
        }
        if (gap > 0) logString.append(" [Gap=" + gap + "]");

        String rStr = edgeLink.fwd ? "" : "(r)";
        logString.append(" @" + edgeLink.strandOffset + ":" + edgeLink.edge.repr() +
                rStr + "=" + edgeLink.edge.points.length + "] ");

        for (int ei = 0; ei < edgeLink.edge.points.length; ei++) {
          LXPoint point;
          if (edgeLink.fwd) point = edgeLink.edge.points[ei];
          else point = edgeLink.edge.points[edgeLink.edge.points.length - ei - 1];
          multiUniverseIndexBuffer.add(point.index);
        }
      }
      while (multiUniverseIndexBuffer.size() < MAX_PIXELS_PER_CHANNEL) {
        multiUniverseIndexBuffer.add(gapPointIndex);
      }
    } else {
      throw new IllegalStateException();
    }
    LX.log(logString.toString());
    this.registerUniverses(lx, multiUniverseIndexBuffer);
  }
}