package titanicsend.ui.text3d;

import static java.awt.Font.PLAIN;
import static java.awt.Font.TRUETYPE_FONT;
import static java.lang.System.exit;

import heronarts.lx.color.LXColor;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * FontMaker - Texture Atlas Creation Tool
 *
 * This is the simplest possible alpha-only texture atlas
 * implementation.  Very fast and lightweight, not a lot
 * of features.
 *
 * Converts Truetype fonts to bitmap fonts with glyph information
 * for each character, for use by the Titanic's End 3D text renderer.
 *
 * To use, just set the input and output filenames in main and
 * run FontMaker from the IDE.
 *
 */
public class FontMaker {
    static final int firstChar = 32;
    static final int lastChar = 255;
    //

    FileInputStream inStream;
    DataOutputStream outStream;

    /**
     * Create font atlas from TTF and write it to a file
     *
     * @param inPath  input stream to truetype font file
     * @param outPath output stream to font atlas data file
     * @param size    Font size
     * @throws FontFormatException if the font file doesn't contain the required
     *                             font tables
     * @throws IOException         if the file can't be read/written
     */
    public FontMaker(String inPath, String outPath, int size) throws FontFormatException, IOException {
        try {
            this.inStream = new FileInputStream(inPath);
            this.outStream = new DataOutputStream(new FileOutputStream(outPath));
        } catch (Exception e) {
            System.out.println("FontMaker: Error creating font: " + e.getMessage());
            exit(1);
        }

        java.awt.Font font = java.awt.Font.createFont(TRUETYPE_FONT, inStream).deriveFont(PLAIN, size);
        inStream.close();

        buildFontAtlas(font);

        outStream.close();
    }

    // create the actual font texture atlas
    private void buildFontAtlas(java.awt.Font font) {
        int imageWidth = 0;
        int imageHeight = 0;

        // make a pass through the font to determine overall width and height
        for (int i = firstChar; i <= lastChar; i++) {
            char c = (char) i;
            BufferedImage ch = createCharImage(font, c);
            if (ch == null) {
                // character not in this font...
                continue;
            }
            imageWidth += ch.getWidth();
            imageHeight = Math.max(imageHeight, ch.getHeight());
        }

        // create an image for our output glyph set
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        // write header information to the file
        try {
            int glyphCount = lastChar - firstChar + 1;
            outStream.writeInt(glyphCount);
            outStream.writeInt(imageWidth);
            outStream.writeInt(imageHeight);
        } catch (Exception e) {
            System.out.println("FontMaker: Error writing font: " + e.getMessage());
            exit(1);
        }

        float x = 0;

        // Generate glyphs for standard printable characters, starting with SPACE
        // (0-31 are control codes, so we skip them) and write them to the output file
        for (int i = firstChar; i <= lastChar; i++) {
            char c = (char) i;
            BufferedImage charImage = createCharImage(font, c);
            if (charImage == null) {
                // character not in this font...
                continue;
            }

            float charWidth = charImage.getWidth();
            float charHeight = charImage.getHeight();

            // draw character to atlas image
            g.drawImage(charImage, (int) x, 0, null);

            // Save character glyph info -- character size and position in texture atlas
            try {
                outStream.writeInt((int) charWidth);
                outStream.writeInt((int) charHeight);
                outStream.writeInt((int) x);
                outStream.writeInt((int) (image.getHeight() - charHeight));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            x += charWidth;
        }

        // get a copy of the full RGBA image data and use it to create
        // a compact alpha channel-only texture for use by the renderer.
        int width = image.getWidth();
        int height = image.getHeight();

        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        try {
            for (int i = 0; i < height; i++) {
                for (int j = 0; j < width; j++) {
                    outStream.write((byte) (0xFF & LXColor.alpha(pixels[i * width + j])));
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private BufferedImage createCharImage(java.awt.Font font, char c) {
        // Create a small temporary image so we can get font metrics for this character
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics();
        g.dispose();

        int charWidth = metrics.charWidth(c);
        int charHeight = metrics.getHeight();

        // This should actually never happen...
        if (charWidth == 0) {
            return null;
        }

        // Create actual glyph image for this character
        image = new BufferedImage(charWidth, charHeight, BufferedImage.TYPE_INT_ARGB);
        g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(font);
        g.setPaint(java.awt.Color.WHITE);
        g.drawString(String.valueOf(c), 0, metrics.getAscent());
        g.dispose();
        return image;
    }

    public static void main(String[] args) {
        final String inFile = "resources/fonts/Inconsolata.ttf";
        final String outFile = "resources/fonts/Inconsolata.font3d";
        final int sizeInPoints = 26;

        try {
            new FontMaker(inFile, outFile, sizeInPoints);
        } catch (Exception e) {
            System.out.println("FontMaker: Error creating font: " + e.getMessage());
            exit(1);
        }

        System.out.println("FontMaker: All done!");
        exit(0);
    }
}
