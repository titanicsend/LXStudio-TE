package titanicsend.app;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXParameter;

public class TEGlobalPatternControls extends LXComponent {
    public final BooleanParameter useGlobalSpeed =
        new BooleanParameter("useSpeed", false);
    public final CompoundParameter globalSpeed =
        new CompoundParameter("Global Speed", 0.25, 0, 1);

    public TEGlobalPatternControls(LX lx) {
        super(lx);
// TODO - add parameters if we want to place UI for this somewhere other than in an effect
//        addParameter("globalSpeedEnable", this.useGlobalSpeed);
//        addParameter("globalSpeed", this.globalSpeed);
    }
}
