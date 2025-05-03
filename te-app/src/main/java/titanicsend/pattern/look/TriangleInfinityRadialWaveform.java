package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Look Shader Patterns")
public class TriangleInfinityRadialWaveform extends GLShaderPattern {

  public TriangleInfinityRadialWaveform(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);
    controls.setValue(TEControlTag.YPOS, -0.14);
    controls.setRange(TEControlTag.SIZE, 1.14, 0.2, 2.0); // also: 0.4
    controls.setRange(TEControlTag.SPEED, 0.03, 0.00, 0.5);
    //        controls.setRange(TEControlTag.QUANTITY, 8.0, 1.0, 24.0);
    controls.setRange(TEControlTag.QUANTITY, 6.0, 2.0, 12.0);
    controls.setRange(TEControlTag.LEVELREACTIVITY, 0.11, 0.0, 0.5);
    controls.setRange(TEControlTag.FREQREACTIVITY, 0.03, 0.0, 0.5);
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));

    addCommonControls();

    addShader("triangle_infinity_radial_waveform.fs", (s) -> {
      s.setUniform("brightnessDampening", 0.5f);
    });
  }
}
