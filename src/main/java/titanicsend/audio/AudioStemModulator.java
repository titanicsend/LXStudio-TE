package titanicsend.audio;

import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UI2dContainer.Position;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UIMeter;
import heronarts.lx.LXCategory;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.modulator.Smoother;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import heronarts.lx.utils.LXUtils;

@LXModulator.Global("Audio Stem")
@LXModulator.Device("Audio Stem")
@LXCategory(LXCategory.AUDIO)
public class AudioStemModulator extends LXModulator implements LXOscComponent, LXNormalizedParameter, UIModulatorControls<AudioStemModulator> {

  public static final double MAX_SMOOTHING_MS = 1000;

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

  public final CompoundParameter smooth = new CompoundParameter("Smooth", 0, 0, MAX_SMOOTHING_MS)
    .setDescription("Amount of smoothing time applied to the input, in milliseconds")
    .setExponent(2)
    .setUnits(Units.MILLISECONDS);

  public AudioStemModulator() {
    this("Audio Stem");
  }

  public AudioStemModulator(String label) {
    super(label);

    addParameter("stem", this.stem);
    addParameter("smooth", this.smooth);
  }

  @Override
  protected double computeValue(double deltaMs) {
    final double input = this.stem.getEnum().getValue();
    final double smooth = this.smooth.getValue();

    if (smooth == 0) {
      return input;
    } else {
      // This math is from the LX Smoother modulator:
      // https://github.com/heronarts/LX/blob/master/src/main/java/heronarts/lx/modulator/Smoother.java
      return LXUtils.lerp(
        getValue(),
        input,
        LXUtils.min(1, deltaMs / smooth));
    }
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
    UI2dContainer.newHorizontalContainer(UIKnob.HEIGHT + 4, 4,
      newDropMenu(this.stem)
      .setY(10)
      .setWidth(60),
      this.newKnob(this.smooth)
    )
    .setLeftMargin(10)
    .addToContainer(uiModulator, Position.LEFT);

    UIMeter.newVerticalMeter(ui, this, 12, UIKnob.HEIGHT)
    .addToContainer(uiModulator, Position.RIGHT);
  }

}