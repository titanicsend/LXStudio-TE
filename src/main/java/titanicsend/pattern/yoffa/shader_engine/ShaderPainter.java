package titanicsend.pattern.yoffa.shader_engine;

import heronarts.lx.model.LXPoint;
import titanicsend.model.TEModel;
import titanicsend.model.TEPanelModel;
import titanicsend.util.Dimensions;

import java.util.Collection;

import static java.lang.Math.abs;

// just a super lightweight version of ImagePainter that we can instantiate
// outside the frame handler.   TODO - This also gives us a place to experiment with
// caching the calculated texture coordinates instead of recalculating them every
// frame.
public class ShaderPainter {
    private int[][] image;
    private int[] colors;

    public ShaderPainter() {
        this.image = null;
        this.colors = null;
    }

    public void setImage(int[][] img) { this.image = img; }
    public void setColors(int[] c) { this.colors = c;}

    private int imageWidth() {
        return image.length;
    }

    private int imageHeight() {
        return image[0].length;
    }

    public void paint(Collection<? extends TEModel> panels) {
        Dimensions dimensions = Dimensions.fromModels(panels);


        for (TEModel panel : panels) {
            for (LXPoint point : panel.getPoints()) {
                paint(point, dimensions);
            }
        }
    }

    public void paint(LXPoint point, Dimensions canvasDimensions) {
        // here the 'z' dimension of TE corresponds with 'x' dimension of the image based on the side that
        // we're painting
        float normalizedX;

        if (canvasDimensions.widerOnZThanX()) {
            normalizedX = (point.zn - canvasDimensions.getMinZn()) / canvasDimensions.getDepthNormalized();
        } else {
            normalizedX = (point.xn - canvasDimensions.getMinXn()) / canvasDimensions.getWidthNormalized();
        }
        float normalizedY = (point.yn - canvasDimensions.getMinYn()) / canvasDimensions.getHeightNormalized();

        float x = (1 - normalizedX) * imageWidth();
        int xi = (int) Math.max(0,Math.min(Math.round(x), imageWidth() - 1));

        float y = normalizedY * imageHeight();
        int yi = (int) Math.max(0,Math.min(Math.round(y), imageHeight() - 1));

        colors[point.index] = image[xi][yi];
    }
}
