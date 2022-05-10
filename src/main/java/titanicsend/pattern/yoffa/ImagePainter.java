package titanicsend.pattern.yoffa;

import heronarts.lx.model.LXPoint;
import titanicsend.model.TEPanelModel;
import titanicsend.model.TEPanelSection;
import titanicsend.util.Dimensions;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

public class ImagePainter {

    private final BufferedImage image;
    private final int[] colors;

    public ImagePainter(String imagePath, int[] colors) throws IOException {
        String path = new File(".").getCanonicalPath();
        image = ImageIO.read(new File(imagePath));
        this.colors = colors;
    }

    public void paint(Collection<TEPanelModel> panels) {
        Dimensions dimensions = Dimensions.fromModels(panels);

        for (TEPanelModel panel : panels) {
            for (LXPoint point : panel.getPoints()) {
                // here the 'z' dimension of TE corresponds with 'x' dimension of the image based on the side that
                //   we're painting
                float normalizedX = (point.zn - dimensions.getMinZn()) / dimensions.getDepthNormalized();
                float normalizedY = (point.yn - dimensions.getMinYn()) / dimensions.getHeightNormalized();

                int x = Math.min(Math.round((1 - normalizedX) * image.getWidth()), image.getWidth() - 1);
                int y = Math.min(Math.round((1 - normalizedY) * image.getHeight()), image.getHeight() - 1);
                int color = image.getRGB(x, y);
                colors[point.index] = color;
            }
        }
    }

}
