package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Look Shader Patterns")
public class SigmoidDanceAudioLevels extends GLShaderPattern {

  public SigmoidDanceAudioLevels(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.setRange(TEControlTag.QUANTITY, 4.0, 0.0, 4.0);
    controls.setRange(TEControlTag.WOW1, 1.5, 0.0, 2.0);
    controls.setRange(TEControlTag.WOW2, 0.2, 0.0, 2.0);
    controls.setRange(TEControlTag.SIZE, 0.55, 0.0, 2.0);

    addCommonControls();

    addShader(
        "sigmoid_dance_audio_levels.fs",
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            s.setUniform("avgVolume", avgVolume.getValuef());
          }
        });
  }
}
