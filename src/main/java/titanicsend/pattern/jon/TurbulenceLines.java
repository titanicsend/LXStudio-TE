package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.TEMath;

@LXCategory("Noise")
public class TurbulenceLines extends DriftEnabledPattern {
  double freqShift = 0.0;

  public TurbulenceLines(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // common controls setup
    controls.setRange(TEControlTag.SPEED, 0, -4, 4);
    controls.setValue(TEControlTag.SPEED, 1.0);

    controls.setRange(TEControlTag.SIZE, 1, 1.25, 0.4);
    controls.setRange(TEControlTag.QUANTITY, 60, 20, 200);
    controls.setRange(TEControlTag.WOW1, 0.5, 0.2, 1);

    controls.setValue(TEControlTag.XPOS, 0.25);
    controls.setValue(TEControlTag.YPOS, -0.25);
    avgTreble.alpha = 0.04;
    avgBass.alpha = 0.02;

    eq.getRaw();
    eq.getDecibels();

    // register common controls with LX
    addCommonControls();

    addShader(
        "turbulent_noise_lines.fs",
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            // calculate incremental transform based on elapsed time
            s.setUniform("iTranslate", (float) getXPosition(), (float) getYPosition());

            // override iTime so we can speed up noise field progression while leaving the controls
            // in a more reasonable range
            s.setUniform("iTime", 2f * (float) getTime());
            double freqShift = avgTreble.getValuef() * getFrequencyReactivity();
            s.setUniform("freqShift", (float) freqShift);
          }
        });
  }


}
