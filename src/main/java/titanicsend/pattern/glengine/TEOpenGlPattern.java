package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.yoffa.effect.NativeShaderPatternEffect;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.yoffa.shader_engine.NativeShader;

public class TEOpenGlPattern extends TEPerformancePattern {
    NativeShaderPatternEffect effect;
    NativeShader shader;


    protected TEOpenGlPattern(LX lx) {
        this(lx, null);
    }

    protected TEOpenGlPattern(LX lx, TEShaderView defaultView) {
        super(lx, defaultView);
    }

    @Override
    public void runTEAudioPattern(double deltaMs) {

        // run the shader
        effect.run(deltaMs);
    }

    @Override
    public void dispose() {
        System.out.println("TEOpenGlPattern.dispose()");
//        effect.close();
//        super.dispose();
    }


}
