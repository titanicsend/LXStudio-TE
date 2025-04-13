package titanicsend.pattern.look;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLEngine;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Look Shader Patterns")
public class SigmoidDanceAudioWaveform extends GLShaderPattern {

  public SigmoidDanceAudioWaveform(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    controls.setValue(TEControlTag.YPOS, -0.04);
    controls.setValue(TEControlTag.SIZE, 0.5);
    controls.setRange(TEControlTag.LEVELREACTIVITY, 4.0, 0.0, 8.0);
    controls.setRange(TEControlTag.FREQREACTIVITY, 1.0, 0.0, 4.0);
    controls.markUnused(controls.getLXControl(TEControlTag.WOW1));
    controls.markUnused(controls.getLXControl(TEControlTag.WOW2));

    addCommonControls();

    addShader(
        "sigmoid_dance_audio_waveform.fs",
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            s.setUniform("avgVolume", (float) GLEngine.getAvgVolume());
          }
        });
  }
}
