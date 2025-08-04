package titanicsend.pattern.yoffa.effect.shaders;

import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static titanicsend.util.TEMath.addArrays;
import static titanicsend.util.TEMath.addToArray;
import static titanicsend.util.TEMath.clamp;
import static titanicsend.util.TEMath.divideArrays;
import static titanicsend.util.TEMath.mod;
import static titanicsend.util.TEMath.multiplyArray;
import static titanicsend.util.TEMath.vectorLength;

import heronarts.lx.parameter.LXParameter;
import java.util.Collection;
import titanicsend.pattern.yoffa.framework.PatternTarget;

// https://www.shadertoy.com/view/MdlXz8
public class WaterShader extends FragmentShaderEffect {

  private static final double TAU = 6.28318530718;
  private static final double[] origin = new double[] {0.5, 0.25};

  public WaterShader(PatternTarget target) {
    super(target);
  }

  @Override
  protected double[] getColorForPoint(
      double[] fragCoordinates, double[] resolution, double timeSeconds) {

    // normalize coords to 0 to 1 range, then do the translate, scale, rotate thing.
    double[] uv = divideArrays(fragCoordinates, resolution);
    uv = translate(uv);
    uv = rotate2D(uv, origin);
    double scale = pattern.getSize();
    uv = multiplyArray(scale, uv);

    // set coord system origin so we stay centered while zooming w/size control
    uv[0] -= origin[0] * scale;
    uv[1] -= origin[1] * scale;

    double tileFactor = TAU * pattern.getQuantity();
    double[] p = addToArray(-250, mod(multiplyArray(tileFactor, uv), TAU));
    double[] i = new double[] {p[0], p[1]};
    double c = 1.0;
    double inten = pattern.getWow2();
    double time = timeSeconds * 0.5;

    for (int n = 0; n < pattern.getWow1(); n++) {
      double t = time * (1.0 - (3.5 / (n + 1)));
      i = addArrays(p, new double[] {cos(t - i[0]) + sin(t + i[1]), sin(t - i[1]) + cos(t + i[0])});
      c +=
          1.0
              / vectorLength(
                  new double[] {p[0] / (sin(i[0] + t) / inten), p[1] / (cos(i[1] + t) / inten)});
    }
    c /= pattern.getWow1();
    c = 1.17 - pow(c, 1.4);

    double colourValue = 0.5 + pow(abs(c), 8.0);

    double[] colour = new double[3];
    colorToRGBArray(calcColor(), colour);
    colour = clamp(multiplyArray(colourValue, colour), 0.0, 1.0);

    return colour;
  }

  @Override
  public Collection<LXParameter> getParameters() {
    return null;
  }
}
