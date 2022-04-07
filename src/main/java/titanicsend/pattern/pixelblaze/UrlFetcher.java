package titanicsend.pattern.pixelblaze;

import heronarts.lx.LX;

import java.net.*;
import java.io.*;

public class UrlFetcher {
  public static String fetch(String url) throws Exception {
    StringBuilder content = new StringBuilder();
    URLConnection urlConnection = new URL(url).openConnection();
    BufferedReader bufferedReader = new BufferedReader(
            new InputStreamReader(urlConnection.getInputStream()));
    String line;
    while ((line = bufferedReader.readLine()) != null) {
      content.append(line + "\n");
    }
    bufferedReader.close();
    return content.toString();
  }
}
