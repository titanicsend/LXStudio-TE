package titanicsend.pattern.pixelblaze;

import javax.script.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.TEPattern;

public class Wrapper {

  //NOTE these are thread-safe, if used with separate bindings
  //https://stackoverflow.com/a/30159424/910094
//  static final ScriptEngine engine;
//  static final Invocable invocable;
//  static final Compilable compilingEngine;
//  static HashMap<String, CompiledScript> scripts = new HashMap<>();
//  static {
//    NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
//    engine = factory.getScriptEngine("--language=es6");
//    invocable = (Invocable)engine;
//    compilingEngine = (Compilable) engine;
//  }
//
//  static synchronized CompiledScript compile(String pbClass) {
//    File file = new File("resources/pixelblaze/" + pbClass + ".js");
//    String js = Files.readString(file.toPath());
//    lastModified = file.lastModified();
//    js = js.replaceAll("\\bexport\\b", "");
//    return compilingEngine.compile(js);
//  }


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

  /**
   * Updates the points that the pattern will operate on, reloading if necessary.
   * @param points
   * @throws ScriptException
   * @throws IOException
   * @throws NoSuchMethodException
   */
  public void setPoints(LXPoint[] points) throws ScriptException, IOException, NoSuchMethodException {
    if (this.points == points)
      return;
    this.points = points;
    load();
  }

}
