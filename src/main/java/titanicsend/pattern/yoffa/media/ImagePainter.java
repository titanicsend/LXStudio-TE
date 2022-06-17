package titanicsend.pattern.yoffa.media;

import heronarts.lx.color.LXColor;
import heronarts.lx.model.LXPoint;
import titanicsend.model.TEModel;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEPanelSection;
import titanicsend.util.Dimensions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static java.lang.Math.abs;

public class ImagePainter {

    private final BufferedImage image;
    private final int[] colors;

    public ImagePainter(String imagePath, int[] colors) throws IOException {
        this(ImageIO.read(new File(imagePath)), colors);
    }

    public ImagePainter(BufferedImage bufferedImage, int[] colors) {
        this.image = bufferedImage;
        this.colors = colors;
    }

    public Color getColorForNormalizedCoordinates(double xn, double yn) {
        xn = abs(xn) % 1;
        yn = abs(yn) % 1;
        int x = (int) Math.floor(image.getWidth() * xn);
        int y = (int) Math.floor(image.getHeight() * yn);
        return new Color(image.getRGB(x, y));
    }

    public void paint(Collection<TEPanelModel> panels) {
        paint(panels, 1);
    }

    public void paint(Collection<? extends TEModel> panels, double scaleRatio) {
        Dimensions dimensions = Dimensions.fromModels(panels);

        for (TEModel panel : panels) {
            for (LXPoint point : panel.getPoints()) {
                // here the 'z' dimension of TE corresponds with 'x' dimension of the image based on the side that
                //   we're painting
                float normalizedX = (point.zn - dimensions.getMinZn()) / dimensions.getDepthNormalized();
                float normalizedY = (point.yn - dimensions.getMinYn()) / dimensions.getHeightNormalized();

                double x = (1 - normalizedX) * image.getWidth();
                x = x / scaleRatio + ((image.getWidth()-(image.getWidth() / scaleRatio)) / 2);
                int xi = (int) Math.min(Math.round(x), image.getWidth() - 1);

                double y = (1 - normalizedY) * image.getHeight();
                y = y / scaleRatio + ((image.getHeight()-(image.getHeight() / scaleRatio)) / 2);
                int yi = (int) Math.min(Math.round(y), image.getHeight() - 1);

                int color = (x < 0 || y < 0) ? LXColor.BLACK : image.getRGB(xi, yi);
                colors[point.index] = color;
            }
        }
    }

}
