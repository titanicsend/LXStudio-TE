package titanicsend.pattern.pixelblaze;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

public class Wrapper {
  private static String getJsFromFile(String pbClass) throws IOException {
    return Files.readString(Path.of("resources/pixelblaze/" + pbClass + ".js"));
  }

  public static Wrapper fromResource(String pbClass, int pixelCount) throws Exception {
    return new Wrapper(new File("resources/pixelblaze/" + pbClass + ".js"), pixelCount);
  }

  File file;
  int pixelCount;
  long lastModified;
  ScriptEngine engine;
  Invocable invocable;
  String renderName;

  public Wrapper(File file, int pixelCount) throws ScriptException, IOException {
    this.file = file;
    this.pixelCount = pixelCount;
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
    engine.eval(getJsFromFile("glue"));
    engine.eval(js);

    if (engine.get("render3D") != null) {
      renderName = "render3D";
    } else if (engine.get("render2D") != null) {
      renderName = "render2D";
    } else {
      renderName = "render";
    }
  }

  public void beforeRender(double deltaMs) throws ScriptException, NoSuchMethodException, IOException {
    engine.put("pixelCount", pixelCount);
    invocable.invokeFunction("beforeRender", deltaMs);
  }

  public void render(LXPoint point) throws ScriptException, NoSuchMethodException {
    this.invocable.invokeFunction(renderName, point.index, point.x, point.y, point.z);
  }

}
