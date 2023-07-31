package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.audio.GraphicMeter;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import heronarts.lx.modulator.SinLFO;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

import static heronarts.lx.color.LXColor.add;
import static java.lang.Math.max;
import static titanicsend.util.TEMath.*;

@LXCategory("Look Java Patterns")
public class CrossSectionsStratifiedAudio extends TEPerformancePattern {

    public final SinLFO x;
    public final SinLFO y;
    public final SinLFO z;

    public final float minX;
    public final float maxX;
    public final float minY;
    public final float maxY;
    public final float minZ;
    public final float maxZ;

    public final float xRange;
    public final float yRange;
    public final float zRange;


    final CompoundParameter xl = new CompoundParameter("xLvl", 1);
    final CompoundParameter yl = new CompoundParameter("yLvl", 1);
    final CompoundParameter zl = new CompoundParameter("zLvl", 0.5);

    final CompoundParameter xr = new CompoundParameter("xSpd", 0.7);
    final CompoundParameter yr = new CompoundParameter("ySpd", 0.6);
    final CompoundParameter zr = new CompoundParameter("zSpd", 0.5);

    final CompoundParameter xw = new CompoundParameter("xSize", 0.2);
    final CompoundParameter yw = new CompoundParameter("ySize", 0.3);
    final CompoundParameter zw = new CompoundParameter("zSize", 0.2);

    GraphicMeter meter;
    float fftResampleFactor;
    int audioTextureWidth = 512;
    float[] waveform = new float[512];
    float[] bands = new float[512];

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

    public CrossSectionsStratifiedAudio(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        this.meter = this.getLX().engine.audio.meter;
        fftResampleFactor = meter.bands.length / 512f;

        minX = modelTE.boundaryPoints.minXBoundaryPoint.x;
        maxX = modelTE.boundaryPoints.maxXBoundaryPoint.x;
        minY = modelTE.boundaryPoints.minYBoundaryPoint.y;
        maxY = modelTE.boundaryPoints.maxYBoundaryPoint.y;
        minZ = modelTE.boundaryPoints.minZBoundaryPoint.z;
        maxZ = modelTE.boundaryPoints.maxZBoundaryPoint.z;
        xRange = (maxX - minX);
        yRange = (maxY - minY);
        zRange = (maxZ - minZ);

        x = new SinLFO(minX, maxX, 5000);
        y = new SinLFO(minY, maxY, 6000);
        z = new SinLFO(minZ, maxZ, 7000);
        addParams();
//        addCommonControls();

        addModulator(x).trigger();
        addModulator(y).trigger();
        addModulator(z).trigger();
    }

    protected void addParams() {
        addParameter("xr", xr);
        addParameter("yr", yr);
        addParameter("zr", zr);
        addParameter("xl", xl);
        addParameter("yl", yl);
        addParameter("zl", zl);
        addParameter("xw", xw);
        addParameter("yw", yw);
        addParameter("zw", zw);
    }

    public void onParameterChanged(LXParameter p) {
        if (p == xr) {
            x.setPeriod(10000 - 8800 * p.getValuef());
        } else if (p == yr) {
            y.setPeriod(10000 - 9000 * p.getValuef());
        } else if (p == zr) {
            z.setPeriod(10000 - 9000 * p.getValuef());
        }
    }

    float xv, yv, zv;

    protected void updateXYZVals() {
        xv = x.getValuef();
        yv = y.getValuef();
        zv = z.getValuef();
    }

    public void runTEAudioPattern(double deltaMs) {
        clearPixels();  // Sets all pixels to transparent for starters

        updateXYZVals();

        loadAudioTexture();
//        System.out.printf("bands: %f %f %f ...\n", bands[0], bands[1], bands[2]);
//        System.out.printf("waveform: %f %f %f ...\n\n", waveform[0], waveform[1], waveform[2]);

        float xlv = 100 * xl.getValuef();
        float ylv = 100 * yl.getValuef();
        float zlv = 100 * zl.getValuef();

        float xwv = 100f / (xw.getValuef());
        float ywv = 100f / (yw.getValuef());
        float zwv = 100f / (zw.getValuef());

        // TODO: this requires common controls to add a gradient param - but this prevents addParams() from adding anything to the UI.
        //        int baseColor = getGradientColor(1.0f); // TODO(look): is 1.0 the right 'lerp' value?
        //        float hue = LXColor.h(baseColor);
        //        System.out.println(String.format("hue = %f", hue));

        float hue = LXColor.h(LXColor.BLUE);


        for (LXPoint p : model.points) {
//            System.out.printf("Math.abs(p.y - maxY) / yRange = %f\n", Math.abs(p.y - maxY) / yRange);
//            System.out.printf("Math.abs(p.x - maxX) / xRange = %f\n", Math.abs(p.x - maxX) / xRange);
//            System.out.printf("Math.abs(p.z - maxZ) / zRange = %f\n", Math.abs(p.z - maxZ) / zRange);
//                System.out.printf("xlv=%f, xwv=%f, p.x=%f, xv=%f, Math.abs(p.x-xv)=%f\n", xlv, xwv, p.x, xv, Math.abs(p.x-xv) / maxX);
//                System.out.printf("xlv - xwv * Math.abs(p.x - xv) = %f, max(0, xlv - xwv * Math.abs(p.x - xv)) = %f\n\n", xlv - xwv * Math.abs(p.x - xv) / maxX, max(0, xlv - xwv * Math.abs(p.x - xv) / maxX));
            int c = 0;

            int nBins = 170;
            float normX = Math.abs(maxX - p.x) / xRange;
            int binX = (int) Math.floor(normX * (nBins - 1));

            float normY = Math.abs(maxY - p.y) / yRange;
            int binY = (int) Math.floor(normY * (nBins - 1));

            float normZ = Math.abs(maxZ - p.z) / zRange;
            int binZ = (int) Math.floor(normZ * (nBins - 1));

////            float adjustedXwv = xwv * (1.0f + waveform[binX]);
//            float adjustedYwv = ywv * (1.0f + waveform[binZ]);
//            adjustedYwv = ywv * normZ;
//            adjustedYwv = ywv * bands[binZ];

            c = add(c, LXColor.hsb(
                    hue + p.x / (10 * xRange) + p.y / (3 * yRange),
                    clamp(140 - 110.0f * Math.abs(p.y - maxY) / yRange, 0, 100),
//                    max(0, xlv - xwv * Math.abs(p.x - xv) / xRange)
                    max(0, xlv - xwv * Math.abs(p.x - (xv * bands[binY])) / xRange)
            ));
            c = add(c, LXColor.hsb(
                    hue + 80 + p.y / (10 * yRange), //LXColor.h(LXColor.RED),
                    clamp(140 - 110.0f * Math.abs(p.x - maxX) / xRange, 0, 100),
                    max(0, ylv - ywv * Math.abs(p.y - (yv * bands[170+binZ])) / yRange)
//                    max(0, ylv - ywv * Math.abs(p.y - (yv * (1 + waveform[binZ]))) / yRange)
            ));
            c = add(c, LXColor.hsb(
                    hue + 160 + p.z / (10 * zRange) + p.y / (2 * yRange), //LXColor.h(LXColor.GREEN),
                    clamp(140 - 110.0f * Math.abs(p.z - maxZ) / zRange, 0, 100),
//                    max(0, zlv - zwv * Math.abs(p.z - zv) / zRange)
                    max(0, zlv - zwv * Math.abs(p.z - (zv * bands[240+binX])) / zRange)
            ));
            colors[p.index] = c;
        }
    }
}
