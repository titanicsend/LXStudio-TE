package titanicsend.pattern.yoffa.effect;

import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.media.ImagePainter;
import titanicsend.pattern.yoffa.client.ShaderToyClient;
import titanicsend.pattern.yoffa.shader_engine.FragmentShader;
import titanicsend.pattern.yoffa.shader_engine.OffscreenShaderRenderer;
import titanicsend.util.Dimensions;

import java.util.Collection;
import java.util.Map;

public class NativeShaderPatternEffect extends PatternEffect {

    private final OffscreenShaderRenderer offscreenShaderRenderer;

    public NativeShaderPatternEffect(FragmentShader fragmentShader, PatternTarget target) {
        super(target);
        offscreenShaderRenderer = new OffscreenShaderRenderer(fragmentShader);
    }

    public static NativeShaderPatternEffect fromShaderToyId(String shaderToyId, PatternTarget target) {
        return new NativeShaderPatternEffect(ShaderToyClient.getShader(shaderToyId), target);
    }

    @Override
    public void onPatternActive() {
        offscreenShaderRenderer.reset();
    }

    @Override
    public void run(double deltaMs) {
        int[][] snapshot = offscreenShaderRenderer.getFrame();
        //TODO we should really use setColor for this instead of exposing colors as this will break blending
        //ImagePainter is the last thing that hasn't been migrated to new framework
        ImagePainter imagePainter = new ImagePainter(snapshot, pattern.getColors());
        for (Map.Entry<LXPoint, Dimensions> entry : pointsToCanvas.entrySet()) {
            imagePainter.paint(entry.getKey(), entry.getValue(), 1);
        }
    }

    @Override
    public Collection<LXParameter> getParameters() {
        return null;
    }
}
