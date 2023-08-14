package titanicsend.pattern.yoffa.effect;

import heronarts.lx.Tempo;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.SplittableRandom;
import java.util.stream.Collectors;

public class NativeShaderPatternEffect extends PatternEffect {

    private final SplittableRandom random;
    protected OffscreenShaderRenderer renderer;
    private FragmentShader fragmentShader;
    private final List<LXParameter> parameters;

    PatternControlData controlData;

    /**
     * Creates new native shader effect
     *
     * @param fragmentShader
     * @param target
     */
    public NativeShaderPatternEffect(FragmentShader fragmentShader, PatternTarget target) {
        super(target);
        random = new SplittableRandom();

        this.controlData = new PatternControlData(pattern);

        if (fragmentShader != null) {
            this.fragmentShader = fragmentShader;
            this.renderer = new OffscreenShaderRenderer(fragmentShader);
            this.parameters = fragmentShader.getParameters();

        } else {
            this.parameters = null;
        }
    }

    /**
     * Creates new native shader effect with additional texture support
     *
     * @param shaderFilename
     * @param target
     * @param textureFilenames
     */
    public NativeShaderPatternEffect(String shaderFilename, PatternTarget target, String... textureFilenames) {
        this(new FragmentShader(new File("resources/shaders/" + shaderFilename),
                Arrays.stream(textureFilenames)
                    .map(x -> new File("resources/shaders/textures/" + x))
                    .collect(Collectors.toList())),
            target);

    }

    @Override
    public void onPatternActive() {
        if (fragmentShader != null) {
            if (renderer == null) {
                renderer = new OffscreenShaderRenderer(fragmentShader);
            }
        }
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
        boolean exploding = pattern.getExplode() > 0;

        // calculate per-frame "explosion" parameters
        if (exploding) {
            double measureProgress = 1.0 - this.pattern.getLX().engine.tempo.getBasis(Tempo.Division.WHOLE); // 1 when we start measure, 0 when we finish
            measureProgress *= measureProgress * measureProgress;
            k = 0.00035 + pattern.getExplode() * measureProgress;
        }

        for (LXPoint point : points) {
            float zn = (1f - point.zn);
            float yn = point.yn;

            if (exploding) {
                // displace each pixel randomly in chessboard directions.
                // this is slightly less random than calling nextDouble()) per coordinate
                // but it saves us an expensive per-pixel calculation and given the
                // number of pixels we're sampling, looks pretty much the same.
                float displacement = (float) (k * (-0.5 + random.nextDouble()));
                zn += displacement;
                zn = Math.max(0, Math.min(1, zn));
                yn += displacement;
                yn = Math.max(0, Math.min(1, yn));
            }

            // use normalized point coordinates to calculate x/y coordinates and then the
            // proper index in the image buffer.  the 'z' dimension of TE corresponds
            // with 'x' dimension of the image based on the side that we're painting.
            int xi = Math.round(zn * xMax);
            int yi = Math.round(yn * yMax);

            int index = 4 * ((yi * xSize) + xi);

            colors[point.index] = image.getInt(index);
        }
    }

    @Override
    public void run(double deltaMs) {
        if (renderer == null) {
            return;
        }

        ByteBuffer image = renderer.getFrame(controlData);
        paint(getPoints(), image, OffscreenShaderRenderer.getXResolution(), OffscreenShaderRenderer.getYResolution());
    }

    // Saves me from having to propagate all those setUniform(name,etc.) methods up the
    // object hierarchy!  It grants great power.  Use responsibly!
    public NativeShader getNativeShader() {
        return renderer.getNativeShader();
    }

    @Override
    public List<LXParameter> getParameters() {
        return parameters;
    }
}
