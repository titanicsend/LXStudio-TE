package titanicsend.pattern.yoffa.effect.shaders;

import static java.lang.Math.atan2;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static titanicsend.util.TEMath.abs;
import static titanicsend.util.TEMath.addArrays;
import static titanicsend.util.TEMath.addToArray;
import static titanicsend.util.TEMath.divideArrays;
import static titanicsend.util.TEMath.fract;
import static titanicsend.util.TEMath.multiplyArray;
import static titanicsend.util.TEMath.subtractArrays;
import static titanicsend.util.TEMath.vectorLength;

import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;
import java.awt.Color;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import titanicsend.color.TEColorType;
import titanicsend.pattern.yoffa.framework.PatternTarget;

// https://www.shadertoy.com/view/Mls3DB
public class RainbowSwirlShader extends FragmentShaderEffect {

  private final CompoundParameter bloom =
      new CompoundParameter("Bloom", 5, -0, 25).setDescription("Bloom");

  private final CompoundParameter zoom =
      new CompoundParameter("Zoom", 0, .7, 5).setDescription("Zoom");

  public RainbowSwirlShader(PatternTarget target) {
    super(target);
  }

  @Override
  protected double[] getColorForPoint(
      double[] fragCoordinates, double[] resolution, double timeSeconds) {
    double[] uvTmp = subtractArrays(fragCoordinates, multiplyArray(.5, resolution));
    double[] uv = divideArrays(uvTmp, new double[] {resolution[1], resolution[1]});
    double d = vectorLength(uv) + zoom.getValue();
    d = pow(d, 2) - timeSeconds;
    double a = atan2(uv[1], uv[0]);

    double[] etmp1 = multiplyArray(a + d, new double[] {4.0, 5.0, 3.0});
    double[] etmp2 =
        new double[] {
          timeSeconds + a * bloom.getValue(), timeSeconds * 3.5, timeSeconds + d * 20.2
        };
    double[] etmp = addArrays(etmp1, etmp2);
    double[] e = Arrays.stream(etmp).map(v -> .15 * sin(v)).toArray();

    double[] color = multiplyArray(2, abs(addToArray(-.5, fract(addToArray(d, e)))));
    Color colorObj = new Color((float) color[0], (float) color[1], (float) color[2]);
    int swatchColor = pattern.getSwatchColor(TEColorType.PRIMARY);
    int finalColor = LXColor.blend(colorObj.getRGB(), swatchColor, LXColor.Blend.MULTIPLY);
    float[] finalFloat = new Color(finalColor).getColorComponents(null);
    return new double[] {finalFloat[0], finalFloat[1], finalFloat[2]};
  }

  @Override
  public Collection<LXParameter> getParameters() {
    return List.of(bloom, zoom);
  }
}
