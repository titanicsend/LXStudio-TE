/**
 * This pattern is derived directly from the LX GradientPattern:
 * https://github.com/heronarts/LX/blob/master/src/main/java/heronarts/lx/pattern/color/GradientPattern.java
 *
 * <p>Copyright 2016- Mark C. Slee, Heron Arts LLC
 *
 * <p>This file is part of the LX Studio software library. By using LX, you agree to the terms of
 * the LX Studio Software License and Distribution Agreement, available at: http://lx.studio/license
 *
 * <p>Please note that the LX license is not open-source. The license allows for free,
 * non-commercial use.
 *
 * <p>HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR OTHERWISE, AND SPECIFICALLY
 * DISCLAIMS ANY WARRANTY OF MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE,
 * WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */
package titanicsend.pattern.justin;

import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.model.LXPoint;
import heronarts.lx.parameter.EnumParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.transform.LXMatrix;
import heronarts.lx.utils.LXUtils;
import titanicsend.pattern.TEPerformancePattern;
import titanicsend.pattern.jon.TEControlTag;

@LXCategory(LXCategory.COLOR)
public class TEGradientPattern extends TEPerformancePattern {

  private final LXMatrix transform = new LXMatrix();

  private interface CoordinateFunction {
    float getCoordinate(LXPoint p, float normalized, float offset);
  }

  public static enum CoordinateMode {
    NORMAL(
        "Normal",
        (p, normalized, offset) -> {
          return normalized - offset;
        }),

    CENTER(
        "Center",
        (p, normalized, offset) -> {
          return 2 * Math.abs(normalized - (.5f + offset * .5f));
        }),

    RADIAL(
        "Radial",
        (p, normalized, offset) -> {
          return p.rcn - offset;
        });

    public final String name;
    public final CoordinateFunction function;
    public final CoordinateFunction invert;

    private CoordinateMode(String name, CoordinateFunction function) {
      this.name = name;
      this.function = function;
      this.invert =
          (p, normalized, offset) -> {
            return function.getCoordinate(p, normalized, offset) - 1;
          };
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  public final EnumParameter<CoordinateMode> xMode =
      new EnumParameter<CoordinateMode>("X Mode", CoordinateMode.NORMAL)
          .setDescription("Which coordinate mode the X-dimension uses");

  public final EnumParameter<CoordinateMode> yMode =
      new EnumParameter<CoordinateMode>("Y Mode", CoordinateMode.NORMAL)
          .setDescription("Which coordinate mode the Y-dimension uses");

  public final EnumParameter<CoordinateMode> zMode =
      new EnumParameter<CoordinateMode>("Z Mode", CoordinateMode.NORMAL)
          .setDescription("Which coordinate mode the Z-dimension uses");

  public TEGradientPattern(LX lx) {
    super(lx);

    // Lock in the ranges in case the defaults change upstream
    this.controls.setRange(TEControlTag.SIZE, 1, 0.1, 5);
    this.controls.setRange(TEControlTag.XPOS, 0, -1, 1);
    this.controls.setRange(TEControlTag.YPOS, 0, -1, 1);
    this.controls.setRange(TEControlTag.WOW1, 0, -1, 1);
    this.controls.setRange(TEControlTag.WOW2, 0, 0, 360);

    ((LXListenableNormalizedParameter) controls.getLXControl(TEControlTag.WOW2)).setWrappable(true);

    controls.markUnused(controls.getLXControl(TEControlTag.SPEED));
    controls.markUnused(controls.getLXControl(TEControlTag.QUANTITY));
    addCommonControls();
  }

  @Override
  protected void runTEAudioPattern(double deltaMs) {
    float size = (float) getSize();

    float xAmount = (float) getWow1();
    float yAmount = (float) getYPos();
    float zAmount = (float) getXPos();

    final float total = Math.abs(xAmount) + Math.abs(yAmount) + Math.abs(zAmount);
    if (total > 1) {
      xAmount /= total;
      yAmount /= total;
      zAmount /= total;
    }

    final float xOffset = 0; // this.xOffset.getValuef();
    final float yOffset = 0; // this.yOffset.getValuef();
    final float zOffset = 0; // this.zOffset.getValuef();

    final CoordinateMode xMode = this.xMode.getEnum();
    final CoordinateMode yMode = this.yMode.getEnum();
    final CoordinateMode zMode = this.zMode.getEnum();

    final CoordinateFunction xFunction = (xAmount < 0) ? xMode.invert : xMode.function;
    final CoordinateFunction yFunction = (yAmount < 0) ? yMode.invert : yMode.function;
    final CoordinateFunction zFunction = (zAmount < 0) ? zMode.invert : zMode.function;

    double pitch = -getRotationAngleFromSpin();
    double roll = Math.toRadians(-getWow2());

    // Build transform matrix as follows:
    // reset matrix at start of each frame
    // translate origin to center for rotation, then rotate
    // scale while still translated so scaling will be centered
    // translate coordinate system back to original position
    transform
        .identity()
        .translate(.5f, .5f, .5f)
        .rotateZ((float) roll)
        .rotateX((float) pitch)
        // .rotateY((float) Math.toRadians(-this.yaw.getValue()))
        .scale(size)
        .translate(-.5f, -.5f, -.5f);

    for (LXPoint p : this.model.points) {
      if (this.modelTE.isGapPoint(p)) {
        continue;
      }

      final float xn =
          p.xn * this.transform.m11
              + p.yn * this.transform.m12
              + p.zn * this.transform.m13
              + this.transform.m14;

      final float yn =
          p.xn * this.transform.m21
              + p.yn * this.transform.m22
              + p.zn * this.transform.m23
              + this.transform.m24;

      final float zn =
          p.xn * this.transform.m31
              + p.yn * this.transform.m32
              + p.zn * this.transform.m33
              + this.transform.m34;

      float lerp =
          LXUtils.clampf(
              xAmount * xFunction.getCoordinate(p, xn, xOffset)
                  + yAmount * yFunction.getCoordinate(p, yn, yOffset)
                  + zAmount * zFunction.getCoordinate(p, zn, zOffset),
              0,
              1);
      colors[p.index] = getGradientColor(lerp);
    }
  }
}
