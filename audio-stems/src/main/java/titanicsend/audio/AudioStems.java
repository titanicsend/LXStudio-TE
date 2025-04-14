package titanicsend.audio;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.osc.LXOscListener;
import heronarts.lx.osc.OscMessage;
import heronarts.lx.parameter.BoundedFunctionalParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter.Units;
import heronarts.lx.parameter.ObjectParameter;
import heronarts.lx.utils.LXUtils;
import heronarts.lx.utils.ObservableList;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Top-level component for Audio Stems, runs as a child of LX Engine */
public class AudioStems extends LXComponent implements LXOscListener {

  private final ObservableList<Stem> mutableStems = new ObservableList<>();
  public final ObservableList<Stem> stems = mutableStems.asUnmodifiableList();

  private Stem[] selectorObjects = new Stem[] {null};
  private String[] selectorOptions = new String[] {null};
  private final List<Selector> selectors = new ArrayList<>();

  public final CompoundParameter gain =
      new CompoundParameter("Gain", 0, -1, 2).setUnits(Units.PERCENT_NORMALIZED);

  /**
   * By omitting 'public', this constructor is package-protected and can only be called from within
   * "titanicsend.audio".
   */
  AudioStems(LX lx) {
    super(lx, "audioStems");
    addParameter("gain", this.gain);
    loadConfig(lx);
    this.lx.engine.osc.addListener(this);
  }

  private void addStem(Stem stem) {
    this.mutableStems.add(stem);
    updateSelectors();
  }

  @SuppressWarnings("unused")
  private void removeStem(Stem stem) {
    if (!this.stems.contains(stem)) {
      throw new IllegalStateException("Cannot remove unknown stem: " + stem.label);
    }
    this.mutableStems.remove(stem);
    updateSelectors();
  }

  private void updateSelectors() {
    int numOptions = this.stems.size();
    this.selectorObjects = new Stem[numOptions];
    this.selectorOptions = new String[numOptions];
    for (int i = 0; i < numOptions; i++) {
      Stem stem = this.stems.get(i);
      this.selectorObjects[i] = stem;
      this.selectorOptions[i] = stem.label;
    }
    for (Selector selector : this.selectors) {
      final Stem selected = selector.getObject();
      selector.setObjects(this.selectorObjects, this.selectorOptions);
      if ((selected != selector.getObject()) && this.stems.contains(selected)) {
        selector.setValue(selected);
      }
    }
  }

  public Selector newSelector(String label, String description) {
    return (Selector) new Selector(label).setDescription(description);
  }

  @Override
  public void oscMessage(OscMessage message) {
    String oscPath = message.getAddressPattern().getValue();
    for (Stem stem : this.stems) {
      if (oscPath.equals(stem.oscPath)) {
        float value = message.getFloat();
        stem.rawParameter.setValue(value);
      }
    }
  }

  private static final String CONFIG_FILENAME = "audioStems.json";
  private static final String KEY_STEMS = "stems";

  private void loadConfig(LX lx) {
    File file = lx.getMediaFile(CONFIG_FILENAME);

    // On first run, copy the config file out of the package jar
    if (!file.exists()) {
      try (InputStream resourceStream = getClass().getResourceAsStream("/" + CONFIG_FILENAME)) {
        if (resourceStream == null) {
          throw new IllegalStateException(
              "Template config file " + CONFIG_FILENAME + " not found in package");
        }
        Files.copy(resourceStream, file.toPath());
      } catch (Exception e) {
        LOG.error(e, "Failed to copy default config file from resources: " + CONFIG_FILENAME);
      }
    }

    // Load from file
    if (file.exists()) {
      try (FileReader fr = new FileReader(file)) {
        JsonObject obj = new Gson().fromJson(fr, JsonObject.class);
        if (obj.has(KEY_STEMS)) {
          JsonArray stemsArray = obj.getAsJsonArray(KEY_STEMS);
          for (JsonElement stemElement : stemsArray) {
            JsonObject stemObj = stemElement.getAsJsonObject();
            String label = stemObj.get("label").getAsString();
            String oscPath = stemObj.get("oscPath").getAsString();
            Stem stem = new Stem(label, oscPath);
            addStem(stem);
          }
        }
      } catch (Exception x) {
        LOG.error(x, "Exception loading audio stems config file: " + file);
      }
    } else {
      LOG.error("Audio stems config file not found: " + file);
    }
  }

  @Override
  public void dispose() {
    this.lx.engine.osc.removeListener(this);
    super.dispose();
  }

  public class Stem {

    public final String label;
    public final String oscPath;
    public final BoundedParameter rawParameter;
    public final BoundedFunctionalParameter parameter;

    private Stem(String label, String oscPath) {
      if (LXUtils.isEmpty(label)) {
        throw new IllegalArgumentException("Audio stem label cannot be empty");
      }
      this.label = label;
      this.oscPath = Objects.requireNonNull(oscPath).trim();

      this.rawParameter = new BoundedParameter(label + "Raw");
      this.parameter =
          new BoundedFunctionalParameter(label) {
            @Override
            protected double computeValue() {
              // Apply adjustments (gain, smoothing) to a raw parameter
              return rawParameter.getValue() * (1.0 + gain.getValue());
            }
          }.setDescription("Audio stem for " + label);
    }

    /** Get the stem's current raw value prior to gain or smoothing. */
    public double getValueRaw() {
      return this.rawParameter.getValue();
    }

    /** Get the stem's current value with global gain applied. */
    public double getValue() {
      return this.parameter.getValue();
    }
  }

  public class Selector extends ObjectParameter<Stem> {

    public Selector(String label) {
      this(label, selectorObjects, selectorOptions);
      selectors.add(this);
    }

    private Selector(String label, Stem[] objects, String[] options) {
      super(label, objects, options);
    }

    @Override
    public void dispose() {
      selectors.remove(this);
      super.dispose();
    }
  }
}
