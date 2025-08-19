package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Look Shader Patterns")
public class TriangleInfinityWaveform extends GLShaderPattern {

  public TriangleInfinityWaveform(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    //        controls.setValue(TEControlTag.YPOS, -0.17);
    controls.setRange(TEControlTag.SIZE, 1.35, 0.2, 2.0);
    controls.setRange(TEControlTag.SPEED, 0.01, 0.00, 0.5);
    controls.setRange(TEControlTag.QUANTITY, 6.0, 2.0, 12.0);
    controls.setRange(TEControlTag.LEVELREACTIVITY, 0.09, 0.0, 0.5);
    controls.setRange(TEControlTag.FREQREACTIVITY, 0.04, 0.0, 0.5);
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    addCommonControls();

    addShader(
        GLShader.config(lx)
            .withFilename("triangle_infinity.fs")
            .withUniformSource(
                (s) -> {
                  s.setUniform("brightnessDampening", 0.5f);
                }));
  }
}
