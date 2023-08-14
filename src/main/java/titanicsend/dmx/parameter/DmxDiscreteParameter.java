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

import java.util.ArrayList;
import java.util.List;

import heronarts.lx.LX;
import heronarts.lx.parameter.ObjectParameter;

public class DmxDiscreteParameter extends ObjectParameter<DmxDiscreteParameterOption> implements DmxParameter {

  // Initial options. Save for making copies.
  private final DmxDiscreteParameterOption[] constructorObjects;
  // All the possible DMX values
  private final int[] internalValues;
  
  private DmxBlendMode dmxBlendMode = DmxBlendMode.LERP;

  private final DmxParameterLimiter limiter;
  
  private boolean scaleToAlpha = false;

  /**
   * Extract options list into individual values
   */
  static private DmxDiscreteParameterOption[] getVerboseDmxOptions(DmxDiscreteParameterOption[] objects) {
    List<DmxDiscreteParameterOption> result = new ArrayList<DmxDiscreteParameterOption>();
    for (DmxDiscreteParameterOption o : objects) {
      for (int v = o.min; v <= o.max; v++) {
        result.add(new DmxDiscreteParameterOption(o.label, v));
      }
    }
    // TODO: fix toArray:
    DmxDiscreteParameterOption[] r = new DmxDiscreteParameterOption[result.size()];
    int i=0;
    for (DmxDiscreteParameterOption item : result) {
      r[i++] = item;
    }
    return r;
  }
  
  public DmxDiscreteParameter(String label, DmxDiscreteParameterOption[] objects) {
    super(label, getVerboseDmxOptions(objects));
    this.constructorObjects = objects;
    this.internalValues = extractIntValues();
    this.limiter = new DmxParameterLimiter(this);;
    this.setWrappable(false);
  }

  private int[] extractIntValues() {
    // Parent class objects list may be longer than list passed to this constructor,
    // because ranges were extracted into individual values.
    DmxDiscreteParameterOption[] objects = super.getObjects();
    int[] values = new int[objects.length];
    for (int i = 0; i < objects.length; i++) {
      values[i] = objects[i].min;
    }
    return values;
  }

  public DmxDiscreteParameter setBlendMode(DmxBlendMode mode) {
    this.dmxBlendMode = mode;
    return this;
  }

  @Override
  public DmxBlendMode getBlendMode() {
    return this.dmxBlendMode;
  }

  private int numBytes = 1;

  public DmxDiscreteParameter setNumBytes(int numBytes) {
    this.numBytes = numBytes;
    return this;
  }

  @Override
  public int getNumBytes() {
    return this.numBytes;
  }

  @Override
  public void writeBytes(byte[] output, int offset) {
    double value = getDmxValueLimited();
    if (this.numBytes == 1) {      
      output[offset] = (byte)value;
    } else if (this.numBytes == 2) {
      output[offset] = (byte)value;
      // TODO: the second byte!
    } else {
      LX.error(new Exception("Invalid number of bytes for DmxDiscreteParameter"));
    }
  }

  @Override
  public DmxParameterLimiter getLimiter() {
    return this.limiter;
  }

  public DmxDiscreteParameter setScaleToAlpha(boolean scaleToAlpha) {
    this.scaleToAlpha = scaleToAlpha;
    return this;
  }

  @Override
  public double getDmxValueLimited() {
    return this.limiter.limit(this.getDmxValue());
  }

  /**
   * Some parameters will scale to alpha, such as dimmer.
   *
   * @param alpha Alpha blend, from 0-1
   */
  @Override
  public double getValueLimited(double alpha) {
    // For DmxDiscreteParameter, this will get restricted to the list of options in setValue()
    return this.limiter.limit(getDmxValue(alpha));
  }

  /**
   * Some parameters will scale to alpha, such as dimmer.
   *
   * @param alpha Alpha blend, from 0-1
   */
  @Override
  public double getDmxValue(double alpha) {
    return getDmxValue() * (this.scaleToAlpha ? alpha : 1);
    //return super.getValue() * (this.scaleToAlpha ? alpha : 1);
  }

  public double getMin() {
    return this.getMinValue();
  }

  public double getMax() {
    return this.getMaxValue();
  }
  
  public int getDmxValue() {
    return this.internalValues[this.getValuei()];
    //DmxDiscreteParameterOption current = this.getObject();
    //return current.min;
  }
  
  public DmxParameter setDmxValue(double value) {
    return setDmxValue((int)value);
  }

  public DmxDiscreteParameter setDmxValue(int dmxValue) {
    for (DmxDiscreteParameterOption o : this.getObjects()) {
      if (dmxValue == o.min) {
        this.setValue(o);
        break;
      }
    }
    return this;
  }

  /**
   * Returns the parameter range as a double
   */
  public double getRangeD() {
    return this.getRange();
  }

  /**
   * For now a convenient way to create mirror parameters for buffers.
   */
  @Override
  public DmxDiscreteParameter copy() {
    DmxDiscreteParameter copy = new DmxDiscreteParameter(this.getLabel(), this.constructorObjects);
    copy.limiter.setLimitType(this.limiter.getLimitType());
    copy.limiter.setLimits(this.limiter.getMin(), this.limiter.getMax());
    copy.setScaleToAlpha(this.scaleToAlpha);
    return copy;
  }

}