package titanicsend.pattern.yoffa.effect;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.yoffa.client.ShaderToyClient;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.FragmentShader;
import titanicsend.pattern.yoffa.shader_engine.OffscreenShaderRenderer;
import titanicsend.util.TE;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.List;

//starts out blank, but pressing the button uses the clipboard as a ShaderToy id and dynamically loads the shader
//note that we can't dynamically update the parameter afaict, so adding parameter hints to a fork will not work
public class ShaderToyPatternEffect extends NativeShaderPatternEffect {

    private final BooleanParameter clipboardSwitch = new BooleanParameter("Paste Id", false)
            .setMode(BooleanParameter.Mode.MOMENTARY);

    public ShaderToyPatternEffect(PatternTarget target) {
        super((FragmentShader) null, target);
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
                TE.log("Problem loading shader: " + e.getMessage());
            }
        }
    }

    @Override
    public java.util.List<LXParameter> getParameters() {
        return List.of(clipboardSwitch);
    }
}
