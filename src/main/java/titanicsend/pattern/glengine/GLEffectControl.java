package titanicsend.pattern.glengine;

import heronarts.lx.LX;
import heronarts.lx.color.LXColor;
import titanicsend.color.TEColorType;

public class GLEffectControl implements GLControlData {

  private final GLShaderEffect effect;
  private final LX lx;

  public GLEffectControl(GLShaderEffect effect) {
    this.effect = effect;
    this.lx = effect.getLX();
  }

  protected int getColor1() {
    return lx.engine.palette.getSwatchColor(TEColorType.PRIMARY.swatchIndex()).getColor();
  }

  protected int getColor2() {
    return lx.engine.palette.getSwatchColor(TEColorType.SECONDARY.swatchIndex()).getColor();
  }

  // send a subset of the controls we use with patterns
  public void setUniforms(GLShader s) {
    s.setUniform("iTime", (float) effect.getTime());
    s.setUniform("iResolution", (float) GLEngine.getWidth(), (float) GLEngine.getHeight());

    // get current primary and secondary colors
    // TODO - we're just grabbing swatch colors here.  Do we need to worry about modulation?
    int col = getColor1();
    s.setUniform(
        "iColorRGB",
        (float) (0xff & LXColor.red(col)) / 255f,
        (float) (0xff & LXColor.green(col)) / 255f,
        (float) (0xff & LXColor.blue(col)) / 255f);
    s.setUniform("iColorHSB", LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);

    col = getColor2();
    s.setUniform(
        "iColor2RGB",
        (float) (0xff & LXColor.red(col)) / 255f,
        (float) (0xff & LXColor.green(col)) / 255f,
        (float) (0xff & LXColor.blue(col)) / 255f);
    s.setUniform("iColor2HSB", LXColor.h(col) / 360f, LXColor.s(col) / 100f, LXColor.b(col) / 100f);
  }
}
