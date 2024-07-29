package titanicsend.audio;

import heronarts.glx.ui.UI2dContainer.Position;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UIMeter;
import heronarts.lx.LXCategory;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;

@LXModulator.Global("Audio Stem")
@LXModulator.Device("Audio Stem")
@LXCategory(LXCategory.AUDIO)
public class AudioStemModulator extends LXModulator implements LXOscComponent, LXNormalizedParameter, UIModulatorControls<AudioStemModulator> {

  public static enum Stem {
    BASS("Bass") {
      @Override
      public double getValue() {
        return AudioStems.get().bass.getValue();
      }
    },
    DRUMS("Drums") {
      @Override
      public double getValue() {
        return AudioStems.get().drums.getValue();
      }

    },
    VOCALS("Vocals") {
      @Override
      public double getValue() {
        return AudioStems.get().vocals.getValue();
      }

    },
    OTHER("Other") {
      @Override
      public double getValue() {
        return AudioStems.get().other.getValue();
      }
    };

    public final String label;

    private Stem(String label) {
      this.label = label;
    }

    abstract public double getValue();
  }

  public final EnumParameter<Stem> stem = new EnumParameter<Stem>("Stem", Stem.BASS)
    .setDescription("Which audio stem is the source for this modulator");

  public AudioStemModulator() {
    this("Audio Stem");

    addParameter("stem", this.stem);
  }

  public AudioStemModulator(String label) {
    super(label);
  }

  @Override
  protected double computeValue(double deltaMs) {
    return this.stem.getEnum().getValue();
  }

  /*
   * LXNormalizedParameter
   */

  /**
   * Pass the modulator value to the LXNormalizedParameter interface
   * so this can be used as a read-only parameter.
   */
  @Override
  public double getNormalized() {
    return this.getValue();
  }

  @Override
  public LXNormalizedParameter setNormalized(double value) {
    throw new UnsupportedOperationException("Can not setNormalized on AudioStemModulator");
  }

  /*
   * UIModulatorControls<>
   */

  public void buildModulatorControls(
      LXStudio.UI ui, UIModulator uiModulator, AudioStemModulator modulator) {
    uiModulator.setContentHeight(UIKnob.HEIGHT + 4);
    uiModulator.setChildSpacing(2);
    newDropMenu(this.stem)
    .setLeftMargin(10)
    .setY(10)
    .setWidth(60)
    .addToContainer(uiModulator, Position.LEFT);
    newDropMenu(this.stem)
    .setLeftMargin(10)
    .setY(10)
    .setWidth(60)
    .addToContainer(uiModulator, Position.LEFT);
    UIMeter.newVerticalMeter(ui, this, 12, UIKnob.HEIGHT)
    .addToContainer(uiModulator, Position.RIGHT);
  }

}