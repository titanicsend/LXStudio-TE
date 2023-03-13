package titanicsend.pattern.yoffa.shader_engine;

import heronarts.lx.audio.GraphicMeter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.util.TEMath;

import java.nio.FloatBuffer;
import java.util.Map;

public class PatternControlData {
    
    private Map<Uniforms.Audio, Float> uniformMap;

    TEPerformancePattern pattern;
    GraphicMeter meter;
    float fftResampleFactor;

    public PatternControlData(TEPerformancePattern pattern) {
        this.pattern = pattern;
        this.meter =  pattern.getLX().engine.audio.meter;
        fftResampleFactor = meter.bands.length / 512f;
    }

    public float getTime() {
        return pattern.iTime.getTime();
    }

    /**
     * Retrieve a single sample of the current frame's fft data from the engine
     * NOTE: 512 samples can always be retrieved, regardless of how many bands
     * the engine actually supplies.  Data will be properly distributed
     * (but not smoothed or interpolated) across the full 512 sample range.
     * @param index (0-511) of the sample to retrieve.
     *
     * @return fft sample, normalized to range 0 to 1.
     */
    public float getFrequencyData(int index) {
       return meter.getBandf((int) Math.floor((float) index * fftResampleFactor));
    }

    /**
     * Retrieve a single sample of the current frame's waveform data from the engine
     * @param index (0-511) of the sample to retrieve
     * @return waveform sample, range -1 to 1
     */
    public float getWaveformData(int index) {
        return meter.getSamples()[index];
    }

    public float getBeat() {
        return (float) pattern.getTempo().basis();
    }
    public float getSinePhaseOnBeat() {
        return (float) pattern.sinePhaseOnBeat();
    }

    public float getBassLevel() {
        return (float) pattern.getBassLevel();
    }

    public float getTrebleLevel() {
        return (float) pattern.getTrebleLevel();
    }

    /**
     * @return Returns the current rotation angle in radians, derived from a real-time LFO, the setting
     * of the "spin" control, and the constant MAX_ROTATIONS_PER_SECOND
     */
    public float getRotationAngle() {
        return pattern.getRotationAngle();
    }

    /**
     * @return Returns the current rotation angle in radians, derived from the sawtooth wave provided
     * by getTempo().basis(), the setting of the "spin" control, and a preset maximum rotations
     * per beat.
     */
    public float getRotationAngleOverBeat() {
        return pattern.getRotationAngleOverBeat();
    }

    public int getColorControl() {
        return pattern.getColorControl();
    }

    public FloatBuffer getCurrentPalette() { return pattern.getCurrentPalette();}

    public float getSpeedControl() {
        return pattern.getSpeedControl();
    }

    public float getXPosControl() {
        return pattern.getXPosControl();
    }

    public float getYPosControl() {
        return pattern.getYPosControl();
    }

    public float getSizeControl() {
        return pattern.getSizeControl();
    }

    public float getQuantityControl() { return pattern.getQuantityControl();}

    /**
     *    For most uses, getRotationAngle() is recommended, but if you
     *    need direct acces to the spin control value, here it is.
     */
    public float getSpinControl() {
        return pattern.getSpinControl();
    }

    public float getBrightnessControl() {
        return pattern.getBrightnessControl();
    }

    public float getWow1Control() {
        return pattern.getWow1Control();
    }

    public float getWow2Control() {
        return pattern.getWow2Control();
    }

    public boolean getWowTriggerControl() {
        return pattern.getWowTriggerControl();
    }

}
