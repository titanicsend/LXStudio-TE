package titanicsend.pattern;

import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.DiscreteParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;
import heronarts.lx.parameter.LXNormalizedParameter;
import heronarts.lx.parameter.LXParameter;
import heronarts.lx.parameter.LXParameterListener;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import titanicsend.color.TEColorParameter;
import titanicsend.color.TEGradientSource;
import titanicsend.pattern.jon.TEControl;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.jon._CommonControlGetter;
import titanicsend.preset.PresetEngine;
import titanicsend.preset.TEUserPresetParameter;
import titanicsend.preset.UserPreset;
import titanicsend.preset.UserPresetCollection;
import titanicsend.util.MissingControlsManager;
import titanicsend.util.TE;

public class TECommonControls {

  private final TEPerformancePattern pattern;

  private final HashMap<TEControlTag, TEControl> controlList =
      new HashMap<TEControlTag, TEControl>();

  public final Set<LXNormalizedParameter> unusedParams = new HashSet<>();

  // Color control is accessible, in case the pattern needs something
  // other than the current color.
  public TEColorParameter color;

  public TEUserPresetParameter preset;

  // Panic control courtesy of JKB's Rubix codebase
  public final BooleanParameter panic =
      (BooleanParameter)
          new BooleanParameter("PANIC", false)
              .setDescription("Panic! Moves parameters into a visible range")
              .setMode(BooleanParameter.Mode.MOMENTARY);

  private final LXParameterListener panicListener =
      (p) -> {
        if (((BooleanParameter) p).getValueb()) {
          onPanic();
        }
      };

  TECommonControls(TEPerformancePattern pat) {
    this.pattern = pat;

    panic.addListener(panicListener);

    // Create the user replaceable controls
    // derived classes must call addCommonControls() in their
    // constructor to add them to the UI.
    buildDefaultControlList();
  }

  public void buildDefaultControlList() {
    LXListenableNormalizedParameter p;

    p =
        new CompoundParameter(TEControlTag.LEVELREACTIVITY.getLabel(), 0.1, 0, 1)
            .setDescription("Level Reactivity");
    setControl(TEControlTag.LEVELREACTIVITY, p);

    p =
        new CompoundParameter(TEControlTag.FREQREACTIVITY.getLabel(), 0.1, 0, 1)
            .setDescription("Frequency Reactivity");
    setControl(TEControlTag.FREQREACTIVITY, p);

    p =
        new CompoundParameter(TEControlTag.SPEED.getLabel(), 0.5, -4.0, 4.0)
            .setPolarity(LXParameter.Polarity.BIPOLAR)
            .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
            .setExponent(1.75)
            .setDescription("Speed");
    setControl(TEControlTag.SPEED, p);

    p =
        new CompoundParameter(TEControlTag.XPOS.getLabel(), 0, -1.0, 1.0)
            .setPolarity(LXParameter.Polarity.BIPOLAR)
            .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
            .setDescription("X Position");
    setControl(TEControlTag.XPOS, p);

    p =
        new CompoundParameter(TEControlTag.YPOS.getLabel(), 0, -1.0, 1.0)
            .setPolarity(LXParameter.Polarity.BIPOLAR)
            .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
            .setDescription("Y Position");
    setControl(TEControlTag.YPOS, p);

    p = new CompoundParameter(TEControlTag.SIZE.getLabel(), 1, 0.01, 5.0).setDescription("Size");
    setControl(TEControlTag.SIZE, p);

    p =
        new CompoundParameter(TEControlTag.QUANTITY.getLabel(), 0.5, 0, 1.0)
            .setDescription("Quantity");
    setControl(TEControlTag.QUANTITY, p);

    p =
        new CompoundParameter(TEControlTag.SPIN.getLabel(), 0, -1.0, 1.0)
            .setPolarity(LXParameter.Polarity.BIPOLAR)
            .setNormalizationCurve(BoundedParameter.NormalizationCurve.BIAS_CENTER)
            .setExponent(2)
            .setDescription("Spin");

    setControl(TEControlTag.SPIN, p);

    p =
        new CompoundParameter(TEControlTag.BRIGHTNESS.getLabel(), 1.0, 0.0, 1.0)
            .setDescription("Brightness");
    setControl(TEControlTag.BRIGHTNESS, p);

    p = new CompoundParameter(TEControlTag.WOW1.getLabel(), 0, 0, 1.0).setDescription("Wow 1");
    setControl(TEControlTag.WOW1, p);

    p = new CompoundParameter(TEControlTag.WOW2.getLabel(), 0, 0, 1.0).setDescription("Wow 2");
    setControl(TEControlTag.WOW2, p);

    p =
        new BooleanParameter(TEControlTag.WOWTRIGGER.getLabel(), false)
            .setMode(BooleanParameter.Mode.MOMENTARY)
            .setDescription("Trigger WoW effects");
    setControl(TEControlTag.WOWTRIGGER, p);

    p =
        new BooleanParameter(TEControlTag.TWIST.getLabel(), false)
            .setMode(BooleanParameter.Mode.TOGGLE)
            .setDescription("Twist (axis swap)");
    setControl(TEControlTag.TWIST, p);

    // in degrees for display 'cause more people think about it that way
    p =
        new TECommonAngleParameter(
                this.pattern,
                this.pattern.spinRotor,
                TEControlTag.ANGLE.getLabel(),
                0,
                -Math.PI,
                Math.PI)
            .setDescription("Static Rotation Angle")
            .setPolarity(LXParameter.Polarity.BIPOLAR)
            .setWrappable(true)
            .setFormatter(
                (v) -> {
                  return Double.toString(Math.toDegrees(v));
                });
    setControl(TEControlTag.ANGLE, p);
  }

  public TEControl getControl(TEControlTag tag) {
    return controlList.get(tag);
  }

  /**
   * Retrieve backing LX control object for given tag
   *
   * @param tag
   */
  public LXListenableNormalizedParameter getLXControl(TEControlTag tag) {
    return controlList.get(tag).control;
  }

  final _CommonControlGetter defaultGetFn =
      new _CommonControlGetter() {
        @Override
        public double getValue(TEControl cc) {
          return cc.getValue();
        }
      };

  public TECommonControls setControl(TEControlTag tag, LXListenableNormalizedParameter lxp) {
    return setControl(tag, lxp, defaultGetFn);
  }

  public TECommonControls setControl(
      TEControlTag tag, LXListenableNormalizedParameter lxp, _CommonControlGetter getFn) {
    TEControl newControl = new TEControl(lxp, getFn);
    controlList.put(tag, newControl);
    return this;
  }

  /**
   * Sets a new getter function (an object implementing the _CommonControlGetter interface) for
   * specified tag's control.
   *
   * @param tag
   * @param getFn
   */
  public TECommonControls setGetterFunction(TEControlTag tag, _CommonControlGetter getFn) {
    controlList.get(tag).getFn = getFn;
    return this;
  }

  /**
   * Get current value of control specified by tag by calling the tag's configured getter function
   * (and NOT by directly calling the control's getValue() function)
   *
   * @param tag
   */
  protected double getValue(TEControlTag tag) {
    TEControl ctl = controlList.get(tag);
    return ctl.getFn.getValue(ctl);
  }

  /**
   * Sets current value for a common control
   *
   * @param tag - tag for control to set
   * @param val - the value to set
   */
  public TECommonControls setValue(TEControlTag tag, double val) {
    getLXControl(tag).setValue(val);
    return this;
  }

  public TECommonControls setExponent(TEControlTag tag, double exp) {
    LXListenableNormalizedParameter p = getLXControl(tag);
    p.setExponent(exp);
    return this;
  }

  public TECommonControls setLabel(TEControlTag tag, String newLabel) {
    LXListenableNormalizedParameter oldControl = getLXControl(tag);
    double value = 0d;
    double v0 = 0d;
    double v1 = 0d;
    if (oldControl instanceof CompoundParameter) {
      value = oldControl.getValue();
      v0 = ((CompoundParameter) oldControl).range.v0;
      v1 = ((CompoundParameter) oldControl).range.v1;
    } else if (oldControl instanceof BoundedParameter) {
      value = ((BoundedParameter) oldControl).getValue();
      v0 = ((BoundedParameter) oldControl).range.v0;
      v1 = ((BoundedParameter) oldControl).range.v1;
    }
    LXListenableNormalizedParameter newControl = updateParam(oldControl, newLabel, value, v0, v1);
    setControl(tag, newControl);
    return this;
  }

  public TECommonControls setNormalizationCurve(
      TEControlTag tag, BoundedParameter.NormalizationCurve curve) {
    LXListenableNormalizedParameter p = getLXControl(tag);
    if (p instanceof BoundedParameter) {
      ((BoundedParameter) p).setNormalizationCurve(curve);
    } else {
      TE.log("Warning: setNormalizationCurve() can not be called on parameter " + tag.toString());
    }
    return this;
  }

  public TECommonControls setRange(TEControlTag tag, double value, double v0, double v1) {
    LXListenableNormalizedParameter oldControl = getLXControl(tag);
    LXListenableNormalizedParameter newControl =
        updateParam(oldControl, oldControl.getLabel(), value, v0, v1);
    setControl(tag, newControl);
    return this;
  }

  public TECommonControls setUnits(TEControlTag tag, LXParameter.Units units) {
    LXListenableNormalizedParameter p = getLXControl(tag);
    p.setUnits(units);
    return this;
  }

  private static LXListenableNormalizedParameter updateParam(
      LXListenableNormalizedParameter oldControl,
      String label,
      double value,
      double v0,
      double v1) {
    LXListenableNormalizedParameter newControl;
    if (oldControl instanceof CompoundParameter) {
      newControl =
          new CompoundParameter(label, value, v0, v1)
              .setNormalizationCurve(((CompoundParameter) oldControl).getNormalizationCurve())
              .setExponent(oldControl.getExponent())
              .setDescription(oldControl.getDescription())
              .setPolarity(oldControl.getPolarity())
              .setUnits(oldControl.getUnits());
    } else if (oldControl instanceof BoundedParameter) {
      newControl =
          new BoundedParameter(label, value, v0, v1)
              .setNormalizationCurve(((BoundedParameter) oldControl).getNormalizationCurve())
              .setExponent(oldControl.getExponent())
              .setDescription(oldControl.getDescription())
              .setPolarity(oldControl.getPolarity())
              .setUnits(oldControl.getUnits());
    } else if (oldControl instanceof BooleanParameter) {
      newControl =
          new BooleanParameter(label)
              .setMode(((BooleanParameter) oldControl).getMode())
              .setDescription(oldControl.getDescription())
              .setUnits(oldControl.getUnits());
    } else if (oldControl instanceof DiscreteParameter) {
      newControl =
          new DiscreteParameter(label, ((DiscreteParameter) oldControl).getOptions())
              .setIncrementMode(((DiscreteParameter) oldControl).getIncrementMode())
              .setDescription(oldControl.getDescription())
              .setUnits(oldControl.getUnits());
    } else {
      TE.error("Unrecognized control type in TE Common Control " + oldControl.getClass().getName());
      return oldControl;
    }
    return newControl;
  }

  /**
   * To use the common controls, call this function from the constructor of
   * TEPerformancePattern-derived classes after configuring the default controls for your pattern.
   *
   * <p>If your pattern adds its own controls in addition to the common controls, you must call
   * addParameter() for them after calling this function so the UI stays consistent across patterns.
   */
  public void addCommonControls(TEPerformancePattern pat) {
    // load the missing controls file
    MissingControlsManager.MissingControls missingControls =
        MissingControlsManager.get().findMissingControls(pat.getClass());

    // controls will be added in the order their tags appear in the
    // TEControlTag enum
    for (TEControlTag tag : TEControlTag.values()) {
      LXListenableNormalizedParameter param = controlList.get(tag).control;

      if (missingControls != null) {
        if (missingControls.missing_control_tags.contains(tag)) {
          markUnused(param);
        }
      }

      this.pattern.addParam(tag.getPath(), param);
    }

    this.pattern.addParam("panic", this.panic);

    String colorPrefix = "";
    if (missingControls != null && !missingControls.uses_palette) {
      colorPrefix = "[x] ";
    }
    color =
        new TEColorParameter(TEGradientSource.get(), colorPrefix + "Color")
            .setDescription("TE Color");
    this.pattern.addParam("te_color", color);

    System.out.println("TECommonControls - A");
    UserPresetCollection collection = PresetEngine.get().getLibrary().get(this.pattern);
    if (!collection.getPresets().isEmpty()) {
      System.out.println("TECommonControls - B");
      preset = new TEUserPresetParameter(this.pattern, collection, "Presets");
      /*

            [LX 2025/07/31 23:00:28] Unexpected error performing action heronarts.lx.command.LXCommand$Parameter$SetNormalized@c94c61d - bad internal state?
      java.util.ConcurrentModificationException
      	at java.base/java.util.ArrayList.forEach(ArrayList.java:1598)
      	at heronarts.lx.parameter.LXListenableParameter.setValue(LXListenableParameter.java:224)
      	at heronarts.lx.parameter.LXListenableParameter.setValue(LXListenableParameter.java:208)
      	at heronarts.lx.parameter.DiscreteParameter.setNormalized(DiscreteParameter.java:407)
      	at heronarts.lx.parameter.DiscreteParameter.setNormalized(DiscreteParameter.java:26)
      	at heronarts.lx.command.LXCommand$Parameter$SetNormalized.perform(LXCommand.java:800)
      	at heronarts.lx.command.LXCommandEngine.perform(LXCommandEngine.java:62)
      	at heronarts.glx.ui.component.UIParameterComponent.setNormalizedCommand(UIParameterComponent.java:112)
             */
      this.pattern.addParam("te_preset", preset);
      preset.addListener(
          new LXParameterListener() {
            @Override
            public void onParameterChanged(LXParameter parameter) {
              if (parameter instanceof TEUserPresetParameter) {
                System.out.println("USER PRESET");
                UserPreset p = ((TEUserPresetParameter) parameter).getObject();
                System.out.println(p.getLabel() + " - " + p.getIndex());

                p.restore(pattern);
              } else {
                System.out.println("OTHER");
              }
            }
          });
    }
  }

  /** Included for consistency. We may need it later. */
  public void removeCommonControls() {
    for (TEControlTag tag : controlList.keySet()) {
      this.pattern.removeParam(tag.getPath());
    }
    controlList.clear();
  }

  public void markUnused(LXNormalizedParameter param) {
    unusedParams.add(param);
  }

  /** Set remote controls in order they will appear on the midi surfaces */
  protected void setRemoteControls() {
    this.pattern.setCustomRemoteControls(
        new LXListenableNormalizedParameter[] {
          getControl(TEControlTag.LEVELREACTIVITY).control,
          getControl(TEControlTag.FREQREACTIVITY).control,
          this.pattern.view,
          getControl(TEControlTag.SPEED).control,
          getControl(TEControlTag.XPOS).control,
          getControl(TEControlTag.YPOS).control,
          getControl(TEControlTag.QUANTITY).control,
          getControl(TEControlTag.SIZE).control,
          getControl(TEControlTag.ANGLE).control,
          getControl(TEControlTag.SPIN).control,
          this.panic,
          this.preset,
          getControl(TEControlTag.WOW1).control,
          getControl(TEControlTag.WOW2).control,
          getControl(TEControlTag.WOWTRIGGER).control,
          this.color.offset,
          this.pattern.captureDefaults
          // To be SHIFT, not implemented yet
        });
  }

  /**
   * Called when the momentary PANIC knob is pressed. Parameters can be reset here or just
   * constrained to a visible range.
   */
  protected void onPanic() {
    // For color, reset everything but Hue
    this.color.blendMode.reset();
    this.color.colorSource.reset();
    this.color.offset.reset();
    this.color.saturation.setNormalized(1);
    this.color.brightness.setNormalized(1);

    getControl(TEControlTag.BRIGHTNESS).control.reset();
    getControl(TEControlTag.SPEED).control.reset();

    getControl(TEControlTag.XPOS).control.reset();
    getControl(TEControlTag.YPOS).control.reset();
    getControl(TEControlTag.QUANTITY).control.reset();
    getControl(TEControlTag.SIZE).control.reset();

    getControl(TEControlTag.ANGLE).control.reset();
    getControl(TEControlTag.SPIN).control.reset();

    getControl(TEControlTag.WOW1).control.reset();
    getControl(TEControlTag.WOW2).control.reset();
    getControl(TEControlTag.WOWTRIGGER).control.reset();
    getControl(TEControlTag.TWIST).control.reset();

    getControl(TEControlTag.LEVELREACTIVITY).control.reset();
    getControl(TEControlTag.FREQREACTIVITY).control.reset();
  }

  public void dispose() {
    panic.removeListener(panicListener);
  }
}
