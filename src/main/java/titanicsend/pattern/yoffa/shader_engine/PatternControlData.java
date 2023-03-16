package titanicsend.pattern.yoffa.shader_engine;

import heronarts.lx.audio.GraphicMeter;
import titanicsend.pattern.TEPerformancePattern;

import java.nio.FloatBuffer;
import java.util.Map;

public class PatternControlData {
    
    private Map<Uniforms.Audio, Float> uniformMap;

    TEPerformancePattern parent;
    GraphicMeter meter;
    float fftResampleFactor;

    public PatternControlData(TEPerformancePattern pattern) {
        this.parent = pattern;
        this.meter =  pattern.getLX().engine.audio.meter;
        fftResampleFactor = meter.bands.length / 512f;
    }

    public double getTime() {
        return parent.iTime.getTime();
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

    public double getBeat() {
        return parent.getTempo().basis();
    }

    public double getSinePhaseOnBeat() {
        return parent.sinePhaseOnBeat();
    }

    public double getBassLevel() {
        return parent.getBassLevel();
    }

    public double getTrebleLevel() {
        return parent.getTrebleLevel();
    }

    /**
     * @return Returns the current rotation angle in radians, derived from a real-time LFO, the setting
     * of the "spin" control, and the constant MAX_ROTATIONS_PER_SECOND
     */
    public double getRotationAngle() {
        return parent.getRotationAngle();
    }

    /**
     * @return Returns the current rotation angle in radians, derived from the sawtooth wave provided
     * by getTempo().basis(), the setting of the "spin" control, and a preset maximum rotations
     * per beat.
     */
    public double getRotationAngleOverBeat() {
        return parent.getRotationAngleOverBeat();
    }

    public int getCurrentColor() {
        return parent.getCurrentColor();
    }

    public FloatBuffer getCurrentPalette() { return parent.getCurrentPalette();}

    public double getSpeed() {
        return parent.getSpeed();
    }

    public double getXPos() {
        return parent.getXPos();
    }

    public double getYPos() {
        return parent.getYPos();
    }

    public double getSize() {
        return parent.getSize();
    }

    public double getQuantity() { return parent.getQuantity();}

    /**
     *    For most uses, getRotationAngle() is recommended, but if you
     *    need direct acces to the spin control value, here it is.
     */
    public double getSpin() {
        return parent.getSpin();
    }

    public double getBrightness() {
        return parent.getBrightness();
    }

    public double getWow1() {
        return parent.getWow1();
    }

    public double getWow2() {
        return parent.getWow2();
    }

    public boolean getWowTrigger() {
        return parent.getWowTrigger();
    }

}
