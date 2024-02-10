package titanicsend.pattern.glengine;

import heronarts.lx.color.LXColor;
import titanicsend.pattern.TEPerformancePattern;

public class GLPatternControl implements GLControlData {

  private TEPerformancePattern pattern;
  protected int lastColor1 = 0;
  protected int lastColor2 = 0;

  // take that Java!  I can pass random pointers around if I feel like it!  Maybe
  public GLPatternControl(TEPerformancePattern pattern) {
    this.pattern = pattern;
  }

  public void setUniforms(GLShader s) {
    // set standard shadertoy-style uniforms
    s.setUniform("iTime", (float) pattern.getTime());
    s.setUniform("iResolution", (float) GLEngine.getWidth(), (float) GLEngine.getHeight());
    // s.setUniform("iMouse", 0f, 0f, 0f, 0f);

    // TE standard audio uniforms
    s.setUniform("beat", (float) pattern.getTempo().basis());
    s.setUniform("sinPhaseBeat", (float) pattern.sinePhaseOnBeat());
    s.setUniform("bassLevel", (float) pattern.getBassLevel());
    s.setUniform("trebleLevel", (float) pattern.getTrebleLevel());

    // added by @look
    s.setUniform("bassRatio", (float) pattern.getBassRatio());
    s.setUniform("trebleRatio", (float) pattern.getTrebleRatio());
    s.setUniform("volumeRatio", pattern.getVolumeRatiof());

    // color-related uniforms
    int col = pattern.calcColor();
    if (col != lastColor1) {
      lastColor1 = col;
      s.setUniform(
          "iColorRGB",
          (float) (0xff & LXColor.red(col)) / 255f,
          (float) (0xff & LXColor.green(col)) / 255f,
          (float) (0xff & LXColor.blue(col)) / 255f);
      s.setUniform(
          "iColorHSB", LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);
    }

    col = pattern.calcColor2();
    if (col != lastColor2) {
      lastColor2 = col;
      s.setUniform(
          "iColor2RGB",
          (float) (0xff & LXColor.red(col)) / 255f,
          (float) (0xff & LXColor.green(col)) / 255f,
          (float) (0xff & LXColor.blue(col)) / 255f);
      s.setUniform(
          "iColor2HSB", LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);
    }

    // uniforms for common controls
    s.setUniform("iSpeed", (float) pattern.getSpeed());
    s.setUniform("iScale", (float) pattern.getSize());
    s.setUniform("iQuantity", (float) pattern.getQuantity());
    s.setUniform("iTranslate", (float) pattern.getXPos(), (float) pattern.getYPos());
    s.setUniform("iSpin", (float) pattern.getSpin());
    s.setUniform("iRotationAngle", (float) pattern.getRotationAngleFromSpin());
    s.setUniform("iBrightness", (float) pattern.getBrightness());
    s.setUniform("iWow1", (float) pattern.getWow1());
    s.setUniform("iWow2", (float) pattern.getWow2());
    s.setUniform("iWowTrigger", pattern.getWowTrigger());
  }
}
