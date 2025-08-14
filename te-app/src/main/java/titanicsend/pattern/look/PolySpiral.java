package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.CompoundParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Look Shader Patterns")
public class PolySpiral extends GLShaderPattern {

  public final CompoundParameter interval = new CompoundParameter("Interval", .5, 0.0, 1.0);

  public PolySpiral(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.setRange(TEControlTag.SIZE, 1.0, 0.3, 2.0);
    controls.setRange(TEControlTag.XPOS, 0.0, -0.5, 0.5);
    controls.setRange(TEControlTag.YPOS, -0.07, -0.5, 0.5);
    controls.setValue(TEControlTag.SPIN, 0.1);
    controls.setRange(TEControlTag.QUANTITY, 7.0, 3.0, 12.0);
    controls.setRange(TEControlTag.WOW1, 1.0, 0.0, 3.0);
    controls.setRange(TEControlTag.WOW2, 5.0, 1.0, 12.0);
    controls.markUnused(controls.getLXControl(TEControlTag.LEVELREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.FREQREACTIVITY));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    addCommonControls();
    addParameter("interval", this.interval);

    GLShader.config(lx)
        .withFilename("polyspiral.fs")
        .withUniformSource(
            (s) -> {
              s.setUniform("interval", interval.getValuef());
            });
  }
}
