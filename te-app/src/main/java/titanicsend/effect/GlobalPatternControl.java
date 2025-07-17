package titanicsend.effect;

import heronarts.lx.GpuDevice;
import heronarts.lx.LX;
import heronarts.lx.LXCategory;
import heronarts.lx.parameter.*;
import titanicsend.app.TEGlobalPatternControls;

@LXCategory("Titanics End")
public class GlobalPatternControl extends TEEffect implements GpuDevice {

  public final BooleanParameter speedEnable =
      new BooleanParameter("Enable", false).setDescription("Use speed from global controller");

  public final CompoundParameter speed =
      new CompoundParameter("Speed", .25)
          .setExponent(2)
          .setDescription("Speed for all running patterns");

  public TEGlobalPatternControls globalControls;

  public GlobalPatternControl(LX lx) {
    super(lx);
    addParameter("speedEnable", this.speedEnable);
    addParameter("speed", this.speed);
    globalControls = (TEGlobalPatternControls) lx.engine.getChild("globalPatternControls");
  }

  @Override
  public void onParameterChanged(LXParameter parameter) {
    super.onParameterChanged(parameter);
    if (parameter == this.speedEnable) {
      globalControls.useGlobalSpeed.setValue(this.speedEnable.isOn());
    }

    // TODO - add switches for any other controls we might want controlled by DMX/Modulators/etc.
  }

  @Override
  public void run(double deltaMs, double enabledAmount) {
    if (globalControls.useGlobalSpeed.isOn()) {
      globalControls.globalSpeed.setValue(this.speed.getValue());
    }
  }
}
