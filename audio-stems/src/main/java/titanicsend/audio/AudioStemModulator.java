package titanicsend.audio;

import heronarts.glx.ui.UI2dContainer;
import heronarts.glx.ui.UI2dContainer.Position;
import heronarts.glx.ui.component.UIKnob;
import heronarts.glx.ui.component.UIMeter;
import heronarts.lx.LXCategory;
import heronarts.lx.modulator.LXModulator;
import heronarts.lx.modulator.LXWaveshape;
import heronarts.lx.osc.LXOscComponent;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.ObjectParameter;
import heronarts.lx.studio.LXStudio;
import heronarts.lx.studio.ui.modulation.UIModulator;
import heronarts.lx.studio.ui.modulation.UIModulatorControls;
import titanicsend.audio.util.EMA;

@LXModulator.Global("Audio Stem")
@LXModulator.Device("Audio Stem")
@LXCategory(LXCategory.AUDIO)
public class AudioStemModulator extends LXModulator
    implements LXOscComponent, LXNormalizedParameter, UIModulatorControls<AudioStemModulator> {

  public static final double MAX_SMOOTHING_MS = 1000;
  // Accumulator rate in maximum units per second
  public static final double MAX_ACCUMULATOR_RATE = 8.0;

  public final AudioStems.Selector stem;

  public enum OutputMode {
    ENERGY("Energy"), // raw stem energy value
    WAVE("Wave"); // wave derived from accumulated energy

    private final String label;

    OutputMode(String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return this.label;
    }
  }

  public final EnumParameter<OutputMode> outputMode =
      new EnumParameter<>("Mode", OutputMode.ENERGY).setDescription("Stem output mode");

  public final CompoundParameter accRate =
      new CompoundParameter("Rate", 0.2, 0.2, MAX_ACCUMULATOR_RATE)
          .setDescription("Wave mode frequency (Accumulation rate in stem energy units/sec)")
          .setExponent(2);

  public final CompoundParameter emaMs =
      new CompoundParameter("EMA", 0, 0, MAX_SMOOTHING_MS)
          .setDescription("Length of EMA smoothing time applied to the input, in milliseconds")
          .setExponent(2)
          .setUnits(Units.MILLISECONDS);

  public final ObjectParameter<LXWaveshape> waveshape =
      new ObjectParameter<>(
              "Shape",
              new LXWaveshape[] {
                LXWaveshape.SIN,
                LXWaveshape.DOWN,
                LXWaveshape.UP,
                LXWaveshape.TRI,
                LXWaveshape.SQUARE
              })
          .setDescription("Waveform used for wave output");

  public final BoundedParameter slope =
      new BoundedParameter("Slope", 1, 1, 15).setDescription("Steepness of of wave output");

  private final EMA ema = new EMA(0);

  private double accumulator = 0.0;

  public AudioStemModulator() {
    this("Audio Stem");
  }

  public AudioStemModulator(String label) {
    super(label);

    this.stem =
        AudioStems.get().newSelector("Stem", "Which audio stem is the source for this modulator");
    addParameter("stem", this.stem);
    addParameter("emaMs", this.emaMs);
    addParameter("outputMode", this.outputMode);
    addParameter("accRate", this.accRate);
    addParameter("shape", this.waveshape);
    addParameter("slope", this.slope);
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
    AudioStems.Stem stem = this.stem.getObject();
    double input = stem != null ? stem.getValue() : 0.0;
    double r = this.ema.update(input, deltaMs);

    if (this.outputMode.getEnum() == OutputMode.WAVE) {
      // Accumulate a rolling 0-1 value at the specified rate
      // so we can drive a wave output function
      this.accumulator += r * this.accRate.getValue() * deltaMs / 1000.0;
      this.accumulator = this.accumulator % 1;

      double exp = slope.getValue();
      r = (exp != 1) ? Math.pow(this.accumulator, exp) : this.accumulator;
      r = waveshape.getObject().compute(r);
    }
    return r;
  }

  /*
   * LXNormalizedParameter
   */

  /**
   * Pass the modulator value to the LXNormalizedParameter interface so this can be used as a
   * read-only parameter.
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

    uiModulator.setLayout(UI2dContainer.Layout.VERTICAL, 2.0F);
    UI2dContainer controls = UI2dContainer.newHorizontalContainer(2 * UIKnob.HEIGHT + 4, 2.0F);

    controls.setTopMargin(10);
    controls.setLeftMargin(6);
    controls.setChildSpacing(6.0f);

    this.addColumn(
        controls,
        50.0f,
        this.newDropMenu(this.stem),
        this.newKnob(this.outputMode).setPosition(0, 6));
    this.addColumn(controls, UIKnob.WIDTH, this.newKnob(this.emaMs), this.newKnob(this.waveshape));
    this.addColumn(
        controls, UIKnob.WIDTH + 18, this.newKnob(this.slope), this.newKnob(this.accRate));

    UIMeter meter = UIMeter.newVerticalMeter(ui, this, 12, 2 * UIKnob.HEIGHT);

    this.addColumn(controls, UIKnob.WIDTH, meter);

    controls.addToContainer(uiModulator, Position.LEFT);
  }
}
