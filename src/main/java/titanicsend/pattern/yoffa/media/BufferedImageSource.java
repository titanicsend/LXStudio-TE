package titanicsend.pattern.yoffa.media;

import java.awt.image.BufferedImage;

public class BufferedImageSource implements ImagePainter.ImageSource {

    private final BufferedImage image;

    public BufferedImageSource(BufferedImage image) {
        this.image = image;
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public int getColor(int x, int y) {
        return image.getRGB(x, y);
    }

}
