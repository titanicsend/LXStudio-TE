package titanicsend.pattern.yoffa.text;

import heronarts.lx.model.LXPoint;
import titanicsend.model.TEModel;
import titanicsend.model.TEPanelModel;
import titanicsend.pattern.yoffa.media.ArrayBackedImageSource;
import titanicsend.pattern.yoffa.media.BufferedImageSource;
import titanicsend.util.Dimensions;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static java.lang.Math.abs;

public class TextPainter {

    private int[] colors;

    public TextPainter(int[] colors) {
        this.colors = colors;
    }

    /**
     * The colors[] array is no longer available to patterns at constructor time
     * so this will need to be called from onActive()
     */
    public void initColors(int[] colors) {
      this.colors = colors;
    }

    public void stencil(Collection<? extends TEModel> models, String text, String fontName,
                        double scaleRatio, double xOffset, double yOffset, double angle) {
        try {
            BufferedImage textImage = TextFactory.stringToBufferedImage(text, fontName);
            textImage = rotateImageByDegrees(textImage, angle);

            Dimensions dimensions = Dimensions.fromModels(models);
            for (TEModel model : models) {
                for (LXPoint point : model.getPoints()) {
                    paint(point, textImage, dimensions, scaleRatio, xOffset, yOffset);
                }
            }
        } catch (Exception e) {
            // failure case, blank out everything
            for (TEModel model : models) {
                for (LXPoint point : model.getPoints()) {
                    colors[point.index] = 0;
                }
            }
        }
    }

    private void paint(LXPoint point, BufferedImage textImage, Dimensions canvasDimensions,
                       double scaleRatio, double xOffset, double yOffset) {
        // here the 'z' dimension of TE corresponds with 'x' dimension of the image based on the side that
        //   we're painting
        float normalizedX;
        if (canvasDimensions.widerOnZThanX()) {
            normalizedX = (point.zn - canvasDimensions.getMinZn()) / canvasDimensions.getDepthNormalized();
        } else {
            normalizedX = (point.xn - canvasDimensions.getMinXn()) / canvasDimensions.getWidthNormalized();
        }
        float normalizedY = (point.yn - canvasDimensions.getMinYn()) / canvasDimensions.getHeightNormalized();

        double x = (1 - normalizedX) * textImage.getWidth();
        x = x / scaleRatio + ((textImage.getWidth()-(textImage.getWidth() / scaleRatio)) / 2);
        x += xOffset * textImage.getWidth();
        int xi = (int) Math.round(x);

        double y = (1 - normalizedY) * textImage.getHeight();
        y = y / scaleRatio + ((textImage.getHeight()-(textImage.getHeight() / scaleRatio)) / 2);
        y += yOffset * textImage.getHeight();
        int yi = (int) Math.round(y);

        if (xi < 0 || xi >= textImage.getWidth() || yi < 0 || yi >= textImage.getHeight()) {
            colors[point.index] = 0;
            return;
        }

        int color = textImage.getRGB(xi, yi);
        if (color == 0) {
            colors[point.index] = color;
        }
    }

    public BufferedImage rotateImageByDegrees(BufferedImage img, double angle) {
        double rads = Math.toRadians(angle);
        double sin = Math.abs(Math.sin(rads)), cos = Math.abs(Math.cos(rads));
        int w = img.getWidth();
        int h = img.getHeight();
        int newWidth = (int) Math.floor(w * cos + h * sin);
        int newHeight = (int) Math.floor(h * cos + w * sin);

        BufferedImage rotated = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        AffineTransform at = new AffineTransform();
        at.translate((newWidth - w) / 2, (newHeight - h) / 2);

        int x = w / 2;
        int y = h / 2;

        at.rotate(rads, x, y);
        g2d.setTransform(at);
        g2d.drawImage(img, 0, 0, null);
        g2d.dispose();

        return rotated;
    }

}
