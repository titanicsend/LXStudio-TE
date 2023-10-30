package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.audio.GraphicMeter;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;

import static heronarts.lx.color.LXColor.add;
import static java.lang.Math.max;
import static titanicsend.util.TEMath.clamp;

@LXCategory("Look Java Patterns")
public class CrossSectionsAudio extends CrossSections {

    protected GraphicMeter meter;
    protected float fftResampleFactor;
    protected int audioTextureWidth = 512;
    protected float[] waveform = new float[512];
    protected float[] bands = new float[512];

    public CrossSectionsAudio(LX lx) {
        super(lx);

        this.meter = this.getLX().engine.audio.meter;
        fftResampleFactor = meter.bands.length / 512f;

//        addParams();
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

    protected void loadAudioTexture() {
        // load frequency and waveform data into our texture, fft data in the first row,
        // normalized audio waveform data in the second.
        for (int n = 0; n < audioTextureWidth; n++) {
            bands[n] = getFrequencyData(n);
            waveform[n] = getWaveformData(n);
        }
    }

    public void runTEAudioPattern(double deltaMs) {
        clearPixels();  // Sets all pixels to transparent for starters
        updateXYZVals();
        loadAudioTexture();
//        System.out.printf("bands: %f %f %f ...\n", bands[0], bands[1], bands[2]);
//        System.out.printf("waveform: %f %f %f ...\n\n", waveform[0], waveform[1], waveform[2]);

        float hue = LXColor.h(LXColor.BLUE);

        for (LXPoint p : model.points) {
            int c = 0;

            int nBins = 512;
            float normX = Math.abs(maxs.x - p.x) / ranges.x;
            int binX = (int) Math.floor(normX * (nBins - 1));

            float normY = Math.abs(maxs.y - p.y) / ranges.y;
            int binY = (int) Math.floor(normY * (nBins - 1));

            float normZ = Math.abs(maxs.z - p.z) / ranges.z;
            int binZ = (int) Math.floor(normZ * (nBins - 1));

////            float adjustedXwv = xwv * (1.0f + waveform[binX]);
//            float adjustedYwv = ywv * (1.0f + waveform[binZ]);
//            adjustedYwv = ywv * normZ;
//            adjustedYwv = ywv * bands[binZ];

            c = add(c, LXColor.hsb(
                    xHue(),
                    xSat(p),
                    xBrightness(p, xv * bands[binY])
            ));
            c = add(c, LXColor.hsb(
                    yHue(),
                    ySat(p),
                    yBrightness(p, yv * bands[binZ])
            ));
            c = add(c, LXColor.hsb(
                    zHue(),
                    zSat(p),
                    zBrightness(p, zv * bands[binX])
            ));
            colors[p.index] = c;
        }
    }
}
