package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;

import static heronarts.lx.color.LXColor.add;
import static java.lang.Math.max;
import static titanicsend.util.TEMath.*;

@LXCategory("Look Java Patterns")
public class CrossSectionsAudioStratified extends CrossSectionsAudio {

    public CrossSectionsAudioStratified(LX lx) {
        super(lx);
    }

    public void runTEAudioPattern(double deltaMs) {
        clearPixels();  // Sets all pixels to transparent for starters
        updateXYZVals();
        loadAudioTexture();

        float hue = LXColor.h(LXColor.BLUE);

        for (LXPoint p : model.points) {
            int c = 0;

            int nBins = 170;
            float normX = Math.abs(maxs.x - p.x) / ranges.x;
            int binX = (int) Math.floor(normX * (nBins - 1));

            float normY = Math.abs(maxs.y - p.y) / ranges.y;
            int binY = (int) Math.floor(normY * (nBins - 1));

            float normZ = Math.abs(maxs.z - p.z) / ranges.z;
            int binZ = (int) Math.floor(normZ * (nBins - 1));

            c = add(c, LXColor.hsb(
                    hue + p.x / (10 * ranges.x) + p.y / (3 * ranges.y),
                    clamp(140 - 110.0f * Math.abs(p.y - maxs.y) / ranges.y, 0, 100),
//                    max(0, xlv - xwv * Math.abs(p.x - xv) / ranges.x)
                    max(0, xlv - xwv * Math.abs(p.x - (xv * bands[binY])) / ranges.x)
            ));
            c = add(c, LXColor.hsb(
                    hue + 80 + p.y / (10 * ranges.y), //LXColor.h(LXColor.RED),
                    clamp(140 - 110.0f * Math.abs(p.x - maxs.x) / ranges.x, 0, 100),
                    max(0, ylv - ywv * Math.abs(p.y - (yv * bands[170+binZ])) / ranges.y)
//                    max(0, ylv - ywv * Math.abs(p.y - (yv * (1 + waveform[binZ]))) / ranges.y)
            ));
            c = add(c, LXColor.hsb(
                    hue + 160 + p.z / (10 * ranges.z) + p.y / (2 * ranges.y), //LXColor.h(LXColor.GREEN),
                    clamp(140 - 110.0f * Math.abs(p.z - maxs.z) / ranges.z, 0, 100),
//                    max(0, zlv - zwv * Math.abs(p.z - zv) / ranges.z)
                    max(0, zlv - zwv * Math.abs(p.z - (zv * bands[240+binX])) / ranges.z)
            ));
            colors[p.index] = c;
        }
    }
}
