package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLEngine;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Look Shader Patterns")
public class SigmoidDanceAudioLevels extends GLShaderPattern {

  public SigmoidDanceAudioLevels(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.setRange(TEControlTag.QUANTITY, 4.0, 0.0, 4.0);
    controls.setRange(TEControlTag.LEVELREACTIVITY, 1.5, 0.0, 2.0);
    controls.setRange(TEControlTag.FREQREACTIVITY, 0.2, 0.0, 2.0);
    controls.setRange(TEControlTag.SIZE, 0.55, 0.0, 2.0);
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));

    addCommonControls();

    addShader(
        "sigmoid_dance_audio_levels.fs",
        (s) -> {
          s.setUniform("avgVolume", (float) GLEngine.getAvgVolume());
        });
  }
}
