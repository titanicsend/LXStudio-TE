/**
 * Copyright 2023- Justin Belcher, Mark C. Slee, Heron Arts LLC
 *
 * This file is part of the LX Studio software library. By using
 * LX, you agree to the terms of the LX Studio Software License
 * and Distribution Agreement, available at: http://lx.studio/license
 *
 * Please note that the LX license is not open-source. The license
 * allows for free, non-commercial use.
 *
 * HERON ARTS MAKES NO WARRANTY, EXPRESS, IMPLIED, STATUTORY, OR
 * OTHERWISE, AND SPECIFICALLY DISCLAIMS ANY WARRANTY OF
 * MERCHANTABILITY, NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR
 * PURPOSE, WITH RESPECT TO THE SOFTWARE.
 *
 * @author Mark C. Slee <mark@heronarts.com>
 */

package titanicsend.dmx.parameter;

import heronarts.lx.LX;
import heronarts.lx.parameter.CompoundParameter;

public class DmxCompoundParameter extends CompoundParameter implements DmxParameter {

  private final DmxParameterLimiter limiter;

  public DmxCompoundParameter(String label) {
    this(label, 0);
  }

  public DmxCompoundParameter(String label, DmxDiscreteParameterOption[] objects) {
    this(label, 0);
    // TODO: Incorporate list of options like DmxDiscreteParameter
  }

  public DmxCompoundParameter(String label, double value) {
    this(label, value, 255);
  }

  public DmxCompoundParameter(String label, double value, double max) {
    this(label, value, 0, max);
  }

  public DmxCompoundParameter(String label, double value, double v0, double v1) {
    super(label, value, v0, v1);
    this.limiter = new DmxParameterLimiter(this);
  }

  private DmxBlendMode dmxBlendMode = DmxBlendMode.LERP;

  public DmxCompoundParameter setBlendMode(DmxBlendMode mode) {
    this.dmxBlendMode = mode;
    return this;
  }

  @Override
  public DmxBlendMode getBlendMode() {
    return this.dmxBlendMode;
  }

  private boolean scaleToAlpha = false;

  public DmxCompoundParameter setScaleToAlpha(boolean scaleToAlpha) {
    this.scaleToAlpha = scaleToAlpha;
    return this;
  }

  private int numBytes = 1;

  public DmxCompoundParameter setNumBytes(int numBytes) {
    this.numBytes = numBytes;
    return this;
  }

  @Override
  public int getNumBytes() {
    return this.numBytes;
  }

  /**
   * Write bytes for this parameter into the output array.
   * Value should be restricted by limiter
   */
  @Override
  public final void writeBytes(byte[] output, int offset) {
    double normalized = (getDmxValueLimited() - getMin()) / getRangeD();
    if (this.numBytes == 1) {      
      output[offset] = (byte)(normalized * 255);
    } else if (this.numBytes == 2) {
      int i = (int)(normalized * 65535);
      output[offset] = (byte) ((i >> 8) & 0xff);
      output[offset+1] = (byte) ((i >> 0) & 0xff);
    } else {
      LX.error(new Exception("Invalid number of bytes for DmxCompoundParameter"));
    }
  }

  @Override
  public DmxParameterLimiter getLimiter() {
    return this.limiter;
  }

  @Override
  public double getDmxValueLimited() {
    return this.limiter.limit(this.getValue());
  }

  /**
   * Some parameters will scale to alpha, such as dimmer.
   *
   * @param alpha Alpha blend, from 0-1
   */
  @Override
  public double getValueLimited(double alpha) {
    return this.limiter.limit(getDmxValue(alpha));
  }

  /**
   * Some parameters will scale to alpha, such as dimmer.
   *
   * @param alpha Alpha blend, from 0-1
   */
  @Override
  public double getDmxValue(double alpha) {
    return super.getValue() * (this.scaleToAlpha ? alpha : 1);
  }
  
  public DmxParameter setDmxValue(double value) {
    setValue(value);
    return this;
  }

  public double getMin() {
    return this.range.min;
  }

  public double getMax() {
    return this.range.max;
  }

  /**
   * Returns the parameter range as a double
   */
  public double getRangeD() {
    return this.getRange();
  }

  @Override
  public DmxCompoundParameter copy() {
    DmxCompoundParameter copy = new DmxCompoundParameter(this.getLabel(), this.getValue(), this.range.v0, this.range.v1);
    copy.limiter.setLimitType(this.limiter.getLimitType());
    copy.limiter.setLimits(this.limiter.getMin(), this.limiter.getMax());
    copy.setNumBytes(numBytes);
    copy.setScaleToAlpha(this.scaleToAlpha);
    return copy;
  }

}