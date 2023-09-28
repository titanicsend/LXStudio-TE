package titanicsend.ui.text3d;

/*
 TrueType font rendering for Chromatik's 3D window
 2023 ZRanger1

 Font loader adapted for BGFX from:
    SilverTiger OpenGL font tutorial
    https://github.com/SilverTiger/lwjgl3-tutorial/wiki/Fonts
    Copyright © 2015-2017, Heiko Brumme
    MIT License
*/

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import heronarts.glx.GLX;
import heronarts.glx.View;
import heronarts.lx.color.LXColor;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;

import static java.awt.Font.MONOSPACED;
import static java.awt.Font.PLAIN;
import static java.awt.Font.TRUETYPE_FONT;

public class TextManager3d {
    private final Map<Character, GlyphInfo> glyphs;
    private final TextRenderer3d renderer;
    private int fontHeight;
    // multiplier to generate final font size in world space units
    // the default multiplier of 10000 yields a world font height of
    // 34000mm
    private float font3dScale = 10000f;
    private int fontColor = LXColor.WHITE;

    public TextManager3d(GLX glx, int size) {
        this(glx, new java.awt.Font(MONOSPACED, PLAIN, size));
    }

    /**
     * Initialize a TextManager3d object with a truetype font from an input stream.
     * TODO - only a single font can currently be associated with a text manager
     * TODO - Are we ever going to need more than this?
     *
     * @param glx currently active GLX object
     * @param in   input stream to truetype font file
     * @param size Font size
     * @throws FontFormatException if the font file doesn't contain the required
     *                             font tables
     * @throws IOException         if the font file can't be read
     */
    public TextManager3d(GLX glx, InputStream in, int size) throws FontFormatException, IOException {
        this(glx, java.awt.Font.createFont(TRUETYPE_FONT, in).deriveFont(PLAIN, size));
    }

    /**
     * Initialize TextManager3d with an existing awt font object
     * @param glx currently active GLX object
     * @param font awt font object
     */
    public TextManager3d(GLX glx, java.awt.Font font) {
        glyphs = new HashMap<>();
        renderer = initializeFont(glx, font);
    }

    public Label labelMaker(String text, Vector3f pos, Vector3f rot) {
        Label l = new Label(text, pos, rot, fontColor);
        renderer.buildRenderBuffers(this,l);
        return l;
    }


    // create the actual font texture atlas
    private TextRenderer3d initializeFont(GLX glx, java.awt.Font font) {
        int imageWidth = 0;
        int imageHeight = 0;

        // make a pass through the font to determine overall width and height
        for (int i = 32; i < 256; i++) {
            char c = (char) i;
            BufferedImage ch = createCharImage(font, c);
            if (ch == null) {
                // character not in this font...
                continue;
            }
            imageWidth += ch.getWidth();
            imageHeight = Math.max(imageHeight, ch.getHeight());
        }

        fontHeight = imageHeight;

        // create an image for our output glyph set
        BufferedImage image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        float x = 0;

        // Generate glyphs for standard printable characters, starting with SPACE
        // (0-31 are control codes, so we skip them.)
        for (int i = 32; i < 256; i++) {
            char c = (char) i;
            BufferedImage charImage = createCharImage(font, c);
            if (charImage == null) {
                // character not in this font...
                continue;
            }

            float charWidth  = charImage.getWidth();
            float charHeight = charImage.getHeight();

            // draw character to atlas image
            g.drawImage(charImage, (int) x, 0, null);

            // Store character info size and texture index info
            GlyphInfo ch = new GlyphInfo(charWidth, charHeight, x, 0); //image.getHeight() - charHeight);
            glyphs.put(c, ch);
            x += charWidth;
        }

        // if rendering on OpenGL, flip image to move origin to bottom left
        // (We don't currently have a way to test this, but may eventually need
        // it for Linux support, so here it is!. Doing it here means we don't
        // have to flip every character at frame time.)
        if (glx.isOpenGL()) {
            AffineTransform transform = AffineTransform.getScaleInstance(1f, -1f);
            transform.translate(0, -image.getHeight());
            AffineTransformOp operation = new AffineTransformOp(transform,
                AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            image = operation.filter(image, null);
        }

        // move image data into a ByteBuffer in ARGB order
        int width = image.getWidth();
        int height = image.getHeight();

        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        ByteBuffer buffer = MemoryUtil.memAlloc(width * height * 4);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                int pixel = pixels[i * width + j];
                buffer.putInt(LXColor.toABGR(pixel));
            }
        }
        // prepare buffer for use by graphics subsystem
        buffer.flip();

        // go ye, and create a TextRenderer3d from our new font image ByteBuffer
        return new TextRenderer3d(glx, buffer, width, height);
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

    /**
     * Gets the width of the specified text.
     *
     * @param text A text string
     * @return The width of the specified text in pixels
     */
    public int getWidth(String text) {
        int lineWidth = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            GlyphInfo g = glyphs.get(c);
            lineWidth += (int) g.width;
        }
        // just to make sure we don't get any negative width control characters...
        return Math.max(0, lineWidth);
    }

    public int getHeight() {
        return fontHeight;
    }

    public GlyphInfo getGlyph(char c) {
        return glyphs.get(c);
    }


    public float getFontScale() {
        return font3dScale;
    }

    /** sets the font scale multiplier.  Default is 10000, which yields a font
     *  that, when rendered in 3D, appears to be somewhere between 10 and 16 points.
     *  (on the TE model, it looks like it'd be a bit over a foot high.)
     *  Does not retroactively change the size of existing labels, so to change
     *  font size as you add labels, you'll need to call this before creating them.
     *
     *  @param scale - multiplier
     */
    public void setFontScale(float scale) {
        font3dScale = scale;
    }

    /**
     * Sets the font color for all new labels.  Does not retroactively change the color of
     * existing labels.
     * @param color packed LX color
     */
    public void setFontColor(int color) {
        fontColor = color;
    }

    public void draw(View view, List<Label> labels) {
        for (Label l : labels) {
            renderer.draw(view, l);
        }
    }

    public void dispose() {
        renderer.dispose();
    }
}


