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
                    xHue(),
                    xSat(p),
                    xBrightness(p, xv * bands[binY])
            ));
            c = add(c, LXColor.hsb(
                    yHue(),
                    ySat(p),
                    yBrightness(p, yv * bands[170+binZ])
//                    max(0, ylv - ywv * Math.abs(p.y - (yv * (1 + waveform[binZ]))) / ranges.y)
            ));
            c = add(c, LXColor.hsb(
                    zHue(),
                    zSat(p),
                    zBrightness(p, zv * bands[240+binX])
            ));
            colors[p.index] = c;
        }
    }
}
