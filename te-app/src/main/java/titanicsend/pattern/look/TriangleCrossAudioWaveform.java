package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.ConstructedShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Look Shader Patterns")
public class TriangleCrossAudioWaveform extends ConstructedShaderPattern {
  public TriangleCrossAudioWaveform(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);
  }

  @Override
  protected void createShader() {
    controls.setRange(TEControlTag.SIZE, 1.0, 0.3, 2.0);
    controls.setRange(TEControlTag.XPOS, 0.07, -0.5, 0.5);
    controls.setRange(TEControlTag.YPOS, -0.04, -0.5, 0.5);
    controls.setRange(TEControlTag.QUANTITY, 12.0, 1.0, 16.0);
    controls.setRange(TEControlTag.LEVELREACTIVITY, 0.3, 0.0, 1.0);
    controls.setRange(TEControlTag.FREQREACTIVITY, 0.11, 0.0, 1.0);
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));
    controls.markUnused(controls.getLXControl(TEControlTag.WOWTRIGGER));

    addShader("triangle_cross_waveform.fs");
  }
}
