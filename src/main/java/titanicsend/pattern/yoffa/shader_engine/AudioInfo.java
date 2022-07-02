package titanicsend.pattern.yoffa.shader_engine;

import heronarts.lx.parameter.NormalizedParameter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class AudioInfo {
    
    private final Map<Uniforms.Audio, Float> uniformMap;
    private final NormalizedParameter[] frequencyData;
    
    public AudioInfo(double basis, double sinPhaseBeat, double bassLevel, double trebleLevel,
                     NormalizedParameter[] frequencyData) {
        uniformMap = Map.of(
            Uniforms.Audio.BEAT, (float) basis,
            Uniforms.Audio.SIN_PHASE_BEAT, (float) sinPhaseBeat,
            Uniforms.Audio.BASS_LEVEL, (float) bassLevel,
            Uniforms.Audio.TREBLE_LEVEL, (float) trebleLevel
        );
        this.frequencyData = frequencyData;
    }

    public Map<Uniforms.Audio, Float> getUniformMap() {
        return uniformMap;
    }

    //todo we can prolly get more granular bands if we want
    public float[] getFrequencyData() {
        float[] data = new float[512];
        float resample = 512f / frequencyData.length;
        int pos = 0;
        for (NormalizedParameter band : frequencyData) {
            for (int i = 0; i < resample; i++) {
                data[pos+i] = band.getNormalizedf();
            }
            pos += resample;
        }
        return data;
    }

    //todo placeholder for now
    //  lx.engine.audio.meter.getSamples might be what we're looking for here
    public float[] getWaveformData() {
        float[] data = new float[512];
        Arrays.fill(data, (float) .8);
        return data;
    }
}
