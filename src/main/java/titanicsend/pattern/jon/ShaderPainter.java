package titanicsend.pattern.jon;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.util.Dimensions;

/*
public class ShaderPainter {
    // maps image -> vehicle for every pixel on vehicle.
    // this uses some memory, but it allows us to do coordinate lookup and
    // image scaling at setup time so drawing can be very fast.
    private int[] imagePixel;

    ShaderPainter() {
        ;
    }

    // for each point on the vehicle, calculate the corresponding point in a flat
    // image bitmap
    public void paint(LXPoint point, Dimensions canvasDimensions, double scaleRatio) {
        // here the 'z' dimension of TE corresponds with 'x' dimension of the image based on the side that
        //   we're painting
        float normalizedX;
        if (canvasDimensions.widerOnZThanX()) {
            normalizedX = (point.zn - canvasDimensions.getMinZn()) / canvasDimensions.getDepthNormalized();
        } else {
            normalizedX = (point.xn - canvasDimensions.getMinXn()) / canvasDimensions.getWidthNormalized();
        }
        float normalizedY = (point.yn - canvasDimensions.getMinYn()) / canvasDimensions.getHeightNormalized();

        double x = (1 - normalizedX) * image.getWidth();
        x = x / scaleRatio + ((image.getWidth()-(image.getWidth() / scaleRatio)) / 2);
        int xi = (int) Math.min(Math.round(x), image.getWidth() - 1);

        double y = (1 - normalizedY) * image.getHeight();
        y = y / scaleRatio + ((image.getHeight()-(image.getHeight() / scaleRatio)) / 2);
        int yi = (int) Math.min(Math.round(y), image.getHeight() - 1);

        int color = (x < 0 || y < 0) ? LXColor.BLACK : image.getColor(xi, yi);
        colors[point.index] = color;
    }

}
*/

