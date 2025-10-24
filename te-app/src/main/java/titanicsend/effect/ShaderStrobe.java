package titanicsend.effect;

/*
 * GPU adaptation of heronarts.lx.effect.StrobeEffect from LX Studio
 * (Mark C. Slee / Heron Arts LLC). Original logic translated to run as a
 * post-processing shader within the Titanic's End pipeline.
 */

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.Tempo;
import heronarts.lx.modulator.LXWaveshape;
import heronarts.lx.modulator.SawLFO;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.FunctionalParameter;
import heronarts.lx.parameter.ObjectParameter;
import heronarts.lx.parameter.LXParameter.Polarity;
import heronarts.lx.parameter.LXParameter.Units;
import heronarts.lx.utils.LXUtils;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderEffect;

@LXCategory("Titanics End")
public class ShaderStrobe extends GLShaderEffect {

    // Parameters (mirroring original StrobeEffect)
    public final ObjectParameter<LXWaveshape> waveshape =
            new ObjectParameter<>(
                    "Waveshape",
                    new LXWaveshape[] {
                            LXWaveshape.SIN, LXWaveshape.TRI, LXWaveshape.SQUARE, LXWaveshape.UP, LXWaveshape.DOWN
                    })
                    .setDescription("Strobe waveform: SIN/TRI for smoother dimming, SQUARE for hard on/off");

    public final BoundedParameter maxFrequency =
            new BoundedParameter("Max Freq", 8.0, 1.0, 30.0)
                    .setDescription("Upper bound on strobe frequency (Hz) when Speed is at 100%")
                    .setUnits(Units.HERTZ);

    public final BoundedParameter minFrequency =
            new BoundedParameter("Min Freq", 0.5, 0.1, 1.0)
                    .setDescription("Lower bound on strobe frequency (Hz) when Speed is at 0%")
                    .setUnits(Units.HERTZ);

    public final CompoundParameter speed =
            new CompoundParameter("Speed", 0.35)
                    .setUnits(Units.PERCENT_NORMALIZED)
                    .setExponent(2.0)
                    .setDescription("Crossfade between Min/Max frequency (0% = slowest, 100% = fastest)");

    public final CompoundParameter depth =
            new CompoundParameter("Depth", 0.85)
                    .setUnits(Units.PERCENT_NORMALIZED)
                    .setDescription("Mix amount: 0% no strobe, 100% full blackout during off phases");

    public final CompoundParameter bias =
            new CompoundParameter("Bias", 0.0, -1.0, 1.0)
                    .setUnits(Units.PERCENT_NORMALIZED)
                    .setPolarity(Polarity.BIPOLAR)
                    .setDescription("Waveform bias: negative favors longer off times, positive favors on");

    public final BooleanParameter tempoSync =
            new BooleanParameter("Sync", true)
                    .setDescription("Sync strobe timing to the global tempo clock");

    public final EnumParameter<Tempo.Division> tempoDivision =
            new EnumParameter<>("Division", Tempo.Division.EIGHTH)
                    .setDescription("Tempo division used for Sync mode (e.g. eighth-notes)");

    public final BoundedParameter tempoPhaseOffset =
            new BoundedParameter("Phase Offset", 0.0)
                    .setUnits(Units.PERCENT_NORMALIZED)
                    .setDescription("Phase offset for tempo sync (0-100% of one beat)");

    // Free-running LFO basis (period derived from min/max freq and speed)
    private final SawLFO basis =
            startModulator(
                    new SawLFO(
                            0.0,
                            1.0,
                            new FunctionalParameter() {
                                @Override
                                public double getValue() {
                                    // period (ms) = 1000 / freq ; freq = lerp(min,max,speed)
                                    double f = LXUtils.lerp(minFrequency.getValue(), maxFrequency.getValue(), speed.getValue());
                                    return 1000.0 / Math.max(0.0001, f);
                                }
                            }));

    public ShaderStrobe(LX lx) {
        super(lx);

        addParameter("waveshape", this.waveshape);
        addParameter("minFrequency", this.minFrequency);
        addParameter("maxFrequency", this.maxFrequency);
        addParameter("speed", this.speed);
        addParameter("depth", this.depth);
        addParameter("bias", this.bias);
        addParameter("tempoSync", this.tempoSync);
        addParameter("tempoDivision", this.tempoDivision);
        addParameter("tempoPhaseOffset", this.tempoPhaseOffset);

                // Default dial-in values for a musical, square strobe out of the box
                this.waveshape.setValue(LXWaveshape.SQUARE);
                this.minFrequency.setValue(0.5);
                this.maxFrequency.setValue(8.0);
                this.speed.setValue(0.35);
                this.depth.setValue(0.85);
                this.bias.setValue(0.0);
                this.tempoSync.setValue(true);
                this.tempoDivision.setValue(Tempo.Division.EIGHTH);
                this.tempoPhaseOffset.setValue(0.0);

        addShader(
                GLShader.config(lx)
                        .withFilename("shader_strobe.fs")
                        .withUniformSource(this::setUniforms));
    }

    private double getTempoBasis() {
        double b = this.lx.engine.tempo.getBasis(this.tempoDivision.getEnum());
        b = (b + this.tempoPhaseOffset.getValue()) % 1.0;
        return b;
    }

    /**
     * Compute the raw strobe LFO for a given basis in [0..1], apply bias curve shaping,
     * return scalar in [0..1] (1 = pass-through, 0 = blackout).
     */
    private float compute(double basis) {
        // waveshape in [0..1]
        double strobe = this.waveshape.getObject().compute(basis);

        // Bias curve (same mapping as original): expPower = bias>=0 ? 1+3*bias : 1/(1-3*bias)
        double b = this.bias.getValue();
        double expPower = (b >= 0.0) ? (1.0 + 3.0 * b) : (1.0 / (1.0 - 3.0 * b));
        if (Math.abs(expPower - 1.0) > 1e-6) {
            strobe = Math.pow(strobe, expPower);
        }

        return (float) strobe;
    }

    private void setUniforms(GLShader shader) {
        // Choose basis: tempo-synced or free-running LFO
        double strobeBasis = this.tempoSync.isOn() ? getTempoBasis() : this.basis.getValue();

        // Raw strobe (0..1) after bias
        float raw = compute(strobeBasis);

        // Mix with depth: lerp(1.0, raw, depth)
        float strobe = LXUtils.lerpf(1.0f, raw, (float) this.depth.getValue());

        // Ship to shader
        shader.setUniform("strobe", strobe);
    }
}