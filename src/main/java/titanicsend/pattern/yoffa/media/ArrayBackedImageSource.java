package titanicsend.pattern.yoffa.media;

public class ArrayBackedImageSource implements ImagePainter.ImageSource {

    private final int[][] image;

    public ArrayBackedImageSource(int[][] image) {
        this.image = image;
    }

    @Override
    public int getWidth() {
        return image.length;
    }

    @Override
    public int getHeight() {
        return image[0].length;
    }

    @Override
    public int getColor(int x, int y) {
        return image[x][y];
    }

}
