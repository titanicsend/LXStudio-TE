package titanicsend.pattern.glengine;

import heronarts.lx.color.LXColor;
import titanicsend.audio.AudioStems;
import titanicsend.pattern.TEPerformancePattern;

public class GLPatternControl implements GLControlData {

  private TEPerformancePattern pattern;

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

    s.setUniform("levelReact", (float) pattern.getLevelReactivity());
    s.setUniform("frequencyReact", (float) pattern.getFrequencyReactivity());

    // Current values from audio stems
    s.setUniform("stemBass", (float) AudioStems.current.bass.getValuef());
    s.setUniform("stemDrums", (float) AudioStems.current.drums.getValuef());
    s.setUniform("stemVocals", (float) AudioStems.current.vocals.getValuef());
    s.setUniform("stemOther", (float) AudioStems.current.other.getValuef());

    // color-related uniforms
    int col = pattern.calcColor();
    s.setUniform(
        "iColorRGB",
        (float) (0xff & LXColor.red(col)) / 255f,
        (float) (0xff & LXColor.green(col)) / 255f,
        (float) (0xff & LXColor.blue(col)) / 255f);
    s.setUniform("iColorHSB", LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);

    col = pattern.calcColor2();
    s.setUniform(
        "iColor2RGB",
        (float) (0xff & LXColor.red(col)) / 255f,
        (float) (0xff & LXColor.green(col)) / 255f,
        (float) (0xff & LXColor.blue(col)) / 255f);
    s.setUniform("iColor2HSB", LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);

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
