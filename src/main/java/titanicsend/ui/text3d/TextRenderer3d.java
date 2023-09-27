package titanicsend.ui.text3d;

import static org.lwjgl.bgfx.BGFX.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import heronarts.glx.ui.UI;
import org.lwjgl.bgfx.BGFXVertexLayout;
import org.lwjgl.system.MemoryUtil;

import heronarts.glx.GLX;
import heronarts.glx.GLXUtils;
import heronarts.glx.View;

public class TextRenderer3d {
    private GLX glx;
    private final static float TEXT3D_SCALE = 10000f;
    private final List<Label> labels = new ArrayList<Label>();
    private ByteBuffer fontTexture;
    private short textureHandle;
    private BGFXVertexLayout vertexLayout;
    private short program;
    private short uniformTexture;
    private ByteBuffer vsCode;
    private ByteBuffer fsCode;
    public float atlasWidth;
    public float atlasHeight;

    public TextRenderer3d(GLX glx, ByteBuffer buffer, int width, int height) {
        this.glx = glx;
        this.atlasWidth = width;
        this.atlasHeight = height;

        // create a font atlas texture from the supplied buffer
        this.fontTexture = buffer;
        this.textureHandle = bgfx_create_texture_2d(width, height, false, 1, BGFX_TEXTURE_FORMAT_RGBA8,
            BGFX_SAMPLER_NONE,
            bgfx_make_ref(this.fontTexture));

        // Vertex buffer layout: 3 position + 2 texture coordinates per character,
        // plus 4 byte vertex color information in ARGB format.
        this.vertexLayout = BGFXVertexLayout.calloc();
        bgfx_vertex_layout_begin(this.vertexLayout, glx.getRenderer());
        bgfx_vertex_layout_add(this.vertexLayout, BGFX_ATTRIB_POSITION, 3,
            BGFX_ATTRIB_TYPE_FLOAT, false, false);
        bgfx_vertex_layout_add(this.vertexLayout, BGFX_ATTRIB_TEXCOORD0, 2,
            BGFX_ATTRIB_TYPE_FLOAT, true, false);
        bgfx_vertex_layout_add(this.vertexLayout, BGFX_ATTRIB_COLOR0,
            4, BGFX_ATTRIB_TYPE_UINT8,false,false);
        bgfx_vertex_layout_end(this.vertexLayout);

        // load and configure texture shaders.
        // TODO - we're borrowing these shaders from the Chromatik's UI code.  We may want
        // TODO - to get fancier with custom shaders eventually.
        try {
            this.vsCode = GLXUtils.loadShader(glx, "vs_view2d");
            this.fsCode = GLXUtils.loadShader(glx, "fs_view2d");
            this.program = bgfx_create_program(
                bgfx_create_shader(bgfx_make_ref(this.vsCode)),
                bgfx_create_shader(bgfx_make_ref(this.fsCode)), true);
            this.uniformTexture = bgfx_create_uniform("s_texColor",
                BGFX_UNIFORM_TYPE_SAMPLER, 1);

        } catch (IOException iox) {
            throw new RuntimeException(iox);
        }
    }

    public void addLabel(TextManager3d t, Label l) {
        this.labels.add(l);
        this.buildRenderBuffers(t, l);
    }

    public void buildRenderBuffers(TextManager3d t, Label l) {

        int lineWidth = t.getWidth(l.text);

        float drawX = TEXT3D_SCALE * -lineWidth / 2f;
        float drawY = TEXT3D_SCALE * -t.getHeight() / 2f;
        float drawZ = 0;

        // at this point, we set up vertex buffers at initialization so we won't need this.
        // but if we ever decide to change text dynamically, it's here, ready to go.
        if (l.vertexBuffer != null) {
            MemoryUtil.memFree(l.vertexBuffer);
        }

        // vertex layout - 3 position coords, 2 texture coords, 4 bytes of color
        int bytesPerVertex = 5 * Float.BYTES + 4 * Byte.BYTES;
        l.vertexBuffer = MemoryUtil.memAlloc(l.vertexCount * bytesPerVertex);

        // generate quads and triangle strip data for the entire
        // string centered at origin (0,0,0).  Then translate and rotate
        // to the correct position at frame generation time.
        for (int i = 0; i < l.text.length(); i++) {
            char ch = l.text.charAt(i);
            GlyphInfo g = t.getGlyph(ch);
            float glyphHeight = g.height * TEXT3D_SCALE;
            float glyphWidth = g.width * TEXT3D_SCALE;

            //
            // Glyph vertex layout:
            // TODO - glyphs may be upside down on OpenGL.  No way to test this ATM.
            // 0 --- 1
            // |  /  |
            // 3 --- 2
            // the two triangles are [0,1,3] and [3,1,2]

            // glyph vertex 0
            l.vertexBuffer.putFloat(drawX);
            l.vertexBuffer.putFloat(drawY);
            l.vertexBuffer.putFloat(drawZ);
            l.vertexBuffer.putFloat(g.x / atlasWidth);
            l.vertexBuffer.putFloat((g.y + g.height) / atlasHeight);
            l.vertexBuffer.putInt(l.color);

            // vertex 1
            l.vertexBuffer.putFloat(drawX + glyphWidth);
            l.vertexBuffer.putFloat(drawY);
            l.vertexBuffer.putFloat(drawZ);
            l.vertexBuffer.putFloat((float) (g.x + g.width) / atlasWidth);
            l.vertexBuffer.putFloat((float) (g.y + g.height) / atlasHeight);
            l.vertexBuffer.putInt(l.color);

            // vertex 3
            l.vertexBuffer.putFloat(drawX);
            l.vertexBuffer.putFloat(drawY + glyphHeight);
            l.vertexBuffer.putFloat(drawZ);
            l.vertexBuffer.putFloat(g.x / atlasWidth);
            l.vertexBuffer.putFloat(g.y / atlasHeight);
            l.vertexBuffer.putInt(l.color);

            // vertex 3
            l.vertexBuffer.putFloat(drawX);
            l.vertexBuffer.putFloat(drawY + glyphHeight);
            l.vertexBuffer.putFloat(drawZ);
            l.vertexBuffer.putFloat(g.x / atlasWidth);
            l.vertexBuffer.putFloat(g.y / atlasHeight);
            l.vertexBuffer.putInt(l.color);

            // vertex 1
            l.vertexBuffer.putFloat(drawX + glyphWidth);
            l.vertexBuffer.putFloat(drawY);
            l.vertexBuffer.putFloat(drawZ);
            l.vertexBuffer.putFloat((float) (g.x + g.width) / atlasWidth);
            l.vertexBuffer.putFloat((float) (g.y + g.height) / atlasHeight);
            l.vertexBuffer.putInt(l.color);

            // vertex 2
            l.vertexBuffer.putFloat(drawX + glyphWidth);
            l.vertexBuffer.putFloat(drawY + glyphHeight);
            l.vertexBuffer.putFloat(drawZ);
            l.vertexBuffer.putFloat((g.x + g.width) / atlasWidth);
            l.vertexBuffer.putFloat( g.y / atlasHeight);
            l.vertexBuffer.putInt(l.color);

            drawX += glyphWidth;
        }

        l.vertexBuffer.flip();
        l.vbh = bgfx_create_vertex_buffer(bgfx_make_ref(l.vertexBuffer), vertexLayout, BGFX_BUFFER_NONE);
    }

    public void submit(UI ui, View view, Label l) {
        l.modelMatrix.get(l.modelMatrixBuf);
        bgfx_set_transform(l.modelMatrixBuf);
        bgfx_set_texture(0, this.uniformTexture, textureHandle, BGFX_SAMPLER_NONE);
        final long state =
                BGFX_STATE_WRITE_RGB |
                BGFX_STATE_WRITE_A |
                BGFX_STATE_WRITE_Z |
                BGFX_STATE_DEPTH_TEST_LESS;

        bgfx_set_state(state, 0);
        bgfx_set_vertex_buffer(0, l.vbh, 0, l.vertexCount);
        bgfx_submit(view.getId(), this.program, 0, BGFX_DISCARD_ALL);

    }

    public void draw(UI ui, View view) {
        for (Label l : this.labels) {
            this.submit(ui, view, l);
        }
    }

    public void dispose() {
        bgfx_destroy_texture(this.textureHandle);
        MemoryUtil.memFree(this.fontTexture);
        MemoryUtil.memFree(this.vsCode);
        MemoryUtil.memFree(this.fsCode);
        this.vertexLayout.free();
        bgfx_destroy_uniform(this.uniformTexture);
        bgfx_destroy_program(this.program);
    }
}
