package titanicsend.pattern.yoffa.effect;

import heronarts.lx.LX;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.media.ImagePainter;
import titanicsend.pattern.yoffa.shader_engine.AudioInfo;
import titanicsend.pattern.yoffa.shader_engine.FragmentShader;
import titanicsend.pattern.yoffa.shader_engine.OffscreenShaderRenderer;
import titanicsend.util.Dimensions;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NativeShaderPatternEffect extends PatternEffect {

    protected OffscreenShaderRenderer offscreenShaderRenderer;
    private FragmentShader fragmentShader;
    private final List<LXParameter> parameters;

    AudioInfo audioInfo;
    boolean useShaderAlpha;

    /**
     * Creates new native shader effect with optional support for alpha blending
     * Use this version of the constructor if the shader has a useable alpha channel.
     * @param fragmentShader
     * @param target
     * @param useAlpha
     */
    public NativeShaderPatternEffect(FragmentShader fragmentShader, PatternTarget target,boolean useAlpha) {
        super(target);
        if (fragmentShader != null) {
            this.fragmentShader = fragmentShader;
            this.offscreenShaderRenderer = new OffscreenShaderRenderer(fragmentShader);
            this.parameters = fragmentShader.getParameters();
            this.audioInfo = new AudioInfo(pattern.getLX().engine.audio.meter);
            useAlphaChannel(useAlpha);
        } else {
            this.parameters = null;
        }
    }

    /**
     * Creates new native shader effect that will ignore the shader's alpha channel.
     * @param fragmentShader
     * @param target
     */
    public NativeShaderPatternEffect(FragmentShader fragmentShader, PatternTarget target) {
        // alpha disabled in this version to preserve backward compatibility
        this(fragmentShader,target,false);
    }

    /**
     * Creates new native shader effect with additional texture support, and
     * optional support for alpha blending.  Use this version of the constructor
     * if the shader is known to have a useful alpha channel.
     * @param shaderFilename
     * @param target
     * @param useAlpha
     * @param textureFilenames
     */
    public NativeShaderPatternEffect(String shaderFilename, PatternTarget target, boolean useAlpha, String... textureFilenames) {
        this(new FragmentShader(new File("resources/shaders/" + shaderFilename),
                        Arrays.stream(textureFilenames)
                                .map(x -> new File("resources/shaders/textures/" + x))
                                .collect(Collectors.toList())),
                target,useAlpha);

    }

    /**
     * Creates new native shader effect with additional texture support, that will
     * ignore information in the shader's alpha channel.
     * @param shaderFilename
     * @param target
     * @param textureFilenames
     */
    public NativeShaderPatternEffect(String shaderFilename, PatternTarget target, String... textureFilenames) {
        // alpha disabled in this version to preserve backward compatibility
        this(shaderFilename,target,false,textureFilenames);
    }


    /**
     * Determines whether alpha values returned from the fragment shader will be used.  Can safely
     * be changed while the shader is running.
     * @param b - true to enable the alpha channel for this shader, false to ignore it, discard
     *          whatever the shader does, and set alpha to full opacity
     *
     */
    public void useAlphaChannel(boolean b) {
        useShaderAlpha = b;
    }

    @Override
    public void onPatternActive() {
        if (fragmentShader != null) {
            offscreenShaderRenderer = new OffscreenShaderRenderer(fragmentShader);
            offscreenShaderRenderer.useAlphaChannel(useShaderAlpha);
        }
    }

    @Override
    public void run(double deltaMs) {
        if (offscreenShaderRenderer == null) {
            return;
        }

        audioInfo.setFrameData(pattern.getTempo().basis(),
                pattern.sinePhaseOnBeat(), pattern.getBassLevel(), pattern.getTrebleLevel());

        int[][] snapshot = offscreenShaderRenderer.getFrame(audioInfo);

        //TODO we should really use setColor for this instead of exposing colors as this will break blending
        //ImagePainter is the last thing that hasn't been migrated to new framework
        ImagePainter imagePainter = new ImagePainter(snapshot, pattern.getColors());
        for (Map.Entry<LXPoint, Dimensions> entry : pointsToCanvas.entrySet()) {
            imagePainter.paint(entry.getKey(), entry.getValue(), 1);
        }
    }

    @Override
    public List<LXParameter> getParameters() {
        return parameters;
    }


}
