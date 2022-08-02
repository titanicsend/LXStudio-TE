package titanicsend.pattern.yoffa.shader_engine;

import heronarts.lx.LX;
import heronarts.lx.audio.GraphicMeter;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.NormalizedParameter;
import titanicsend.util.TEMath;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AudioInfo {
    
    private Map<Uniforms.Audio, Float> uniformMap;
    GraphicMeter meter;
    float fftResampleFactor;

    int color;
    FloatBuffer palette;

    public AudioInfo(GraphicMeter meter) {
        this.meter = meter;
        fftResampleFactor = meter.bands.length / 512f;
    }

    
    public void setFrameData(double basis, double sinPhaseBeat,
                             double bassLevel, double trebleLevel, int col,
                             FloatBuffer pal) {
        uniformMap = Map.of(
            Uniforms.Audio.BEAT, (float) basis,
            Uniforms.Audio.SIN_PHASE_BEAT, (float) sinPhaseBeat,
            Uniforms.Audio.BASS_LEVEL, (float) bassLevel,
            Uniforms.Audio.TREBLE_LEVEL, (float) trebleLevel
        );

        this.palette = pal;
        this.color = col;
    }

    public void setFrameData(double basis, double sinPhaseBeat, double bassLevel, double trebleLevel) {
    }

    public Map<Uniforms.Audio, Float> getUniformMap() {
        return uniformMap;
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

}
