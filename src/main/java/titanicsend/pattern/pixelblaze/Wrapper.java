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
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.TEPattern;

public class Wrapper {

  private static String getJsFromFile(String pbClass) throws IOException {
    return Files.readString(Path.of("resources/pixelblaze/" + pbClass + ".js"));
  }

  public static Wrapper fromResource(String pbClass, TEAudioPattern pattern, LXPoint[] points, int[] colors) throws Exception {
    return new Wrapper(new File("resources/pixelblaze/" + pbClass + ".js"), pattern, points, colors);
  }

  File file;
  TEAudioPattern pattern;
  LXPoint[] points;
  int[] colors;
  long lastModified;
  ScriptEngine engine;
  Invocable invocable;
  String renderName;
  boolean hasError = false;

  public Wrapper(File file, TEAudioPattern pattern, LXPoint[] points, int[] colors) throws ScriptException, IOException {
    this.file = file;
    this.pattern = pattern;
    this.points = points;
    this.colors = colors;
  }

  public void reloadIfNecessary() throws ScriptException, IOException, NoSuchMethodException {
    if (file.lastModified() != lastModified) {
      LX.log("Reloading pattern");
      load();
    }
  }

  public void load() throws IOException, ScriptException, NoSuchMethodException {
    try {
      String js = Files.readString(file.toPath());
      lastModified = file.lastModified();

      js = js.replaceAll("\\bexport\\b", "");

      NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
      engine = factory.getScriptEngine("--language=es6");
      invocable = (Invocable)engine;

      engine.put("pixelCount", points.length);
      engine.put("__pattern", pattern);
      engine.put("__now", System.currentTimeMillis());
      engine.eval(getJsFromFile("glue"));
      engine.eval(js);
      invocable.invokeFunction("glueRegisterControls");
      hasError = false;
    } catch (Throwable t) {
      hasError = true;
      throw t;
    }
  }

  public void render(double deltaMs) throws ScriptException, NoSuchMethodException {
    if (hasError)
      return;

    engine.put("__now", System.currentTimeMillis());
    engine.put("__points", points);
    engine.put("__colors", colors);

    invocable.invokeFunction("glueBeforeRender", deltaMs, System.currentTimeMillis(), points, colors);
    invocable.invokeFunction("glueRender");
  }

}
