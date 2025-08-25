package titanicsend.pattern.pixelblaze;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class UrlFetcher {
  public static String fetch(String url) throws Exception {
    StringBuilder content = new StringBuilder();
    URLConnection urlConnection = new URL(url).openConnection();
    BufferedReader bufferedReader =
        new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
    String line;
    while ((line = bufferedReader.readLine()) != null) {
      content.append(line + "\n");
    }
    bufferedReader.close();
    return content.toString();
  }
}
