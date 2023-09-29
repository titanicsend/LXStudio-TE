package titanicsend.ui.text3d;

import static org.lwjgl.bgfx.BGFX.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.lwjgl.bgfx.BGFX;
import org.lwjgl.bgfx.BGFXVertexLayout;
import org.lwjgl.system.MemoryUtil;
import heronarts.glx.GLX;
import heronarts.glx.View;
import titanicsend.util.TE;

public class TextRenderer3d {
    private ByteBuffer fontTexture;
    private FloatBuffer colorBuffer;
    private FloatBuffer backgroundBuffer;
    private final short textureHandle;
    private final BGFXVertexLayout vertexLayout;
    private final short program;
    private final short uniformTexture;
    private final short uniformColor;
    private final short uniformBackground;
    private final ByteBuffer vsCode;
    private final ByteBuffer fsCode;
    public float atlasWidth;
    public float atlasHeight;

    public TextRenderer3d(GLX glx, ByteBuffer buffer, int width, int height) {
        this.atlasWidth = width;
        this.atlasHeight = height;

        // create a font atlas texture from the supplied buffer
        this.fontTexture = buffer;
        this.textureHandle = bgfx_create_texture_2d(width, height, false, 1, BGFX_TEXTURE_FORMAT_R8,
            BGFX_SAMPLER_NONE,
            bgfx_make_ref(this.fontTexture));

        // allocate memory for font foreground and background color uniforms
        colorBuffer = MemoryUtil.memAllocFloat(4);
        backgroundBuffer = MemoryUtil.memAllocFloat(4);

        // Vertex buffer layout: 3 position + 2 texture coordinates per character,
        // plus 4 byte vertex color information in ARGB format.
        this.vertexLayout = BGFXVertexLayout.calloc();
        bgfx_vertex_layout_begin(this.vertexLayout, glx.getRenderer());
        bgfx_vertex_layout_add(this.vertexLayout, BGFX_ATTRIB_POSITION, 3,
            BGFX_ATTRIB_TYPE_FLOAT, false, false);
        bgfx_vertex_layout_add(this.vertexLayout, BGFX_ATTRIB_TEXCOORD0, 2,
            BGFX_ATTRIB_TYPE_FLOAT, true, false);
        bgfx_vertex_layout_add(this.vertexLayout, BGFX_ATTRIB_COLOR0,
            4, BGFX_ATTRIB_TYPE_UINT8, false, false);
        bgfx_vertex_layout_end(this.vertexLayout);

        // load and configure our custom texture shaders.
        try {
            this.vsCode = loadCustomBGFXShader(glx, "vs_font3d");
            this.fsCode = loadCustomBGFXShader(glx, "fs_font3d");
            this.program = bgfx_create_program(
                bgfx_create_shader(bgfx_make_ref(this.vsCode)),
                bgfx_create_shader(bgfx_make_ref(this.fsCode)), true);

            this.uniformColor = bgfx_create_uniform("u_color", 2, 1);
            this.uniformBackground = bgfx_create_uniform("u_background", 2, 1);
            this.uniformTexture = bgfx_create_uniform("s_texFont",
                BGFX_UNIFORM_TYPE_SAMPLER, 1);

        } catch (IOException iox) {
            throw new RuntimeException(iox);
        }
    }

    public ByteBuffer loadCustomBGFXShader(GLX glx, String name) throws IOException {

        // get the shader path for the current renderer
        // we support dx11, opengl, and metal
        // TODO - add Vulkan if/when Chromatik adds support
        String path = "resources/shaders/bgfx/";
        switch (glx.getRenderer()) {
            case 3:
            case 4:
                path = path + "dx11/";
                break;
            case 6:
                path = path + "metal/";
                break;
            case 9:
                path = path + "glsl/";
                break;
            default:
                throw new IOException("Custom shaders are not supported on " + bgfx_get_renderer_name(glx.getRenderer()));
        }

        try {
            path = path + name + ".bin";

            // read file into a native buffer
            byte[] fileData = Files.readAllBytes(Path.of(path));
            ByteBuffer outBuf = MemoryUtil.memAlloc(fileData.length);
            outBuf.put(fileData);
            outBuf.flip();
            return outBuf;

        } catch (IOException e) {
            TE.err("Unable to load shader " + name + " from " + path);
            return null;
        }
    }


    public void buildRenderBuffers(TextManager3d t, Label l) {

        int lineWidth = t.getWidth(l.text);

        float drawX = t.getFontScale() * -lineWidth / 2f;
        float drawY = t.getFontScale() * -t.getHeight(l.text) / 2f;
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
            float glyphHeight = g.height * t.getFontScale();
            float glyphWidth = g.width * t.getFontScale();

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
            l.vertexBuffer.putFloat((g.x + g.width) / atlasWidth);
            l.vertexBuffer.putFloat((g.y + g.height) / atlasHeight);
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
            l.vertexBuffer.putFloat((g.x + g.width) / atlasWidth);
            l.vertexBuffer.putFloat((g.y + g.height) / atlasHeight);
            l.vertexBuffer.putInt(l.color);

            // vertex 2
            l.vertexBuffer.putFloat(drawX + glyphWidth);
            l.vertexBuffer.putFloat(drawY + glyphHeight);
            l.vertexBuffer.putFloat(drawZ);
            l.vertexBuffer.putFloat((g.x + g.width) / atlasWidth);
            l.vertexBuffer.putFloat(g.y / atlasHeight);
            l.vertexBuffer.putInt(l.color);

            drawX += glyphWidth;
        }

        l.vertexBuffer.flip();
        l.vbh = bgfx_create_vertex_buffer(bgfx_make_ref(l.vertexBuffer), vertexLayout, BGFX_BUFFER_NONE);
    }

    // convert color to float array and place in uniform buffer
    public void setColorUniform(int color,FloatBuffer buffer) {
        buffer.put(0, (float) (color >>> 16 & 0xFF) / 255.0F);
        buffer.put(1, (float) (color >>> 8 & 0xFF) / 255.0F);
        buffer.put(2, (float) (color & 0xFF) / 255.0F);
        buffer.put(3, (float) (color >>> 24 & 0xFF) / 255.0F);
    }

    public void draw(View view, Label l) {
        l.modelMatrix.get(l.modelMatrixBuf);
        bgfx_set_transform(l.modelMatrixBuf);
        setColorUniform(l.color,colorBuffer);
        setColorUniform(l.background,backgroundBuffer);
        BGFX.bgfx_set_uniform(uniformColor, colorBuffer, 1);
        BGFX.bgfx_set_uniform(uniformBackground, backgroundBuffer, 1);
        bgfx_set_texture(0, this.uniformTexture, textureHandle, BGFX_SAMPLER_NONE);
        final long state =
            BGFX_STATE_WRITE_RGB |
                BGFX_STATE_WRITE_A |
                BGFX_STATE_WRITE_Z |
                BGFX_STATE_BLEND_ALPHA |
                BGFX_STATE_DEPTH_TEST_LESS;

        bgfx_set_state(state, 0);
        bgfx_set_vertex_buffer(0, l.vbh, 0, l.vertexCount);
        bgfx_submit(view.getId(), this.program, 0, BGFX_DISCARD_ALL);
    }

    public void dispose() {
        bgfx_destroy_texture(this.textureHandle);
        MemoryUtil.memFree(this.fontTexture);
        MemoryUtil.memFree(this.vsCode);
        MemoryUtil.memFree(this.fsCode);
        MemoryUtil.memFree(colorBuffer);
        MemoryUtil.memFree(backgroundBuffer);
        this.vertexLayout.free();
        bgfx_destroy_uniform(this.uniformTexture);
        bgfx_destroy_uniform(this.uniformColor);
        bgfx_destroy_uniform(this.uniformBackground);
        bgfx_destroy_program(this.program);
    }
}
