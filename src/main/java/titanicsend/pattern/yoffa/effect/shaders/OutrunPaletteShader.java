package titanicsend.pattern.yoffa.effect.shaders;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.Tempo;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LinkedColorParameter;
import heronarts.lx.modulator.Click;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.ObjectParameter;
import titanicsend.pattern.TEAudioPattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.PatternTarget;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;
import titanicsend.pattern.yoffa.shader_engine.ShaderOptions;


@LXCategory("Native Shaders Panels")
public class OutrunPaletteShader extends TEAudioPattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;

    public final LinkedColorParameter color =
            registerColor("Color", "color", ColorType.PANEL,
                    "Panel Color");

    public OutrunPaletteShader(LX lx) {
        super(lx);
        // create new effect with alpha on and no automatic
        // parameter uniforms
        ShaderOptions options = new ShaderOptions();
        options.useAlpha(true);
        options.useLXParameterUniforms(false);

        effect = new NativeShaderPatternEffect("outrun_grid_palette.fs",
                PatternTarget.allPanelsAsCanvas(this), options);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {
        // Example of sending a vec3 to a shader.
        // Get the current color and convert to
        // normalized rgb in range 0..1 for openGL
        int baseColor = this.color.calcColor();

        float rn = (float) (0xff & LXColor.red(baseColor)) / 255f;
        float gn = (float) (0xff & LXColor.green(baseColor)) / 255f;
        float bn = (float) (0xff & LXColor.blue(baseColor)) / 255f;
        shader.setUniform("color", rn, gn, bn);

        // run the shader
        effect.run(deltaMs);
    }

    @Override
    // THIS IS REQUIRED if you're not using ConstructedPattern!
    // Initialize the NativeShaderPatternEffect and retrieve the native shader object
    // from it when the pattern becomes active
    public void onActive() {
        effect.onActive();
        shader = effect.getNativeShader();
    }
}

