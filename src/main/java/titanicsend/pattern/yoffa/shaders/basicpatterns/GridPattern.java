package titanicsend.pattern.yoffa.shaders.basicpatterns;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.yoffa.shaders.FragmentShader;
import titanicsend.pattern.yoffa.shaders.GridShader;

@LXCategory("Noncommercial Shaders")
public class GridPattern extends BasicShaderPattern {

    public GridPattern(LX lx) {
        super(lx);
    }
    @Override
    protected FragmentShader getFragmentShader() {
        return new GridShader();
    }
}
