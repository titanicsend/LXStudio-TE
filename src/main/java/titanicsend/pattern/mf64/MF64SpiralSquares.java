package titanicsend.pattern.mf64;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEWholeModel;
import titanicsend.pattern.TEMidiFighter64DriverPattern;
import titanicsend.pattern.jon.ButtonColorMgr;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.util.TEMath;

import java.util.ArrayList;

import static titanicsend.util.TEColor.TRANSPARENT;

public class MF64SpiralSquares extends TEMidiFighter64Subpattern {
    private static final double PERIOD_MSEC = 100.0;
    private static final int[] flashColors = {
            LXColor.rgb(255, 0, 0),
            LXColor.rgb(255, 170, 0),
            LXColor.rgb(255, 255, 0),
            LXColor.rgb(0, 255, 0),
            LXColor.rgb(0, 170, 170),
            LXColor.rgb(0, 0, 255),
            LXColor.rgb(255, 0, 255),
            LXColor.rgb(255, 255, 255),
    };


    ButtonColorMgr colorMap;

    private int flashColor = TRANSPARENT;
    private boolean active;
    private boolean stopRequest;

    private VariableSpeedTimer time;
    private float sinT, cosT;
    private TEWholeModel model;
    private LXPoint[] pointArray;

    private int refCount;

    public MF64SpiralSquares(TEMidiFighter64DriverPattern driver) {
        super(driver);
        this.model = this.driver.getModel();

        // get safe list of all pattern points.
        ArrayList<LXPoint> newPoints = new ArrayList<>(model.points.length);
        newPoints.addAll(model.edgePoints);
        newPoints.addAll(model.panelPoints);
        pointArray = newPoints.toArray(new LXPoint[0]);

        time = new VariableSpeedTimer();
        colorMap = new ButtonColorMgr();
        this.active = false;
        this.stopRequest = false;

        refCount = 0;
    }

    @Override
    public void buttonDown(TEMidiFighter64DriverPattern.Mapping mapping) {
        this.stopRequest = false;
        colorMap.addButton(mapping.col,flashColors[mapping.col]);
        refCount++;
        this.active = true;
    }

    @Override
    public void buttonUp(TEMidiFighter64DriverPattern.Mapping mapping) {
        colorMap.removeButton(mapping.col);
        refCount--;
        if (refCount == 0) this.stopRequest = true;
    }

    /**
     * FFS -- Java has no real mod operator?  Why??  Are we not
     * well into the Century of the Fruit Bat?  Isn't forcing
     * all bytes to be signed trouble enough for one language?  What next?<p>
     *
     * @return The floored remainder of the division a/b. The result will have
     * the same sign as b.
     */
    public static float mod(float a, float b) {
        float result = a % b;
        if (result < 0) {
            result += b;
        }
        return result;
    }

    private void paintAll(int colors[], int color) {

        // clear the decks if we're getting ready to stop
        if (stopRequest) {
            stopRequest = false;
            active = false;
            color = TRANSPARENT;
        }

        // calculate time scale at current bpm
        time.setScale(4f * (float) (driver.getTempo().bpm() / 60.0));

        // rotation rate is one per second. We sneakily control speed
        // by controlling the speed of time, so we can avoid trig operations
        // at pixel time.
        cosT = (float) Math.cos(TEMath.TAU / 1000);
        sinT = (float) Math.sin(TEMath.TAU / 1000);
        float t1 = time.getTimef();

        // a squared spiral from Pixelblaze pattern "Tunnel of Squares" at
        // https://github.com/zranger1/PixelblazePatterns/tree/master/2D_and_3D
        for (LXPoint point : this.pointArray) {

            // move normalized coord origin to model center
            float x = point.zn - 0.5f;
            float y = point.yn - 0.25f;

            // repeat pattern over x axis at interval cx
            // because!
            float cx = 0.3f;
            x = mod(x + 0.5f * cx, cx) - 0.5f * cx;


            // set up our square spiral
            float x1 = Math.signum(x);
            float y1 = Math.signum(y);

            float sx = x1 * cosT + y1 * sinT;
            float sy = y1 * cosT - x1 * sinT;

            float dx = (float) Math.abs(Math.sin(4.0 * Math.log(x * sx + y * sy) + point.azimuth - t1));
            int on = ((dx * dx * dx) < 0.15) ? 1 : 0;

            colors[point.index] = color * on;
        }
    }

    @Override
    public void run(double deltaMsec, int colors[]) {
        time.tick();
        if (this.active == true) {
            paintAll(colors, colorMap.getCurrentColor());
        }
    }
}
