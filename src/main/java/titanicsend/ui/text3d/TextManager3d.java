package titanicsend.ui.text3d;

import heronarts.glx.GLX;
import heronarts.glx.View;
import heronarts.lx.color.LXColor;
import java.util.List;
import org.joml.Vector3f;

public class TextManager3d {
    private final TextRenderer3d renderer;

    // multiplier to generate final font size in world space units

    private float font3dScale = 10000f;
    private int fontForeground = LXColor.WHITE;
    private int fontBackground = LXColor.BLACK;

    /**
     * Initialize a TextManager3d object with a font atlas file path
     * TODO - only a single font can currently be associated with a text manager/renderer
     * TODO - Are we ever going to need more than this?
     *
     * @param glx      currently active GLX object
     * @param fontPath relative (from project root) path to .font3d file
     */
    public TextManager3d(GLX glx, String fontPath) {
        renderer = new TextRenderer3d(glx, fontPath);
    }

    public Label labelMaker(String text, Vector3f pos, Vector3f rot) {
        Label l = new Label(text, pos, rot, fontForeground, fontBackground);
        renderer.buildRenderBuffers(this, l);
        return l;
    }

    public float getFontScale() {
        return font3dScale;
    }

    /**
     * sets the font scale multiplier.  Default is 10000, which yields a font
     * that, when rendered in 3D, appears to be somewhere between 10 and 16 points.
     * (on the TE model, it looks like it'd be a bit over a foot high.)
     * Does not retroactively change the size of existing labels, so to change
     * font size as you add labels, you'll need to call this before creating them.
     *
     * @param scale - multiplier
     */
    public void setFontScale(float scale) {
        font3dScale = scale;
    }

    /**
     * Sets the font color for all new labels.  Does not retroactively change the color of
     * existing labels.
     *
     * @param color packed LX color
     */
    public void setFontColor(int color) {
        fontForeground = color;
    }

    /**
     * Sets the font background color for all new labels.  Does not retroactively change f
     * existing labels.
     *
     * @param color packed LX color
     */
    public void setFontBackground(int color) {
        fontBackground = color;
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
