package titanicsend.audio;

import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UI2dContainer.Position;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UIMeter;
import heronarts.lx.LXCategory;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import titanicsend.util.EMA;

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

  public final CompoundParameter emaMs = new CompoundParameter("EMA", 0, 0, MAX_SMOOTHING_MS)
    .setDescription("Length of EMA smoothing time applied to the input, in milliseconds")
    .setExponent(2)
    .setUnits(Units.MILLISECONDS);

  private final EMA ema = new EMA(0);

  public AudioStemModulator() {
    this("Audio Stem");
  }

  public AudioStemModulator(String label) {
    super(label);

    addParameter("stem", this.stem);
    addParameter("emaMs", this.emaMs);
  }

  @Override
  public void onParameterChanged(LXParameter p) {
    if (p == this.emaMs) {
      updatePeriod();
    }
  }

  private void updatePeriod() {
    double emaMs = this.emaMs.getValue();
    this.ema.setPeriod(emaMs);
  }

  @Override
  protected double computeValue(double deltaMs) {
    double input = this.stem.getEnum().getValue();
    return this.ema.update(input, deltaMs);
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
      this.newKnob(this.emaMs)
    )
    .setLeftMargin(10)
    .addToContainer(uiModulator, Position.LEFT);

    UIMeter.newVerticalMeter(ui, this, 12, UIKnob.HEIGHT)
    .addToContainer(uiModulator, Position.RIGHT);
  }

}