package titanicsend.pattern.cnk;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Combo FG")
public class DotPolka extends GLShaderPattern {

  public DotPolka(LX lx) {
    super(lx, TEShaderView.ALL_POINTS);

    // #iUniform float iScale=8. in {.5, 22.}
    controls.setRange(TEControlTag.SIZE, 8.0, 0.5, 22.0);
    
    // #iUniform float iQuantity = .8 in {0, 1}
    controls.setValue(TEControlTag.QUANTITY, 0.8);
    
    // #iUniform float levelReact = 0. in {0, 1}
    controls.setValue(TEControlTag.LEVELREACTIVITY, 0.0);
    
    // #iUniform float frequencyReact = 0. in {0, 1}
    controls.setValue(TEControlTag.FREQREACTIVITY, 0.0);

    // register common controls with the UI
    addCommonControls();

    // add the OpenGL shader and its frame-time setup function
    addShader(
        "dotpolka.fs",
        new GLShaderFrameSetup() {
          @Override
          public void OnFrame(GLShader s) {
            // iBeatTime acts like iTime but counts beats instead of seconds
            // beatCount is the total number of beats, beat is the fractional part (0.0-1.0)
            double iBeatTime = lx.engine.tempo.beatCount() + lx.engine.tempo.basis();
            s.setUniform("iBeatTime", (float) iBeatTime);
          }
        });
  }
}