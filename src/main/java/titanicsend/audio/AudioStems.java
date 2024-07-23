package titanicsend.audio;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.osc.LXOscListener;
import heronarts.lx.osc.OscMessage;
import heronarts.lx.parameter.BoundedFunctionalParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter.Units;

public class AudioStems extends LXComponent implements LXOscListener { 

  public static final String PATH_STEM = "/te/stem/";
  public static final String PATH_BASS = "bass";
  public static final String PATH_DRUMS = "drums";
  public static final String PATH_VOCAL = "vocals";
  public static final String PATH_OTHER = "other";

  public static AudioStems current;

  public final CompoundParameter gain =
    new CompoundParameter("Gain", 0, -1, 2)
    .setUnits(Units.PERCENT_NORMALIZED);

  /*
   * Raw input values
   */

  public final BoundedParameter bassRaw = new BoundedParameter("bassRaw");
  public final BoundedParameter drumsRaw = new BoundedParameter("drumsRaw");
  public final BoundedParameter vocalRaw = new BoundedParameter("vocalRaw");
  public final BoundedParameter otherRaw = new BoundedParameter("otherRaw");

  /*
   * Values after gain, smoothing, etc.
   */

  public final BoundedFunctionalParameter bass =
    new BoundedFunctionalParameter("Bass") {
      @Override
      protected double computeValue() {
        return adjusted(bassRaw);
      }
    }
    .setDescription("Audio stem for bass");

  public final BoundedFunctionalParameter drums =
    new BoundedFunctionalParameter("Drums") {
      @Override
      protected double computeValue() {
        return adjusted(drumsRaw);
      }
    }
    .setDescription("Audio stem for drums");

  public final BoundedFunctionalParameter vocal =
    new BoundedFunctionalParameter("Vocal") {
      @Override
      protected double computeValue() {
        return adjusted(vocalRaw);
      }
    }
    .setDescription("Audio stem for vocal");

  public final BoundedFunctionalParameter other =
    new BoundedFunctionalParameter("Other") {
      @Override
      protected double computeValue() {
        return adjusted(otherRaw);
      }
    }
    .setDescription("Audio stem for other");

  /**
   * Apply adjustments (gain, smoothing) to a raw parameter
   */
  private double adjusted(BoundedParameter raw) {
    return raw.getValue() * (1 + gain.getValue());
  }

  public AudioStems(LX lx) {
    super(lx, "audioStems");
    current = this;

    addParameter("gain", this.gain);
    addParameter("bass", this.bass);
    addParameter("drums", this.drums);
    addParameter("vocal", this.vocal);
    addParameter("other", this.other);

    this.lx.engine.osc.addListener(this);
  }

  /**
   * Starting point for OSC input.
   * Called for messages received by the LX OSC Receiver.
   */
  @Override
  public void oscMessage(OscMessage message) {
    String address = message.getAddressPattern().getValue();

    if (address.startsWith(PATH_STEM)) {
      address = address.trim();
      if (address.length() == PATH_STEM.length()) {
        LX.warning("Audio stem name not specified: " + address);
        return;
      }
      
      float value = message.getFloat();
      
      String stem = address.substring(PATH_STEM.length());
      if (stem.equals(PATH_BASS)) {
        handleBass(value);
      } else if (stem.equals(PATH_DRUMS)) {
        handleDrums(value);
      } else if (stem.equals(PATH_VOCAL)) {
        handleVocal(value);
      } else if (stem.equals(PATH_OTHER)) {
        handleOther(value);
      } else {
        LX.warning("Unknown audio stem path: " + address);
        return;
      }
    }
  }

  private void handleBass(float value) {
    this.bassRaw.setValue(value);
  }
  
  private void handleDrums(float value) {
    this.drumsRaw.setValue(value);
  }
  
  private void handleVocal(float value) {
    this.vocalRaw.setValue(value);
  }
  
  private void handleOther(float value) {
    this.otherRaw.setValue(value);
  }

  @Override
  public void dispose() {
    this.lx.engine.osc.removeListener(this);
    super.dispose();
  }
}
