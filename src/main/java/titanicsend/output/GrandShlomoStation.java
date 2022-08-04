package titanicsend.output;

import heronarts.lx.LX;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

public class GrandShlomoStation {

  // Using a map like a set because Java doesn't have Set.get(T) returning T
  private static Map<ChromatechSocket, ChromatechSocket> outputs = new HashMap<>();

  public static ChromatechSocket getOrMake(String ipStr, int channelNum) {
    InetAddress ip;
    try {
      ip = InetAddress.getByName(ipStr);
    } catch (UnknownHostException e) {
      throw new Error(e);
    }
    ChromatechSocket output = new ChromatechSocket(ip, channelNum);
    if (!outputs.containsKey(output)) outputs.put(output, output);
    return outputs.get(output);
  }

  public static void activateAll(LX lx, int gapPointIndex) {
    List<ChromatechSocket> sockets = new ArrayList<>(outputs.keySet());
    Collections.sort(sockets);
    for (ChromatechSocket socket : sockets) {
      socket.activate(lx, gapPointIndex);
    }
  }
}
