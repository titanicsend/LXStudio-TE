package titanicsend.ui.text3d;

// Texture atlas size and offset information for a single glyph
public class GlyphInfo {

    public final float width;   // width of glyph in pixels
    public final float height;  // height of glyph in pixels
    public final float x;       // x offset of glyph in texture atlas
    public final float y;       // y offset of glyph in texture atlas

    public GlyphInfo(float width, float height, float x, float y) {
        this.width = width;
        this.height = height;
        this.x = x;
        this.y = y;
    }
}
