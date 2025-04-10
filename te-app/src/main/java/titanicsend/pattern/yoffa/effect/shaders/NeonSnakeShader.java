package titanicsend.pattern.yoffa.effect.shaders;

import static java.lang.Math.*;
import static java.lang.Math.abs;
import static titanicsend.util.TEMath.*;

import heronarts.lx.parameter.LXParameter;
import java.util.Collection;
import titanicsend.pattern.TECommonControls;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.yoffa.framework.PatternTarget;

// https://www.shadertoy.com/view/4lB3DG
public class NeonSnakeShader extends FragmentShaderEffect {
  double[] origin;

  public NeonSnakeShader(PatternTarget target) {
    super(target);
    TECommonControls ctl = pattern.getControls();

    ctl.setRange(TEControlTag.SIZE, 0.3, 0.2, 0.9); // dispersion/scale
    ctl.setRange(TEControlTag.WOW1, 1, .1, 2); // glow
    ctl.setRange(TEControlTag.WOW2, 0, 0, .25); // beat reactivity

    // this is roughly where the center of the snake winds up
    // on the vehicle.
    origin = new double[] {0.5, 0.25};
  }

  @Override
  protected double[] getColorForPoint(
      double[] fragCoordinates, double[] resolution, double timeSeconds) {

    // Wow1 controls the base glow level
    double glow = pattern.getWow1();

    // Wow2 makes the snake expand and contract a little with the beat
    double dispersion =
        pattern.getSize() + pattern.getWow2() * sin(PI * pattern.getTempo().basis());

    // normalize coordinates
    double[] uv = divideArrays(fragCoordinates, resolution);
    uv[1] -= pattern.getYPos() + 0.25; // offset y to roughly center snake vertically

    // rotate
    uv = rotate2D(uv, origin);

    // scale (fixed scale to adapt pattern to vehicle)
    uv = multiplyArray(3, uv);

    // get current calculated palette color (plus alpha, which we'll fill in later)
    double[] waveColor = new double[4];
    colorToRGBArray(calcColor(), waveColor);

    double brightness = 0;
    for (int i = 0; i < 13; i++) {
      uv[1] += dispersion * sin(uv[0] + i / 1.5 + timeSeconds);
      double waveWidth = glow * abs(1.0 / (150 * uv[1]));
      brightness += waveWidth;
    }

    // gamma correct brightness and use it as alpha
    brightness = titanicsend.util.TEMath.clamp(brightness, 0, 1);
    waveColor[3] = brightness * brightness;
    return waveColor;
  }

  @Override
  public Collection<LXParameter> getParameters() {
    return null;
  }
}
