/**
 * Copyright 2023- Justin K. Belcher, Mark C. Slee, Heron Arts LLC
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

package titanicsend.model.justin;

import heronarts.lx.LX;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXParameterListener;

/**
 * Similar to LXVirtualParameter, but with priority on normalized methods.
 * Allows target parameter to be set after initialization, to play nice
 * with loading from file.
 *
 * It IS a DiscreteParameter and it WRAPS a DiscreteParameter.
 */
abstract public class LXVirtualDiscreteParameter<T extends DiscreteParameter> extends DiscreteParameter {

  private T parameter = null;

  public LXVirtualDiscreteParameter(String label) {
    super(label, 1);
  }

  public LXVirtualDiscreteParameter(T parameter) {
    this(parameter != null ? parameter.getLabel() : null, parameter);
  }

  public LXVirtualDiscreteParameter(String label, T parameter) {
    super(label, 1);
    setParameter(parameter, false);
  }

  public T getParameter() {
    return this.parameter;
  }

  public LXVirtualDiscreteParameter<T> setParameter(T parameter) {
    return setParameter(parameter, true);
  }

  public LXVirtualDiscreteParameter<T> setParameter(T parameter, boolean fireImmediately) {
    if (this.parameter != null) {
      this.parameter.removeListener(realParameterListener);
    }
    this.parameter = parameter;
    if (this.parameter != null) {
      this.parameter.addListener(realParameterListener);
    }
    if (fireImmediately) {
      onRealParameterChanged();
    }
    return this;
  }

  private final LXParameterListener realParameterListener = (p) -> {
    onRealParameterChanged();
  };

  protected void onRealParameterChanged() {
    bang();
  }

  @Override
  public double getValue() {
    if (parameter != null) {
      return parameter.getValue();
    }
    return 0;
  }

  @Override
  public DiscreteParameter setNormalized(double value) {
    if (this.parameter != null) {
      this.parameter.setNormalized(value);
    }
    return this;
  }

  @Override
  public double getNormalized() {
    if (this.parameter != null) {
      return this.parameter.getNormalized();
    }
    return 0;
  }

  @Override
  public LXListenableNormalizedParameter incrementNormalized(double amount) {
    if (this.parameter != null) {
      this.parameter.incrementNormalized(amount);
    }
    return this;
  }

  @Override
  public LXListenableNormalizedParameter incrementNormalized(double amount, boolean wrap) {
    if (this.parameter != null) {
      this.parameter.incrementNormalized(amount, wrap);
    }
    return this;
  }

  @Override
  protected double updateValue(double value) {
    return value;
  }

  @Override
  public DiscreteParameter setMappable(boolean mappable) {
    LX.error("Can not call setMappable() on LXVirtualDiscreteParameter, call on target parameter instead.");
    return this;
  }

  @Override
  public boolean isMappable() {
    if (this.parameter != null) {
      return this.parameter.isMappable();
    }
    return super.isMappable();
  }

  @Override
  public DiscreteParameter setWrappable(boolean wrappable) {
    return super.setWrappable(wrappable);
  }

  @Override
  public boolean isWrappable() {
    if (this.parameter != null) {
      return this.parameter.isWrappable();
    }
    return super.isWrappable();
  }

  /*
   * DiscreteParameter overrides
   */

  @Override
  public int getMinValue() {
    if (this.parameter != null) {
      return this.parameter.getMinValue();
    }
    return super.getMinValue();
  }

  @Override
  public int getMaxValue() {
    if (this.parameter != null) {
      return this.parameter.getMaxValue();
    }
    return super.getMaxValue();
  }

  @Override
  public int getRange() {
    if (this.parameter != null) {
      return this.parameter.getRange();
    }
    return super.getRange();
  }

  @Override
  public DiscreteParameter increment() {
    if (this.parameter != null) {
      return this.parameter.increment();
    }
    return super.increment();
  }

  @Override
  public DiscreteParameter decrement() {
    if (this.parameter != null) {
      return this.parameter.decrement();
    }
    return super.decrement();
  }

  @Override
  public String getOption() {
    if (this.parameter != null) {
      return this.parameter.getOption();
    }
    return super.getOption();
  }

  @Override
  public String[] getOptions() {
    if (this.parameter != null) {
      return this.parameter.getOptions();
    }
    return super.getOptions();
  }

  @Override
  public int getValuei() {
    if (this.parameter != null) {
      return this.parameter.getValuei();
    }
    return super.getValuei();
  }

  @Override
  public void dispose() {
    if (this.parameter != null) {
      this.parameter.removeListener(this.realParameterListener);
      this.parameter = null;
    }
    super.dispose();
  }

}
