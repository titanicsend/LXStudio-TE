package titanicsend.pattern.pixelblaze;

import heronarts.lx.LX;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEAudioPattern;

import javax.script.ScriptException;
import java.util.HashMap;

public class PixelblazePattern extends TEAudioPattern {
  public static final int RENDER_ERROR_LOG_INTERVAL_MS = 5_000;
  private Wrapper wrapper;
  long lastLogMs = 0; //to prevent spamming the logs with script errors
  HashMap<String, LXParameter> patternParameters = new HashMap<>();

  /**
   * This should be overridden in subclasses to load a different source
   * file in the resources/pixelblaze directory. The '.js' extension is added.
   * @return
   */
  protected String getScriptName() {
    return "test";
  }

  public PixelblazePattern(LX lx) {
    super(lx);

    try {
      wrapper = Wrapper.fromResource(getScriptName(), this, model.points, colors);
      wrapper.load();
    } catch (Exception e) {
      LX.log("Error initializing Pixelblaze script:" + e.getMessage());
    }
  }

  /**
   * Used by the glue to register discovered slider controls
   * @param key
   * @param label
   */
  public void addSlider(String key, String label) {
    if (parameters.containsKey(key)) {
      return;
    }
    LX.log("Pixelblaze adding parameter for " + label);
    CompoundParameter energy = new CompoundParameter(label, .5, 0, 1);
    addParameter(key, energy);
    patternParameters.put(key, energy);
  }

  /**
   * Used by the glue to invoke slider controls
   * @param key
   * @return
   */
  public double getSlider(String key) {
    LXParameter parameter = parameters.get(key);
    if (parameter != null) {
      return parameter.getValue();
    }
    return 0;
  }

  public void runTEAudioPattern(double deltaMs) {
    if (wrapper == null)
      return;

    try {
      updateGradients();
      wrapper.reloadIfNecessary();
      wrapper.render(deltaMs);
    } catch (ScriptException | NoSuchMethodException sx) {
      //the show must go on, and we don't want to spam the logs.
      if (System.currentTimeMillis() - lastLogMs > RENDER_ERROR_LOG_INTERVAL_MS) {
        LX.log("Error rendering Pixelblaze script:" + sx.getMessage());
        lastLogMs = System.currentTimeMillis();
      }
    } catch (Exception e) {
      e.printStackTrace();
      LX.error(e);
      return;
    }
  }
}