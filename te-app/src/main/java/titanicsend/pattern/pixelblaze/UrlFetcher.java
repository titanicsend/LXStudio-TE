package titanicsend.pattern.pixelblaze;

import java.io.*;
import java.net.*;

public class UrlFetcher {
  public static String fetch(String url) throws Exception {
    StringBuilder content = new StringBuilder();
    URLConnection urlConnection = new URI(url).toURL().openConnection();

    BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
    String line;
    while ((line = bufferedReader.readLine()) != null) {
      content.append(line).append("\n");
    }
    bufferedReader.close();
    return content.toString();
  }
}
