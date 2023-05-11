package titanicsend.pattern.yoffa.effect;

import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.TEPattern;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NativeShaderPatternEffect extends PatternEffect {

    protected OffscreenShaderRenderer offscreenShaderRenderer;
    private FragmentShader fragmentShader;
    private final List<LXParameter> parameters;

    private ShaderPainter painter;

    TEPattern.ColorType colorType;
     PatternControlData controlData;
    ShaderOptions shaderOptions;

    /**
     * Creates new native shader effect with the specified shader options.
     * @param fragmentShader
     * @param target
     * @param options - ShaderOptions object
     */
    public NativeShaderPatternEffect(FragmentShader fragmentShader, PatternTarget target, ShaderOptions options) {
        super(target);
        this.colorType = target.colorType;
        this.controlData = new PatternControlData(pattern);
        this.shaderOptions = options;
        painter = new ShaderPainter();

        if (fragmentShader != null) {
            this.fragmentShader = fragmentShader;
            this.offscreenShaderRenderer = new OffscreenShaderRenderer(fragmentShader,options);
            this.parameters = fragmentShader.getParameters();

        } else {
            this.parameters = null;
        }
    }

    /**
     * Creates new native shader effect with default options - shader alpha will be ignored,
     * audio data and LX parameters will be provided as uniforms to the shader.
     * @param fragmentShader
     * @param target
     */
    public NativeShaderPatternEffect(FragmentShader fragmentShader, PatternTarget target) {
        // alpha disabled in this version to preserve backward compatibility
        this(fragmentShader,target,new ShaderOptions());
    }

    /**
     * Creates new native shader effect with additional texture support, using
     * the specified shader options.
     * @param shaderFilename
     * @param target
     * @param options - ShaderOptions object
     * @param textureFilenames
     */
    public NativeShaderPatternEffect(String shaderFilename, PatternTarget target, ShaderOptions options, String... textureFilenames) {
        this(new FragmentShader(new File("resources/shaders/" + shaderFilename),
                        Arrays.stream(textureFilenames)
                                .map(x -> new File("resources/shaders/textures/" + x))
                                .collect(Collectors.toList())),
                target,options);

    }

    /**
     * Creates new native shader effect with additional texture support, using
     * the default options - shader alpha will be ignored, audio data and LX parameters
     * will be provided as uniforms to the shader.
     * @param shaderFilename
     * @param target
     * @param textureFilenames
     */
    public NativeShaderPatternEffect(String shaderFilename, PatternTarget target, String... textureFilenames) {
        this(shaderFilename,target,new ShaderOptions(), textureFilenames);
    }

    @Override
    public void onPatternActive() {
        if (fragmentShader != null) {
            if (offscreenShaderRenderer == null) {
                offscreenShaderRenderer = new OffscreenShaderRenderer(fragmentShader, shaderOptions);

            }
        }
    }

    @Override
    public void run(double deltaMs) {
        if (offscreenShaderRenderer == null) {
            return;
        }

        int[][] snapshot = offscreenShaderRenderer.getFrame(controlData);

        /*
         TODO - We should really use setColor for this instead of exposing colors as this will break blending
         TODO - pass the images to be blended into a shader as textures and do blending in hardware!
        */
        painter.setImage(snapshot);
        painter.setColors(pattern.getColors());
        for (LXPoint point : getPoints()) {
            painter.paint(point);
        }
    }

    // Saves me from having to propagate all those setUniform(name,etc.) methods up the
    // object hierarchy!  It grants you great power.  Use responsibly!
    public NativeShader getNativeShader() { return offscreenShaderRenderer.getNativeShader(); }

    @Override
    public List<LXParameter> getParameters() {
        return parameters;
    }
}
