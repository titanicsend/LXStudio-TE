package titanicsend.pattern.jon;

import heronarts.lx.parameter.LXListenableNormalizedParameter;

/**
 * A dynamically user-configurable control, for use with the TE
 * common controls.
 */
public class TEControl {

    public TEControl(LXListenableNormalizedParameter ctl, _CommonControlGetter getFn) {
        this.control = ctl;
        this.getFn = getFn;
    }

    public LXListenableNormalizedParameter control;
    public _CommonControlGetter getFn;

    /**
     * internal:
     * @return - the result of the actual LX control's getValue() function, for
     * use by the assigned _CommonControlGetter function.
     */
    public double getValue() {
        return control.getValue();
    }
}
