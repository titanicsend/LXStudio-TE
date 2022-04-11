package titanicsend.pattern.jeff;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.*;
import titanicsend.model.TEEdgeModel;
import titanicsend.pattern.TEPattern;

import heronarts.lx.audio.GraphicMeter;

/*
    Building on BassReactive, this pattern demonstrates:

    * Applying an aspect of the sound data (bass) to a pattern (mid-edge pulse width)
    * How to use a parameter that allows the VJ to select the number of bass bands averaged
    * How to use the attack/decay setting of a GraphicMeter

    Be sure to try this pattern with an audio input source selected. In the top
    left of the UI, click "GLOBAL", enable the Audio with the green top-left toggle,
    and select an input. To route music to your speakers AND to LX at the same time,
    use BlackHole. BlackHole can be useful to get the songs you play on your computer
    to loopback as an audio input (simulating the line-in audio feed we’ll have on
    the car). Use Audio Midi Setup to configure a multi-output device:

    https://existential.audio/blackhole/
    https://github.com/ExistentialAudio/BlackHole/wiki/Multi-Output-Device

 */
@LXCategory("Audio Examples")
public class BassReactiveEdge extends TEPattern {
    // The GraphicMeter holds the analyzed frequency content of the audio input
    protected final GraphicMeter eq = lx.engine.audio.meter;

    // Titanic's End wants all patterns to implement a parameter called "Energy",
    // which controls the amount of motion, light, movement, action, particles, etc.
    public final CompoundParameter energy =
            new CompoundParameter("Energy", .1, 0, 1)
                    .setDescription("Amount of motion for various modes");

    // Build on BassReactive by allowing the VJ to select the upper bin of
    // the frequency bins being averaged.
    public final DiscreteParameter bassBandCount =
            new DiscreteParameter("Bands", 1, 1, eq.numBands/2)
                    .setDescription("Number of bands used to sense bass");


    public BassReactiveEdge(LX lx) {
        super(lx);
        addParameter("energy", energy);
        addParameter("bands", bassBandCount);
    }

    public void run(double deltaMs) {
        clearPixels();  // Sets all pixels to transparent for starters

        /* Get the average bass level of some low frequency bands.
        The default lx.engine.audio.meter breaks up sound into 16 bands,
        the user can select how many of the lowest bands to average. */
        float bassLevel = eq.getAveragef(0, bassBandCount.getValuei());

        for (TEEdgeModel edge : model.edgesById.values()) {
            // Max width of the lit section of this edge, from 0 to 200 percent
            // of the overall edge length.
            float maxWidth = energy.getNormalizedf() * 2;

            // Scale the percentage of this max size we will light based on `bassLevel`
            float bassWidth = bassLevel * maxWidth;

            // low and high straddle the center (.5f is 50% of the edge length)
            float low = .5f - bassWidth / 2;
            float high = low + bassWidth;

            for (TEEdgeModel.Point point : edge.points) {
                if (point.pct >= low && point.pct < high)
                    colors[point.index] = LXColor.RED;
            }
        }
    }

}
