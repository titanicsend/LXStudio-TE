package titanicsend.pattern.yoffa.media;

import static java.lang.Math.abs;

import heronarts.lx.model.LXPoint;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ImagePainter {

  private final ImageSource image;
  private int[] colors;

  public ImagePainter(String imagePath) throws IOException {
    this(imagePath, null);
  }

  public ImagePainter(String imagePath, int[] colors) throws IOException {
    this(ImageIO.read(new File(imagePath)), colors);
  }

  public ImagePainter(BufferedImage bufferedImage, int[] colors) {
    this.image = new BufferedImageSource(bufferedImage);
    this.colors = colors;
  }

  public ImagePainter(int[][] image, int[] colors) {
    this.image = new ArrayBackedImageSource(image);
    this.colors = colors;
  }

  /**
   * The colors[] array is no longer available to patterns at constructor time so this will need to
   * be called from onActive()
   */
  public void initColors(int[] colors) {
    this.colors = colors;
  }

  public Color getColorForNormalizedCoordinates(double xn, double yn) {
    xn = abs(xn) % 1;
    yn = abs(yn) % 1;
    int x = (int) Math.floor(image.getWidth() * xn);
    int y = (int) Math.floor(image.getHeight() * yn);
    return new Color(image.getColor(x, y));
  }

  public void paint(LXPoint point) {
    paint(point, 1);
  }

  public void paint(LXPoint point, double scaleRatio) {
    // here the 'z' dimension of TE corresponds with 'x' dimension of the image based on the side
    // that
    //   we're painting
    double x = (1 - point.zn) * image.getWidth();
    x = x / scaleRatio + ((image.getWidth() - (image.getWidth() / scaleRatio)) / 2);
    int xi = (int) Math.max(0, Math.min(Math.round(x), image.getWidth() - 1));

    double y = (1 - point.yn) * image.getHeight();
    y = y / scaleRatio + ((image.getHeight() - (image.getHeight() / scaleRatio)) / 2);
    int yi = (int) Math.max(0, Math.min(Math.round(y), image.getHeight() - 1));

    colors[point.index] = image.getColor(xi, yi);
  }

  public interface ImageSource {
    int getWidth();

    int getHeight();

    int getColor(int x, int y);
  }
}
