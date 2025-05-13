package titanicsend.pattern.jon;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import titanicsend.pattern.glengine.GLShader;
import titanicsend.pattern.glengine.GLShaderPattern;
import titanicsend.pattern.yoffa.framework.TEShaderView;

@LXCategory("Native Shaders Panels")
public class Phasers extends GLShaderPattern {

  // Work to be done per frame
  private void setUniforms(GLShader s) {
    // use the size control to control both the laser's beam size and surrounding glow
    CompoundParameter scaleCtl = (CompoundParameter) controls.getLXControl(TEControlTag.SIZE);
    double beamWidth = 0.005 + 0.0125 * scaleCtl.getNormalized();
    s.setUniform("beamWidth", (float) beamWidth);
    s.setUniform("iRotationAngle", (float) -getRotationAngleFromSpin());
  }

  public Phasers(LX lx) {
    super(lx, TEShaderView.ALL_PANELS);

    // set parameters for common controls

    // start with beam split 5 ways and spinning slowly
    controls
        .setRange(TEControlTag.QUANTITY, 5, 1, 8)
        .setUnits(TEControlTag.QUANTITY, LXParameter.Units.INTEGER);

    // Speed controls background movement speed and direction
    controls.setRange(TEControlTag.SPEED, 0.25, -1.0, 1.0);

    // Spin controls spin rate
    controls.setValue(TEControlTag.SPIN, 0.25); // give a little initial spin

    // Size controls beam width and dispersion
    controls.setRange(TEControlTag.SIZE, 21, 40, 2);

    // Wow1 controls beat reactivity
    controls.setRange(TEControlTag.WOW1, 0.0, 0.0, 0.6);

    // Wow2 is background fog brightness
    controls.setRange(TEControlTag.WOW2, 2, 0, 4);

    // After configuring all the common controls, register them with the UI
    addCommonControls();

    // Create the underlying shader pattern
    addShader(GLShader.config(lx).withFilename("phasers.fs").withUniformSource(this::setUniforms));
  }
}
