package titanicsend.pattern.yoffa.text;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TextFactory {

    public static void main(String[] args) throws IOException {
        BufferedImage image = stringToBufferedImage(String.valueOf("TEST".charAt(0)), "TimesNewRoman");
        ImageIO.write(image, "png", new File("/Users/yoffa/Desktop/Sample.png"));
        System.out.println("done!");
    }

    public static List<BufferedImage> getTextByCharacter(String text, String fontName) {
        List<BufferedImage> images = new ArrayList<>(text.length());
        for (char letter : text.toCharArray()) {
            images.add(stringToBufferedImage(String.valueOf(letter), fontName));
        }
        return images;
    }

    public static BufferedImage stringToBufferedImage(String text, String fontName) {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);// Represents an image with 8-bit RGBA color components packed into integer pixels.
        Graphics2D graphics2d = image.createGraphics();
        Font font = new Font(fontName, Font.PLAIN, 24);
        graphics2d.setFont(font);
        FontMetrics fontmetrics = graphics2d.getFontMetrics();
        int width = fontmetrics.stringWidth(text);
        int height = fontmetrics.getHeight();
        graphics2d.dispose();

        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        graphics2d = image.createGraphics();
        graphics2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        graphics2d.setFont(font);
        fontmetrics = graphics2d.getFontMetrics();
        graphics2d.setColor(Color.BLACK);
        graphics2d.drawString(text, 0, fontmetrics.getAscent());
        graphics2d.dispose();

        return image;
    }

}
