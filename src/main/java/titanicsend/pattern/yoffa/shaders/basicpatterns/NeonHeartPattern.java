package titanicsend.pattern.yoffa.shaders.basicpatterns;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.pattern.yoffa.shaders.FragmentShader;
import titanicsend.pattern.yoffa.shaders.NeonHeartShader;

@LXCategory("Noncommercial Shaders")
public class NeonHeartPattern extends BasicShaderPattern {

    public NeonHeartPattern(LX lx) {
        super(lx);
    }

    @Override
    protected FragmentShader getFragmentShader() {
        return new NeonHeartShader();
    }
}
