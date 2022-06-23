package titanicsend.pattern.yoffa.shader_engine;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.util.GLBuffers;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static com.jogamp.opengl.GL.*;

//Technically we don't need to implement GLEventListener unless we plan on rendering on screen, but let's leave it
//  for good practice
public class NativeShader implements GLEventListener {

    //we need to draw an object with a vertex shader to put our fragment shader on
    //literally just create a rectangle that takes up the whole screen to paint on
    private static final float [] VERTICES = {
            1.0f,  1.0f,  0.0f,
            1.0f,  -1.0f,  0.0f,
            -1.0f,  -1.0f,  0.0f,
            -1.0f,  1.0f,  0.0f
    };

    //we are drawing with triangles, so we need two to make our rectangle
    private static final int [] INDICES = 		{
            0, 1, 2,
            2, 0, 3
    };

    private static final Map<Integer, Integer> INDEX_TO_GL_ENUM = Map.of(
            0, GL_TEXTURE0,
            1, GL_TEXTURE1,
            2, GL_TEXTURE2,
            3, GL_TEXTURE3); //stupid

    private static final String FRAGMENT_SHADER_TEMPLATE = ShaderUtils.loadResource("resources/shaders/template.fs");
    private static final String SHADER_BODY_PLACEHOLDER = "{{%shader_body%}}";

    private final FragmentShader fragmentShader;
    private final int xResolution;
    private final int yResolution;

    private final FloatBuffer vertexBuffer;
    private final IntBuffer indexBuffer;
    private final Map<Integer, Texture> textures;
    private ShaderProgram shaderProgram;
    private long startTime;
    private int[][] snapshot;



    public NativeShader(FragmentShader fragmentShader, int xResolution, int yResolution) {
        this.xResolution = xResolution;
        this.yResolution = yResolution;
        this.fragmentShader = fragmentShader;
        this.vertexBuffer = Buffers.newDirectFloatBuffer(VERTICES.length);
        this.indexBuffer = Buffers.newDirectIntBuffer(INDICES.length);
        this.vertexBuffer.put(VERTICES);
        this.indexBuffer.put(INDICES);
        this.textures =  new HashMap<>();
    }

    @Override
    public void init(GLAutoDrawable glAutoDrawable) {
        initShaderProgram(glAutoDrawable);
        downloadTextureFiles(fragmentShader);
        startTime = System.currentTimeMillis();
    }

    @Override
    public void display(GLAutoDrawable glAutoDrawable) {
        GL4 gl4 = glAutoDrawable.getGL().getGL4();
        setUpCanvas(gl4);
        setUniforms(gl4);
        saveSnapshot(gl4, xResolution, yResolution);
        gl4.glUseProgram(0);
    }

    private void saveSnapshot(GL4 gl4, int width, int height) {
        //read colors directly from the buffer for perf
        ByteBuffer buffer = GLBuffers.newDirectByteBuffer(width * height * 4);
        gl4.glReadBuffer(GL_BACK);
        gl4.glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buffer);

        //usually we'd write to a BufferedImage, but we're doing this every frame, so perf would be bad
        //we're going to end up just using it for colors/co-ordinates, so just throw it in a 2d array
        this.snapshot = new int[width][height];
        for (int h = 0; h < height; h++) {
            for (int w = 0; w < width; w++) {
                // buffer is cycling 4 bytes for rgba
                snapshot[w][height-h-1] = new Color((buffer.get() & 0xff), (buffer.get() & 0xff),
                        (buffer.get() & 0xff)).getRGB();
                buffer.get();   // consume/ignore alpha
            }
        }
    }

    private void setUpCanvas(GL4 gl4) {
        gl4.glUseProgram(shaderProgram.getProgramId());

        int[] bufferHandlesB = new int[1];
        gl4.glGenBuffers(1, bufferHandlesB, 0);

        bindBufferData(gl4, vertexBuffer, GL_ARRAY_BUFFER, Float.BYTES);
        gl4.glVertexAttribPointer(shaderProgram.getShaderAttributeLocation(ShaderAttribute.POSITION),
                3, GL4.GL_FLOAT, false, 0, 0);
        gl4.glEnableVertexAttribArray(shaderProgram.getShaderAttributeLocation(ShaderAttribute.POSITION));

        bindBufferData(gl4, indexBuffer, GL_ELEMENT_ARRAY_BUFFER, Integer.BYTES);
        gl4.glDrawElements(GL2.GL_TRIANGLES, INDICES.length, GL2.GL_UNSIGNED_INT, 0);

        gl4.glDisableVertexAttribArray(shaderProgram.getShaderAttributeLocation(ShaderAttribute.POSITION));
    }

    private void setUniforms(GL4 gl4) {
        float timeSeconds = ((float) (System.currentTimeMillis() - startTime)) / 1000;
        int timeLocation = gl4.glGetUniformLocation(shaderProgram.getProgramId(), Uniforms.TIME_SECONDS);
        gl4.glUniform1f(timeLocation, timeSeconds);

        int resLocation = gl4.glGetUniformLocation(shaderProgram.getProgramId(), Uniforms.RESOLUTION);
        gl4.glUniform2f(resLocation, xResolution, yResolution);

        //dummy values for mouse makes sure shaders that change on mouse still compile
        int mouseLocation = gl4.glGetUniformLocation(shaderProgram.getProgramId(), Uniforms.MOUSE);
        gl4.glUniform4f(mouseLocation, 0, 0, 0, 0);

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
    }

    private void initShaderProgram(GLAutoDrawable glAutoDrawable) {
        glAutoDrawable.getContext().makeCurrent();
        GL4 gl4 = glAutoDrawable.getGL().getGL4();
        File vertexShader = new File("resources/shaders/default.vs");
        shaderProgram = new ShaderProgram();
        String shaderCode = FRAGMENT_SHADER_TEMPLATE.replace(SHADER_BODY_PLACEHOLDER, fragmentShader.shaderBody());
        shaderProgram.init(gl4, vertexShader, shaderCode);
    }

    private void downloadTextureFiles(FragmentShader fragmentShader) {
        for (Map.Entry<Integer, String> textureInput : fragmentShader.channelToTexture().entrySet()) {
            try {
                URL url = new URL(textureInput.getValue());
                textures.put(textureInput.getKey(), TextureIO.newTexture(url, false, null));
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

    public void reset() {
        startTime = System.currentTimeMillis();
    }

    public boolean isInitialized() {
        return shaderProgram != null && shaderProgram.isInitialized();
    }
}
