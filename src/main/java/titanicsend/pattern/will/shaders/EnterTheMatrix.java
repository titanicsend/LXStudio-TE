package titanicsend.pattern.will.shaders;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.pattern.jon.TEControlTag;

@LXCategory("DREVO Shader Patterns")
public class EnterTheMatrix extends GLShaderPattern {

    public EnterTheMatrix(LX lx) {
        super(lx, TEShaderView.ALL_POINTS);

        controls.setRange(TEControlTag.WOW1, 0.14, 0, 0.5); // complexity
        controls.setRange(TEControlTag.WOW2, 0.5, 0.2, 3); // contrast
        controls.setRange(TEControlTag.SIZE, 2., 3, 0.5); // scale
        controls.setRange(TEControlTag.QUANTITY, 7, 1, 13); // number of kaleidoscope slices
        controls.setValue(TEControlTag.SPIN, 0.125);
        controls.setRange(TEControlTag.LEVELREACTIVITY,0.1,0,1);
        controls.setRange(TEControlTag.FREQREACTIVITY,0.1,0,1);

        // register common controls with LX
        addCommonControls();

        addShader(new GLShader(lx, "enter_the_matrix.fs", getControlData()));
    }
}
