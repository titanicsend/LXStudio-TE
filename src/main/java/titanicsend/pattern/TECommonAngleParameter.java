package titanicsend.pattern;

import heronarts.lx.parameter.BoundedParameter;
import heronarts.lx.parameter.CompoundParameter;
import heronarts.lx.parameter.LXListenableNormalizedParameter;


// Create new class for Angle control so we can override the reset
// behavior and have reset set the current composite rotation angle
// to the spin control's current setting.
class TECommonAngleParameter extends CompoundParameter {

    public TECommonAngleParameter(String label, double value, double v0, double v1) {
        super(label, value, v0, v1);
    }

    @Override
    public LXListenableNormalizedParameter incrementNormalized(double amount) {
        // High resolution
        return super.incrementNormalized(amount / 5.);
    }

    @Override
    public BoundedParameter reset() {
        // if not spinning, resetting angle controls
        // resets both the static angle and the spin angle.
        if (getSpin() == 0) {
            spinRotor.setAngle(0);
        }

        // If spinning, reset static angle to 0, and also
        // add a corresponding offset to spinRotor to avoid a visual glitch.
        else {
            spinRotor.addAngle(-this.getValue());
        }
        return super.reset();
    }
}

