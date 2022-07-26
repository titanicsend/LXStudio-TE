package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.LXParameter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.*;
import java.util.HashMap;
import java.util.Map;

import static com.jogamp.opengl.GL.*;
import static titanicsend.pattern.yoffa.shader_engine.GeneralUniforms.*;

// ***************************************************************************
//
// Class to store a user-specified value for a uniform parameter
// in the shader

//Technically we don't need to implement GLEventListener unless we plan on rendering on screen, but let's leave it
//for good practice
public class NativeShader implements GLEventListener {

    //we need to draw an object with a vertex shader to put our fragment shader on
    //literally just create a rectangle that takes up the whole screen to paint on
    //TODO currently we're only able to run one OpenGL pattern at a time. We need to split this into multiple
    //  rectangles so we can display multiple patterns in the frame.
    private static final float[] VERTICES = {
            1.0f, 1.0f, 0.0f,
            1.0f, -1.0f, 0.0f,
            -1.0f, -1.0f, 0.0f,
            -1.0f, 1.0f, 0.0f
    };

    //we are drawing with triangles, so we need two to make our rectangle
    private static final int[] INDICES = {
            0, 1, 2,
            2, 0, 3
    };

    private static final Map<Integer, Integer> INDEX_TO_GL_ENUM = Map.of(
            0, GL_TEXTURE0,
            1, GL_TEXTURE1,
            2, GL_TEXTURE2,
            3, GL_TEXTURE3); //stupid

    private static final String FRAGMENT_SHADER_TEMPLATE =
            ShaderUtils.loadResource("resources/shaders/framework/template.fs");
    private static final String SHADER_BODY_PLACEHOLDER = "{{%shader_body%}}";

    private final FragmentShader fragmentShader;
    private final int xResolution;
    private final int yResolution;

    private final FloatBuffer vertexBuffer;
    private final IntBuffer indexBuffer;
    private final Map<Integer, Texture> textures;
    private final Integer audioChannel;
    private ShaderProgram shaderProgram;
    private long startTime;

    ByteBuffer backBuffer;
    private int[][] snapshot;

    private ShaderOptions shaderOptions;
    private int alphaMask;

    private AudioInfo audioInfo;

    private final int audioTextureWidth;
    private final int audioTextureHeight;
    FloatBuffer audioTextureData;

    // map of user created uniforms.
    protected HashMap<String, GeneralUniforms> uniforms = null;
    // Direct buffers for passing data arrays to the shader as uniforms
    protected IntBuffer intBuffer;
    protected FloatBuffer floatBuffer;

    public NativeShader(FragmentShader fragmentShader, int xResolution, int yResolution, ShaderOptions options) {
        this.xResolution = xResolution;
        this.yResolution = yResolution;
        this.fragmentShader = fragmentShader;
        this.vertexBuffer = Buffers.newDirectFloatBuffer(VERTICES.length);
        this.indexBuffer = Buffers.newDirectIntBuffer(INDICES.length);
        this.vertexBuffer.put(VERTICES);
        this.indexBuffer.put(INDICES);
        this.textures = new HashMap<>();
        this.audioInfo = null;
        this.audioChannel = fragmentShader.getAudioInputChannel();

        // gl-compatible buffer for reading offscreen surface to cpu memory
        this.backBuffer = GLBuffers.newDirectByteBuffer(xResolution * yResolution * 4);
        this.snapshot = new int[xResolution][yResolution];

        // set shader options
        this.shaderOptions = options;
        this.setAlphaMask(shaderOptions.getAlpha());

        this.audioTextureWidth = 512;
        this.audioTextureHeight = 2;
        this.audioTextureData = GLBuffers.newDirectFloatBuffer(audioTextureHeight * audioTextureWidth);
    }

    /**
     * Determines whether alpha values returned from the fragment shader will be used.  Can safely
     * be changed while the shader is running.
     *
     * @param b - true to enable the alpha channel for this shader, false to ignore it, discard
     *          whatever the shader does, and set alpha to full opacity
     */
    public void setAlphaMask(boolean b) {
        this.alphaMask = (b) ? 0 : 0xff;
    }

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        glAutoDrawable.getContext().makeCurrent();
        GL4 gl4 = glAutoDrawable.getGL().getGL4();

        if (!isInitialized()) {
          initShaderProgram(gl4);
          downloadTextureFiles(fragmentShader);
          gl4.glUseProgram(shaderProgram.getProgramId());
        }

        startTime = System.currentTimeMillis();
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        // switch to this shader's gl context and render
        glAutoDrawable.getContext().makeCurrent();
        GL4 gl4 = glAutoDrawable.getGL().getGL4();
        setUniforms(gl4);
        setUpCanvas(gl4);
        saveSnapshot(gl4, xResolution, yResolution);
    }

    private void saveSnapshot(GL4 gl4, int width, int height) {
        backBuffer.rewind();
        gl4.glReadBuffer(GL_BACK);
        gl4.glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, backBuffer);

        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                snapshot[w][h] = LXColor.rgba((backBuffer.get() & 0xff), (backBuffer.get() & 0xff),
                        backBuffer.get() & 0xff, alphaMask | (backBuffer.get() & 0xff));
            }
        }
    }

    private void setUpCanvas(GL4 gl4) {
        // allocate geometry buffer handles
        int[] bufferHandlesB = new int[1];
        gl4.glGenBuffers(1, bufferHandlesB, 0);

        // vertices
        bindBufferData(gl4, vertexBuffer, GL_ARRAY_BUFFER, Float.BYTES);
        gl4.glVertexAttribPointer(shaderProgram.getShaderAttributeLocation(ShaderAttribute.POSITION),
                3, GL4.GL_FLOAT, false, 0, 0);
        gl4.glEnableVertexAttribArray(shaderProgram.getShaderAttributeLocation(ShaderAttribute.POSITION));

        // geometry built from vertices (triangles!)
        bindBufferData(gl4, indexBuffer, GL_ELEMENT_ARRAY_BUFFER, Integer.BYTES);
        gl4.glDrawElements(GL2.GL_TRIANGLES, INDICES.length, GL2.GL_UNSIGNED_INT, 0);
    }

    private void setUniforms(GL4 gl4) {
        float timeSeconds = ((float) (System.currentTimeMillis() - startTime)) / 1000;

        // set standard shadertoy-style uniforms
        setUniform(Uniforms.TIME_SECONDS, timeSeconds);
        setUniform(Uniforms.RESOLUTION, (float) xResolution, (float) yResolution);
        setUniform(Uniforms.MOUSE, 0f, 0f, 0f, 0f);

        // add basic audio information uniforms
        for (Map.Entry<Uniforms.Audio, Float> audioEntry : audioInfo.getUniformMap().entrySet()) {
            setUniform(audioEntry.getKey().getUniformName(), audioEntry.getValue());
        }

        // if enabled, add all LX parameters as uniforms
        if (shaderOptions.getLXParameterUniforms()) {
            for (LXParameter customParameter : fragmentShader.getParameters()) {
                setUniform(customParameter.getLabel() + Uniforms.CUSTOM_SUFFIX, customParameter.getValuef());
            }
        }

        // add texture channels
        // TODO - need to expand the "setUniform()" mechanism to support textures too.

        for (Map.Entry<Integer, Texture> textureInput : textures.entrySet()) {
            Texture texture = textureInput.getValue();
            gl4.glActiveTexture(INDEX_TO_GL_ENUM.get(textureInput.getKey()));
            texture.enable(gl4);
            texture.bind(gl4);
            int channelLocation = gl4.glGetUniformLocation(shaderProgram.getProgramId(), Uniforms.CHANNEL +
                    textureInput.getKey());
            gl4.glUniform1i(channelLocation, textureInput.getKey());
            texture.disable(gl4);
        }

        // if enabled, set audio waveform and fft data as a 512x2 texture
        if (audioChannel != null && shaderOptions.getWaveData()) {

            gl4.glActiveTexture(INDEX_TO_GL_ENUM.get(0));
            gl4.glEnable(GL_TEXTURE_2D);
            int[] texHandle = new int[1];
            gl4.glGenTextures(1, texHandle, 0);
            gl4.glBindTexture(GL4.GL_TEXTURE_2D, texHandle[0]);

            // load frequency and waveform data into our texture, fft data in the first row,
            // normalized audio waveform data in the second.
            for (int n = 0; n < audioTextureWidth; n++) {
                audioTextureData.put(n, audioInfo.getFrequencyData(n));
                audioTextureData.put(n + audioTextureWidth, audioInfo.getWaveformData(n));
            }

            gl4.glTexImage2D(GL4.GL_TEXTURE_2D, 0, GL4.GL_R32F, audioTextureWidth, audioTextureHeight, 0, GL4.GL_RED, GL_FLOAT, audioTextureData);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            gl4.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            setUniform(Uniforms.CHANNEL + this.audioChannel, (int) 0);
        }

        // hand the complete uniform list to OpenGL
        updateUniforms(gl4);
    }

    private void initShaderProgram(GL4 gl4) {
        File vertexShader = new File("resources/shaders/framework/default.vs");
        shaderProgram = new ShaderProgram();
        String shaderCode = FRAGMENT_SHADER_TEMPLATE.replace(SHADER_BODY_PLACEHOLDER, fragmentShader.getShaderBody());
        shaderProgram.init(gl4, vertexShader, shaderCode,
                           fragmentShader.getShaderName(),fragmentShader.getShaderTimestamp());
        setUpCanvas(gl4);
    }

    private void downloadTextureFiles(FragmentShader fragmentShader) {
        for (Map.Entry<Integer, String> textureInput : fragmentShader.getChannelToTexture().entrySet()) {
            try {
                if (fragmentShader.hasRemoteTextures()) {
                    URL url = new URL(textureInput.getValue());
                    textures.put(textureInput.getKey(), TextureIO.newTexture(url, false, null));
                } else {
                    File file = new File(textureInput.getValue());
                    textures.put(textureInput.getKey(), TextureIO.newTexture(file, false));
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void bindBufferData(GL4 gl4, Buffer buffer, int destinationBufferConstant, int bufferElementBytes) {
        int[] bufferHandles = new int[1];
        gl4.glGenBuffers(1, bufferHandles, 0);

        buffer.rewind();
        gl4.glBindBuffer(destinationBufferConstant, bufferHandles[0]);
        gl4.glBufferData(destinationBufferConstant, (long) buffer.capacity() * bufferElementBytes,
                buffer, gl4.GL_STATIC_DRAW);
    }

    @Override
    public void dispose(GLAutoDrawable glAutoDrawable) {
        GL4 gl4 = glAutoDrawable.getGL().getGL4();
        shaderProgram.dispose(gl4);
    }

    @Override
    public void reshape(GLAutoDrawable glAutoDrawable, int x, int y, int width, int height) {
        //do nothing
    }

    public int[][] getSnapshot() {
        return snapshot;
    }

    public void updateAudioInfo(AudioInfo audioInfo) {
        this.audioInfo = audioInfo;
    }

    public void reset() {
        startTime = System.currentTimeMillis();
    }

    public boolean isInitialized() {
        return shaderProgram != null && shaderProgram.isInitialized();
    }

    /*
     Generic shader uniform handler - based on processing.opengl.PShader (www.processing.org)
     The idea is that this mechanism can handle our normal shadertoy-like and LX engine generated
     uniforms, plus any other uniforms the user wants to use.  This makes it easier to do things like
     sending the current foreground color to the shader as a vec3.

     When doing this, THE PATTERN/SHADER AUTHOR IS RESPONSIBLE for seeing that the uniform names match
     in the Java pattern code and the shader.  Otherwise... nothing ... will happen.

     Sigh... most of this code consists of ten zillion overloaded methods to set uniforms of all the supported
     types. Oh yeah, and a couple of worker methods to manage the uniform list we're building.
     */

    // worker for adding user specified uniforms to our big list'o'uniforms
    protected void addUniform(String name, int type, Object value) {
        if (uniforms == null) {
            uniforms = new HashMap<>();
        }
        uniforms.put(name, new GeneralUniforms(type, value));
    }

    // parse uniform list and create necessary GL objects
    // TODO  add support for generalized array types and textures.  Once that's done
    // TODO  we can use this subsystem for *all* uniforms and eliminate some complexity.
    protected void updateUniforms(GL4 gl4) {
        int[] v;
        float[] vf;
        if (uniforms != null && 0 < uniforms.size()) {
            for (String name : uniforms.keySet()) {
                int loc = gl4.glGetUniformLocation(shaderProgram.getProgramId(), name);
                ;
                if (loc == -1) {
                    // LX.log("No uniform \"" + name + "\"  found in shader");
                    continue;
                }
                GeneralUniforms val = uniforms.get(name);

                switch (val.type) {
                    case INT1:
                        v = ((int[]) val.value);
                        gl4.glUniform1i(loc, v[0]);
                        break;
                    case INT2:
                        v = ((int[]) val.value);
                        gl4.glUniform2i(loc, v[0], v[1]);
                        break;
                    case INT3:
                        v = ((int[]) val.value);
                        gl4.glUniform3i(loc, v[0], v[1], v[2]);
                        break;
                    case INT4:
                        v = ((int[]) val.value);
                        gl4.glUniform4i(loc, v[0], v[1], v[2], v[3]);
                        break;
                    case FLOAT1:
                        vf = ((float[]) val.value);
                        gl4.glUniform1f(loc, vf[0]);
                        break;
                    case FLOAT2:
                        vf = ((float[]) val.value);
                        gl4.glUniform2f(loc, vf[0], vf[1]);
                        break;
                    case FLOAT3:
                        vf = ((float[]) val.value);
                        gl4.glUniform3f(loc, vf[0], vf[1], vf[2]);
                        break;
                    case FLOAT4:
                        vf = ((float[]) val.value);
                        gl4.glUniform4f(loc, vf[0], vf[1], vf[2], vf[3]);
                        break;
                    default:
                        LX.log("Unsupported uniform type");
                        break;
                }
            }
            uniforms.clear();
        }
    }

    /**
     * setter -- single int
     */
    public void setUniform(String name, int x) {
        addUniform(name, INT1, new int[]{x});
    }

    /**
     * 2 element int array or ivec2
     */
    public void setUniform(String name, int x, int y) {
        addUniform(name, INT2, new int[]{x, y});
    }

    /**
     * 3 element int array or ivec3
     */
    public void setUniform(String name, int x, int y, int z) {
        addUniform(name, INT3, new int[]{x, y, z});
    }

    /**
     * 4 element int array or ivec4
     */
    public void setUniform(String name, int x, int y, int z, int w) {
        addUniform(name, GeneralUniforms.INT4, new int[]{x, y, z, w});
    }

    /**
     * single float
     */
    public void setUniform(String name, float x) {
        addUniform(name, GeneralUniforms.FLOAT1, new float[]{x});
    }

    /**
     * 2 element float array or vec2
     */
    public void setUniform(String name, float x, float y) {
        addUniform(name, GeneralUniforms.FLOAT2, new float[]{x, y});
    }

    /**
     * 3 element float array or vec3
     */
    public void setUniform(String name, float x, float y, float z) {
        addUniform(name, GeneralUniforms.FLOAT3, new float[]{x, y, z});
    }

    /**
     * 4 element float array or vec4
     */
    public void setUniform(String name, float x, float y, float z, float w) {
        addUniform(name, GeneralUniforms.FLOAT4, new float[]{x, y, z, w});
    }

    public void setUniform(String name, boolean x) {
        addUniform(name, INT1, new int[]{(x) ? 1 : 0});
    }

    public void setUniform(String name, boolean x, boolean y) {
        addUniform(name, INT2,
                new int[]{(x) ? 1 : 0, (y) ? 1 : 0});
    }
}
