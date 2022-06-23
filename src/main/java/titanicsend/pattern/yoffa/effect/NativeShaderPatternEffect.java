package titanicsend.pattern.yoffa.effect;

import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.framework.PatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.media.ImagePainter;
import titanicsend.pattern.yoffa.client.ShaderToyClient;
import titanicsend.pattern.yoffa.shader_engine.FragmentShader;
import titanicsend.pattern.yoffa.shader_engine.OffscreenShaderRenderer;
import titanicsend.util.Dimensions;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public class NativeShaderPatternEffect extends PatternEffect {

    private final BooleanParameter clipboardSwitch = new BooleanParameter("Paste Id", false)
            .setMode(BooleanParameter.Mode.MOMENTARY);

    private OffscreenShaderRenderer offscreenShaderRenderer;

    public NativeShaderPatternEffect(PatternTarget target) {
        super(target);
    }

    @Override
    public void onPatternActive() {
        if (offscreenShaderRenderer != null) {
            offscreenShaderRenderer.reset();
        }
    }

    @Override
    public void run(double deltaMs) {
        if (offscreenShaderRenderer == null) {
            return;
        }

        int[][] snapshot = offscreenShaderRenderer.getFrame();
        //TODO we should really use setColor for this instead of exposing colors as this will break blending
        //ImagePainter is the last thing that hasn't been migrated to new framework
        ImagePainter imagePainter = new ImagePainter(snapshot, pattern.getColors());
        for (Map.Entry<LXPoint, Dimensions> entry : pointsToCanvas.entrySet()) {
            imagePainter.paint(entry.getKey(), entry.getValue(), 1);
        }
    }

    @Override
    public List<LXParameter> getParameters() {
        return List.of(clipboardSwitch);
    }

    @Override
    public void onParameterChanged(LXParameter parameter) {
        if (parameter == this.clipboardSwitch && this.clipboardSwitch.getValueb()) {
            try {
                Transferable clipboardValue = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
                if (clipboardValue.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    String shaderId = (String) clipboardValue.getTransferData(DataFlavor.stringFlavor);
                    FragmentShader fragmentShader = ShaderToyClient.getShader(shaderId);
                    offscreenShaderRenderer = new OffscreenShaderRenderer(fragmentShader);
                }
            } catch (Exception e) {
                //usually for this project I like to let exceptions escape and be surfaced to the user, but when they
                //  escape in onParameterChanged methods it just hangs the parameter :/
                System.out.println("Problem loading shader: " + e.getMessage());
            }
        }
    }
}
