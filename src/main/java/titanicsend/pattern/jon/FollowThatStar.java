package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.*;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.util.TEMath;

@LXCategory("Combo FG")
public class FollowThatStar extends TEAudioPattern {

    // pattern variables
    protected float trailSize = 0.35f;
    protected double segSpacing = 0.5f;
    protected int segments = 5;
    protected double timebase;  // main time accumulator
    protected double t1;         // scratch timer
    protected double[] xOffsets;
    protected double[] yOffsets;

    // Controls
    public final CompoundParameter energy =
            new CompoundParameter("Energy", .5, 0, 1)
                    .setDescription("Stars sparkle and move");

    protected final CompoundParameter beatScale = (CompoundParameter)
            new CompoundParameter("Speed", 60, 120, 1)
                    .setExponent(1)
                    .setUnits(LXParameter.Units.INTEGER)
                    .setDescription("Overall movement speed");


    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PANEL,
                    "Panel Color");


    public FollowThatStar(LX lx) {
        super(lx);
        addParameter("beatScale", this.beatScale);
        addParameter("energy", energy);

        timebase = 0;
        xOffsets = new double[segments];
        yOffsets = new double[segments];
    }

    // Minkowski distance at fractional exponents makes the nice 4-pointed star!
    public float minkowskiDistance(double x1, double y1, double p) {
        return (float) (Math.pow(Math.pow(Math.abs(x1), p) + Math.pow(Math.abs(y1), p), 1.0 / p));
    }

    public void runTEAudioPattern(double deltaMs) {
        float saturation,brightness,alpha;

        // whole system is constantly moving over time.
        timebase = ((double) System.currentTimeMillis()) / 1000.0;

        // pick up the current set color
        int baseColor = this.color.calcColor();
        float baseHue = LXColor.h(baseColor);
        float baseSat = LXColor.s(baseColor) / 100f;
        float baseBri = LXColor.b(baseColor) / 100f;
        alpha = 100f;

        // Sound reactivity, amount controlled by energy control setting.
        double e = energy.getValue();
        double beat = lx.engine.tempo.bpm() / beatScale.getValue();
        t1 = timebase * beat;

        double phase = lx.engine.tempo.basis() * e;
        double bass =  avgBass.getValue() * e;

        double yWiggle = 0.25 * bass;
        double sparkle = trailSize + 0.5 * bass;

        // precalculate distance offsets for each segment.  Bias Y movement a little so its stays a
        // mostly on the panels and off the top edges.
        for (int i = 0; i < segments; i++) {
            double t = t1 - (segSpacing * (i + 1));
            xOffsets[i] = 0.92 * Math.sin(t) + 0.05 * Math.cos(t * 6);
            yOffsets[i] = -0.25 + (0.65 * Math.sin(t * 0.85) + yWiggle * Math.sin(t * 2));
        }

        // per pixel calculations
        for (LXPoint point : model.points) {

            // translate and rescale normalized coords from -1 to 1
            double x = 2 * (point.zn - 0.5);  // z axis on vehicle
            double y = 2 * (point.yn - 0.5);

            brightness = 0;

            // Add the light contribution of each star to the current pixel value
            for (int i = 0; i < segments; i++) {
               double scale = (double) i + 1;
               brightness += sparkle / minkowskiDistance(x - xOffsets[i],
                                                                   y - yOffsets[i],
                                                                   0.375) / scale;
            }

            saturation = baseSat * (float) TEMath.clamp(100f * (phase+(2.25f-brightness)),0,100);
            brightness = baseBri * (float) TEMath.clamp(brightness * brightness * 100f,0,100);
            colors[point.index] = LXColor.hsba(
                    baseHue,
                    saturation,
                    brightness,
                    brightness
            );
        }
    }
}
