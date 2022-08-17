package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.util.TE;
import titanicsend.util.TEMath;

import java.util.Random;

// All LED platforms, no matter how large, must have KITT!
@LXCategory("Edge FG")
public class FrameBrights extends TEAudioPattern {
    private static final int MAX_ZONES = 60;
    boolean[] zoneIsLit;
    long seed;
    Random prng;
    float cycleCount;
    double lastCycle;
    protected final CompoundParameter cycleLength = (CompoundParameter)
            new CompoundParameter("Measures", 2, 1, 16)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Number of measures between segment shifts");
    protected final CompoundParameter zonesPerEdge = (CompoundParameter)
            new CompoundParameter("Zones", 30, 1, MAX_ZONES)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Total lit segments per edge");

    public final CompoundParameter minBrightness =
            new CompoundParameter("BG Bri", 0.125, 0.0, 1)
                    .setDescription("Background Brightness");

    public final CompoundParameter minHeight =
            new CompoundParameter("Height", 0.43, 0.0, 1)
                    .setDescription("Min starting height for bright lights");

    protected final CompoundParameter minLit = (CompoundParameter)
            new CompoundParameter("MinLit", 1, 0, MAX_ZONES)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Min lit segments per edge");

    protected final CompoundParameter maxLit = (CompoundParameter)
            new CompoundParameter("MaxLit", 3, 1, MAX_ZONES)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Max lit segments per edge");

    public final CompoundParameter energy =
            new CompoundParameter("Energy", .5, 0, 1)
                    .setDescription("Depth of light pulse");

    public final BooleanParameter sync =
            new BooleanParameter("Sync", true)
                    .setDescription("Autosync segment changes to measure");

    public final BooleanParameter change =
            new BooleanParameter("Change", false)
                    .setMode(BooleanParameter.Mode.MOMENTARY)
                    .setDescription("New light segments NOW!");

    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PRIMARY,
                    "Color");

    public FrameBrights(LX lx) {
        super(lx);
        addParameter("energy", energy);
        addParameter("beatsPerCycle", cycleLength);
        addParameter("zoneCount", zonesPerEdge);
        addParameter("minLit", minLit);
        addParameter("maxLit", maxLit);
        addParameter("minBri", minBrightness);
        addParameter("height", minHeight);
        addParameter("sync", sync);
        addParameter("change", change);

        prng = new Random();
        lastCycle = 99f; // trigger immediate start;
        cycleCount = 0f;
        zoneIsLit = new boolean[MAX_ZONES];  // should be plenty of room.
    }

    // choose between minLit and maxLit random segments of an edge to light
    // and prepare the data necessary to do quickly
    void lightRandomSegments(int zoneCount, int minLit, int maxLit) {
        int nLit = Math.max(minLit, Math.round(maxLit * prng.nextFloat()));

        // set all segments dark
        for (int i = 0; i < zoneCount; i++) {
            zoneIsLit[i] = false;
        }

        // pick a new set of segments to light
        for (int i = 0; i < nLit; i++) {
            int index = (int) Math.floor(prng.nextFloat() * (zoneCount - 1));
            zoneIsLit[index] = true;
        }
    }

    public void runTEAudioPattern(double deltaMs) {
        updateGradients();

        // sync lit segment selection changes to measures, and
        // light pulses to the beat.
        float currentCycle = (float) measure();
        float currentBeat = (float) getTempo().basis();

        // if autosync is enabled, we change lights every cycleLength measures.
        if (sync.getValueb()) {
            if (currentCycle < lastCycle) {
                if (cycleCount >= cycleLength.getValuef()) {
                    seed = System.currentTimeMillis();
                    cycleCount = 0;
                }
                cycleCount++;
            }
        }
        // pressing the "Change" button doesn't disrupt ongoing beat/measure counting
        // if autosync is operating.
        if (change.getValueb()) {
            seed = System.currentTimeMillis();
        }

        lastCycle = measure();

        // reset prng so we get the same set of numbers each frame 'till
        // we change the seed.
        prng.setSeed(seed);

        // get the current color
        int baseColor = this.color.calcColor();

        // spotlight brightness pulses with the beat
        float spotBrightness = (float) (1.0 - energy.getValue() * currentBeat);

        // get display parameter variables from control settings
        float yMin = minHeight.getValuef();
        float minBri = minBrightness.getValuef();
        float briShift = (float) (energy.getValue() * Math.min(0.25,minBri * 0.25));
        int zoneCount = (int) zonesPerEdge.getValue();

        // get, and de-confuse min and max number of segments to light
        // (max must be greater than min, both must be <= current zone count)
        int minZones = (int) minLit.getValue();
        int maxZones = (int) maxLit.getValue();

        minZones = Math.min(zoneCount, minZones);
        minZones = (minZones > maxZones) ? maxZones : minZones;
        maxZones = Math.min(zoneCount,maxZones);

        for (TEEdgeModel edge : model.getAllEdges()) {
            float alpha;
            lightRandomSegments(zoneCount,minZones,maxZones);
            for (TEEdgeModel.Point point : edge.points) {
                int zone = (int) Math.floor(point.frac * (zoneCount-1));

                // lit segments to pulsing max brightness
                if (zoneIsLit[zone] && point.yn >= yMin) {
                    alpha = spotBrightness;
                }
                else {
                    // non-lit segments get shifting min brightness pattern
                    // build inexpensive but complex-looking pattern using precalculated point data
                    float w = (float) (10.0 * ((1.0-(point.elevation / Math.PI)) * (point.xn+point.yn)));
                    alpha = (minBri + briShift * TEMath.trianglef(currentCycle+w));
                }

                // clear and reset alpha channel
                baseColor = baseColor & ~LXColor.ALPHA_MASK;
                baseColor = baseColor | ((int) (alpha * 255) << LXColor.ALPHA_SHIFT);
                colors[point.index] = baseColor;
            }
        }
    }
}