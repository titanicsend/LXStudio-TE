package titanicsend.pattern.pixelblaze;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import titanicsend.util.TEColor;

public class Wrapper {

  private static String getJsFromFile(String pbClass) throws IOException {
    return Files.readString(Path.of("resources/pixelblaze/" + pbClass + ".js"));
  }

  public static Wrapper fromResource(String pbClass, LXPoint[] points, int[] colors) throws Exception {
    return new Wrapper(new File("resources/pixelblaze/" + pbClass + ".js"), points, colors);
  }

  File file;
  LXPoint[] points;
  int[] colors;
  int pixelCount;
  long lastModified;
  ScriptEngine engine;
  Invocable invocable;
  String renderName;

  public Wrapper(File file, LXPoint[] points, int[] colors) throws ScriptException, IOException {
    this.file = file;
    this.points = points;
    this.colors = colors;
    this.pixelCount = points.length;
    load();
  }

  public void reloadIfNecessary() throws ScriptException, IOException {
    if (file.lastModified() != lastModified) {
      LX.log("Reloading pattern");
      load();
    }
  }

  void load() throws IOException, ScriptException {
    String js = Files.readString(file.toPath());
    lastModified = file.lastModified();

    js = js.replaceAll("\\bexport\\b", "");

    NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
    engine = factory.getScriptEngine("--language=es6");
    invocable = (Invocable)engine;

    engine.put("pixelCount", pixelCount);
    engine.put("__now", System.currentTimeMillis());
    engine.eval(getJsFromFile("glue"));
    engine.eval(js);
  }

  public void render(double deltaMs) throws ScriptException, NoSuchMethodException, IOException {
    engine.put("pixelCount", pixelCount);
    engine.put("__now", System.currentTimeMillis());
    engine.put("__points", points);
    engine.put("__colors", colors);

    invocable.invokeFunction("beforeRender", deltaMs);
    invocable.invokeFunction("glueRender");
  }

}
