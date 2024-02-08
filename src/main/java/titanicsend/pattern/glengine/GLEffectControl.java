package titanicsend.pattern.glengine;

import heronarts.lx.color.LXColor;
import titanicsend.pattern.glengine.GLControlData;
import titanicsend.pattern.glengine.GLShaderEffect;

public class GLEffectControl implements GLControlData {

  private GLShaderEffect effect;
  protected int lastColor1 = 0;
  protected int lastColor2 = 0;

  // take that Java!  I can pass random pointers around if I feel like it!  Maybe
  public GLEffectControl(GLShaderEffect effect) {
    this.effect = effect;
  }  
  
  public void setUniforms(GLShader s) {

/*
    // set standard shadertoy-style uniforms
    s.setUniform("iTime", (float) effect.getTime());
    s.setUniform("iResolution", (float) GLEngine.getWidth(), (float) GLEngine.getHeight());
    // s.setUniform("iMouse", 0f, 0f, 0f, 0f);

    // TE standard audio uniforms
    s.setUniform("beat", (float) effect.getTempo().basis());
    s.setUniform("sinPhaseBeat", (float) effect.sinePhaseOnBeat());
    s.setUniform("bassLevel", (float) effect.getBassLevel());
    s.setUniform("trebleLevel", (float) effect.getTrebleLevel());

    // added by @look
    s.setUniform("bassRatio", (float) effect.getBassRatio());
    s.setUniform("trebleRatio", (float) effect.getTrebleRatio());
    s.setUniform("volumeRatio", effect.getVolumeRatiof());

    // color-related uniforms
    int col = effect.calcColor();
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

    col = effect.calcColor2();
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
    s.setUniform("iSpeed", (float) effect.getSpeed());
    s.setUniform("iScale", (float) effect.getSize());
    s.setUniform("iQuantity", (float) effect.getQuantity());
    s.setUniform("iTranslate", (float) effect.getXPos(), (float) effect.getYPos());
    s.setUniform("iSpin", (float) effect.getSpin());
    s.setUniform("iRotationAngle", (float) effect.getRotationAngleFromSpin());
    s.setUniform("iBrightness", (float) effect.getBrightness());
    s.setUniform("iWow1", (float) effect.getWow1());
    s.setUniform("iWow2", (float) effect.getWow2());
    s.setUniform("iWowTrigger", effect.getWowTrigger());

 */
  }
}
