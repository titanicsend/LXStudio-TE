package titanicsend.pattern.mf64;

import heronarts.lx.model.LXPoint;
import titanicsend.pattern.TEMidiFighter64DriverPattern;

import static titanicsend.util.TEColor.setBrightness;

public class MF64EdgeSparks extends TEMidiFighter64Subpattern {
    boolean active = false;
    boolean stopRequest = false;
    double time;
    double startTime;
    static final float rocketSize = 0.045f;
    static final float rocketPos = 1f - rocketSize;
    static final float beatCount = 2f;

    public MF64EdgeSparks(TEMidiFighter64DriverPattern driver) {
        super(driver);
        startTime = 0;
    }

    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        buttons.addButton(mapping.col, overlayColors[mapping.col]);
        this.active = true;
        stopRequest = false;
        // uncomment to enable "glitch in place" feature.
        //startTime += 200;
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        if (buttons.removeButton(mapping.col) == 0) this.stopRequest = true;
    }

    private void paintAll(int color) {
        time = System.currentTimeMillis();

        // calculate milliseconds per beat at current bpm and build
        // a sawtooth wave that goes from 0 to 1 over that timespan
        float interval = beatCount * (float) (1000.0 / (driver.getTempo().bpm() / 60.0));
        float cycle = (float) (time - startTime) / interval;

        // grab colors of all currently pressed buttons
        int[] colorSet = buttons.getAllColors();

        // if we've completed a cycle see if we reset or stop
        if (cycle >= 1f) {
            if (stopRequest) {
                this.active = false;
                this.stopRequest = false;
                return;
            }
            startTime = time;
            cycle = 0;
        }

        // Basic approach adapted from Ben Hencke's "Fireworks Nova" pixelblaze pattern.
        // Build a moving sawtooth, coloring the leading edge with a solid color and
        // trailing off into random sparkles.  It winds up looking a lot like a particle
        // system, but it's much cheaper to compute.
        for (LXPoint point : modelTE.edgePoints) {
            float v;

            // get flipped y coord
            float y = 1f - point.yn;
            int colorIndex = (int) (8f * point.zn) % colorSet.length;

            // calculate y distance from our moving wave
            float wavefront = 1.0f - (cycle - y);

            // "rocket" at leading edge of wave
            //v = ((wavefront <= 1.0f) && (wavefront >= rocketPos)) ? 1f - (1f-wavefront) / rocketSize : 0;
            v = ((wavefront <= 1.0f) && (wavefront >= rocketPos)) ? 1f : 0;

            // random decaying "sparks" behind
            float spark = ((wavefront < rocketPos) && (wavefront >= 0.35)) ? wavefront : 0;
            if (spark > (3f * Math.random())) v = wavefront * wavefront * wavefront;

            if (v > 0) {
                color = setBrightness(colorSet[colorIndex], v);
                setColor(point.index, color);
            }
        }
    }

    @Override
    public void run(double deltaMsec) {
        if (this.active) {
            paintAll(buttons.getCurrentColor());
        }
    }
}
