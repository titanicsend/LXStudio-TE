package titanicsend.output;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import heronarts.lx.output.ArtNetDatagram;
import titanicsend.model.TEModel;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static java.lang.Math.min;

public class TEArtNetOutput {
  // Limit packets to 170 pixels per Universe, to stay within DMX's 512-byte limit
  public static final int MAX_PIXELS_PER_UNIVERSE = 170;

  private static class SubModelEntry {
    TEModel subModel;
    int universeNum;
    int strandOffset;
    boolean fwd;
    public SubModelEntry(TEModel subModel, int universeNum, int strandOffset, boolean fwd) {
      this.subModel = subModel;
      this.universeNum = universeNum;
      this.strandOffset = strandOffset;
      this.fwd = fwd;
    }
  }

  String ipAddress;
  static Map<String, TEArtNetOutput> ipMap = new HashMap<>();
  private final List<SubModelEntry> subModelEntries;
  private boolean activated;

  private TEArtNetOutput(String ipAddress) {
    this.ipAddress = ipAddress;
    this.subModelEntries = new ArrayList<>();
    this.activated = false;
  }

  public static TEArtNetOutput getOrMake(String ipAddress) {
    if (!ipMap.containsKey(ipAddress)) {
      ipMap.put(ipAddress, new TEArtNetOutput(ipAddress));
    }
    return ipMap.get(ipAddress);
  }

  public static void registerSubmodel(TEModel subModel, String ipAddress, int deviceNum,
                                      int strandOffset, boolean fwd) {
    assert deviceNum >= 1;
    //assert deviceNum <= 4;
    assert strandOffset >= 0;
    TEArtNetOutput output = getOrMake(ipAddress);
    assert !output.activated;
    output.subModelEntries.add(new SubModelEntry(subModel, deviceNum, strandOffset, fwd));
  }

  // Sort by device number, then by strand offset
  private static class SortSubModelEntries implements Comparator<SubModelEntry> {
    public int compare(SubModelEntry a, SubModelEntry b) {
      if (a.universeNum != b.universeNum) {
        return a.universeNum - b.universeNum;
      } else {
        return a.strandOffset - b.strandOffset;
      }
    }
  }

  private static int registerOutput(LX lx, InetAddress addr, List<Integer> indexBuffer, int universe) {
    int size = indexBuffer.size();
    if (size <= 0) return universe;
    assert universe != 0;
    //LX.log("New output with " + size + " pixels");
    while (size > 0) {
      int numPixels = min(size, MAX_PIXELS_PER_UNIVERSE);
      int[] ib = indexBuffer.subList(0, numPixels).stream().mapToInt(i -> i).toArray();
      int remaining = size - numPixels;
      size -= numPixels;
      ArtNetDatagram outputDevice = new ArtNetDatagram(lx, ib, universe);
      outputDevice.setAddress(addr);
      /*
      StringBuilder builder = new StringBuilder();
      for (int i : ib) {
        builder.append(i);
        builder.append(" ");
      }
      String ibStr = builder.toString();
      LX.log("New output at " + addr + " universe " + universe + " with " + ib.length + " pixels: " + ibStr);
      */
      lx.addOutput(outputDevice);
      if (size > 0) indexBuffer = indexBuffer.subList(numPixels, numPixels + remaining);
      universe++;
      int channel = universe / 10;
      int subChannel = universe % 10;
      // On our controllers, Channel 1 is universes 10-12, 2 is 20-22, etc. For long outputs,
      // we need to roll to the next available universe.
      if (subChannel > 2) {
        subChannel = 0;
        channel++;
      }
      if (channel > 8) {
        LX.error("Ran out of room to grow " + addr);
        return -1;
      }
      universe = 10 * channel + subChannel;
    }
    return universe;
  }

  private String pixString(int numPix) {
    if (numPix == 0) return "";
    else return " {" + numPix + "pix}";
  }

  private void activate(LX lx, int gapPointIndex) {
    assert !this.activated;
    this.subModelEntries.sort(new SortSubModelEntries());
    int currentUniverseNum = 0;
    int currentStrandOffset = -1;

    InetAddress addr;
    try {
      addr = InetAddress.getByName(this.ipAddress);
    } catch (UnknownHostException e) {
      throw new Error(e);
    }

    StringBuilder logString = new StringBuilder("ArtNet " + this.ipAddress + ": ");
    ArrayList<Integer> indexBuffer = new ArrayList<>();
    for (SubModelEntry subModelEntry : this.subModelEntries) {
      int numPoints = subModelEntry.subModel.points.length;
      if (subModelEntry.universeNum > currentUniverseNum) {
        int nextUniverse = registerOutput(lx, addr, indexBuffer, currentUniverseNum);
        if (nextUniverse < 0) return;  // Already logged the error
        if (nextUniverse > subModelEntry.universeNum) {
          LX.error("Rollover from previous output takes us to universe " + nextUniverse +
                  " but that's past configured " + this.ipAddress + ":" + subModelEntry.universeNum);
          return;
        }
        currentUniverseNum = subModelEntry.universeNum;
        currentStrandOffset = 0;
        String deviceSummary = "#" + currentUniverseNum + " ";
        logString.append(pixString(indexBuffer.size()));
        logString.append(deviceSummary);
        indexBuffer = new ArrayList<>();
      }
      assert subModelEntry.universeNum == currentUniverseNum;

      int gap = subModelEntry.strandOffset - currentStrandOffset;
      if (gap < 0) {
        throw new Error(subModelEntry.subModel.repr() + " offset must be >= " + currentStrandOffset);
      } else if (gap > 0) {
        String gapSummary = "[Gap=" + gap + "] ";
        logString.append(gapSummary);
        currentStrandOffset += gap;
        for (int i = 0; i < gap; i++) indexBuffer.add(gapPointIndex);
      }
      String rStr = subModelEntry.fwd ? "" : "(r)";
      String smSummary = "[" + currentStrandOffset + ":" + rStr + subModelEntry.subModel.repr() + "=" + numPoints + "] ";
      logString.append(smSummary);
      currentStrandOffset += numPoints;
      for (int i = 0; i < subModelEntry.subModel.points.length; i++) {
        LXPoint point;
        if (subModelEntry.fwd) point = subModelEntry.subModel.points[i];
        else point = subModelEntry.subModel.points[subModelEntry.subModel.points.length - i - 1];
        indexBuffer.add(point.index);
      }
    }

    logString.append(pixString(indexBuffer.size()));

    // We did this in the loop when we changed universes, but there might be one left at the end
    registerOutput(lx, addr, indexBuffer, currentUniverseNum);

    LX.log(logString.toString());
    this.activated = true;
  }

  public static void activateAll(LX lx, int gapPointIndex) {
    List<String> ips = new ArrayList<>(ipMap.keySet());
    Collections.sort(ips);
    for (String ip : ips) {
      ipMap.get(ip).activate(lx, gapPointIndex);
    }
  }
}
