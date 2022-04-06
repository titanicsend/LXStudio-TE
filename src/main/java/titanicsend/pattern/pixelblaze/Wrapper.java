package titanicsend.pattern.pixelblaze;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URLEncoder;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class Wrapper {
  private static String getJsFromWeb(String pbClass) throws Exception {
    String url = "https://raw.githubusercontent.com/jvyduna/pb-examples/master/src/"
            + URLEncoder.encode(pbClass).replaceAll("\\+", "\\%20") + ".js";
    return UrlFetcher.fetch(url);
  }

  private static FileReader getJsFromFile(String pbClass) throws FileNotFoundException {
    return new FileReader("resources/pixelblaze/" + pbClass + ".js");
  }

  public static Invocable makeInvocable(String pbClass) throws Exception {
    String js = getJsFromWeb(pbClass);
    NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
    ScriptEngine engine = factory.getScriptEngine();
    js = js.replaceAll("\\bexport\\b", "");
    engine.eval(getJsFromFile("glue"));
    engine.eval(js);
    return (Invocable) engine;
  }
}
