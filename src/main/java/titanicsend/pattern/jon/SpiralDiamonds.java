package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TEMath;

@LXCategory("Panel FG")
public class SpiralDiamonds extends TEPerformancePattern {

    private static final float cosT = (float) Math.cos(TEMath.TAU / 1000);
    private static final float sinT = (float) Math.sin(TEMath.TAU / 1000);


    public final CompoundParameter energy =
            new CompoundParameter("Energy", .1, 0, 1)
                    .setDescription("Ummm.... what does this button do?");

    public SpiralDiamonds(LX lx) {
        super(lx, TEShaderView.DOUBLE_LARGE);

        // Quantity controls density of diamonds
        controls.setRange(TEControlTag.QUANTITY,4,1,7)
                .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

        addCommonControls();
        addParameter("energy", energy);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {

        float t1 = (float) getRotationAngleFromSpeed();
        double t2 = (float) getRotationAngleFromSpin();
        float scaleFactor = (float) getSize();

        float cosT2 = (float) Math.cos(t2);
        float sinT2 = (float) Math.sin(t2);

        int color = calcColor();
        double squareocity = getQuantity();

         for (LXPoint point : getModel().getPoints()) {
             // move normalized coord origin to model center
             float x = point.zn - 0.5f + (float) getXPos();
             float y = point.yn - 0.25f + (float) getYPos();

             // Scale according to size control setting
             // NOTE: The order of translation/scaling/rotation depends on
             // what you want your pattern to do. This pattern is definitely
             // not an example of a universally "proper" order.
             x *= scaleFactor;
             y *= scaleFactor;

             // repeat pattern over x axis at interval cx to make
             // two spirals at the default scale, one on each end of car.
             float cx = 0.3f;
             x = TEMath.floorModf(x + 0.5f * cx, cx) - 0.5f * cx;

             // rotate according to spin control setting
             // we do this inline, using precomputed sin/cos
             // because for this many pixels, we need the performance
             float outX = (cosT2 * x) - (sinT2  * y);
             y = (sinT2 * x) + (cosT2 * y);
             x = outX;

             // set up our square spiral
             float x1 = Math.signum(x);
             float y1 = Math.signum(y);

             float sx = x1 * cosT + y1 * sinT;
             float sy = y1 * cosT - x1 * sinT;

             // t1, from the speed control, determines speed & direction of the spiral.
             float dx = (float) Math.abs(Math.sin(squareocity * Math.log(x * sx + y * sy) + point.azimuth - t1));
             int on = ((dx * dx * dx) < 0.15) ? 1 : 0;

             // finally, set color of pixel
             colors[point.index] = color * on;
         }
    }

}
