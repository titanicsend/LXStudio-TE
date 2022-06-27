package titanicsend.pattern.yoffa.effect;

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

    public NativeShaderPatternEffect(FragmentShader fragmentShader, PatternTarget target) {
        super(target);
        if (fragmentShader != null) {
            this.fragmentShader = fragmentShader;
            this.offscreenShaderRenderer = new OffscreenShaderRenderer(fragmentShader);
            this.parameters = fragmentShader.getParameters();
        } else {
            this.parameters = null;
        }
    }

    public NativeShaderPatternEffect(String shaderFilename, PatternTarget target, String... textureFilenames) {
        this(new FragmentShader(new File("resources/shaders/" + shaderFilename),
                        Arrays.stream(textureFilenames)
                                .map(x -> new File("resources/shaders/textures/" + x))
                                .collect(Collectors.toList())),
                target);

    }

    @Override
    public void onPatternActive() {
        offscreenShaderRenderer = new OffscreenShaderRenderer(fragmentShader);
    }

    @Override
    public void run(double deltaMs) {
        if (offscreenShaderRenderer == null) {
            return;
        }

        AudioInfo audioInfo = new AudioInfo(pattern.getTempo().basis(),
                pattern.sinePhaseOnBeat(), pattern.getBassLevel(), pattern.getTrebleLevel(),
                pattern.getLX().engine.audio.meter.bands);
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
