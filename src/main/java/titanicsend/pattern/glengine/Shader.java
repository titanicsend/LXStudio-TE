package titanicsend.pattern.glengine;

import com.jogamp.opengl.GLAutoDrawable;
import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.shader_engine.FragmentShader;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.pattern.yoffa.shader_engine.PatternControlData;
import titanicsend.util.TE;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import java.util.stream.Collectors;

public class Shader {
    private GLEngine glEngine = null;
    private FragmentShader fragmentShader;
    private NativeShader nativeShader;
    private List<LXParameter> parameters;
    private GLAutoDrawable canvas = null;
    private TEPerformancePattern pattern;

    private PatternControlData controlData;

    /**
     * Creates new native shader effect
     *
     */
    public Shader(LX lx, FragmentShader fragmentShader, TEPerformancePattern pattern) {
        this.pattern = pattern;
        this.controlData = new PatternControlData(pattern);

        if (glEngine == null) {
            this.glEngine = (GLEngine) lx.engine.getChild(GLEngine.PATH);
            TE.log("Shader: Retrieved GLEngine object from LX");
        }

        if (fragmentShader != null) {
            this.fragmentShader = fragmentShader;
            this.canvas = glEngine.getCanvas();
            this.parameters = fragmentShader.getParameters();

            nativeShader = new NativeShader(fragmentShader, glEngine.getWidth(), glEngine.getHeight());
        } else {
            this.parameters = null;
        }
    }

    /**
     * Creates new native shader effect with additional texture support
     * @param lx LX instance
     * @param shaderFilename shader to use
     * @param pattern Pattern associated w/this shader
     */
    public Shader(LX lx, String shaderFilename, TEPerformancePattern pattern , String... textureFilenames) {
        this(lx,new FragmentShader(new File("resources/shaders/" + shaderFilename),
                Arrays.stream(textureFilenames)
                    .map(x -> new File("resources/shaders/textures/" + x))
                    .collect(Collectors.toList())),
            pattern);
    }

    public void onPatternActive() {
       // TODO - init if necessary
    }

    public void onPatternInactive() {
        // TODO - GL teardown if necessary
    }

    public ByteBuffer getFrame(PatternControlData ctlInfo) {
        nativeShader.updateControlInfo(ctlInfo);
        nativeShader.display(canvas);

        return nativeShader.getSnapshot();
    }

    /**
     * Called after a frame has been generated, this function samples the OpenGL backbuffer to set color at the
     * specified points.
     *
     * @param points list of points to paint
     * @param image backbuffer containing image for this frame
     * @param xSize x resolution of image
     * @param ySize y resolution of image
     */
    public void paint(List<LXPoint> points, ByteBuffer image, int xSize, int ySize) {
        double k = 0;
        int xMax = xSize - 1;
        int yMax = ySize - 1;
        int[] colors = pattern.getColors();

        for (LXPoint point : points) {
            float zn = (1f - point.zn);
            float yn = point.yn;

            // use normalized point coordinates to calculate x/y coordinates and then the
            // proper index in the image buffer.  the 'z' dimension of TE corresponds
            // with 'x' dimension of the image based on the side that we're painting.
            int xi = Math.round(zn * xMax);
            int yi = Math.round(yn * yMax);

            int index = 4 * ((yi * xSize) + xi);

            colors[point.index] = image.getInt(index);
        }
    }

    public void run(double deltaMs) {
        if (canvas == null) {
            return;
        }
        ByteBuffer image = getFrame(controlData);
        paint(this.pattern.getModel().getPoints(), image, glEngine.getWidth(), glEngine.getHeight());
    }

    public List<ShaderConfiguration> getShaderConfig() {
        return fragmentShader.getShaderConfig();
    }
    public List<LXParameter> getParameters() {
        return parameters;
    }
}
