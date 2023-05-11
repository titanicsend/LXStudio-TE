package titanicsend.pattern.yoffa.shader_engine;

import heronarts.lx.model.LXPoint;

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

    public void paint(LXPoint point) {
        // here the 'z' dimension of TE corresponds with 'x' dimension of the image based on the side that
        // we're painting

        // calculate so that if normalized x/y are always in range 0-1, x and y will be
        // limited to between the proper image array index range, no clamping is needed
        int xi = Math.round((1f - point.zn) * (imageWidth() - 1f));
        int yi = Math.round(point.yn * (imageHeight() - 1f));

        colors[point.index] = image[xi][yi];
    }
}
