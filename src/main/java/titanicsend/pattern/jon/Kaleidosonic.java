package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Native Shaders Panels")
public class Kaleidosonic extends GLShaderPattern {

  public Kaleidosonic(LX lx) {
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

    addShader(new GLShader(lx, "kaleidosonic.fs", getControlData(), "color_noise.png"));
  }
}
