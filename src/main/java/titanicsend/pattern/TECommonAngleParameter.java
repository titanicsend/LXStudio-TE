package titanicsend.pattern;

import com.jogamp.common.nio.Buffers;
import heronarts.lx.LX;
import heronarts.lx.color.ColorParameter;
import heronarts.lx.color.GradientUtils.GradientFunction;
import heronarts.lx.color.LXColor;
import heronarts.lx.parameter.*;
import heronarts.lx.parameter.BooleanParameter.Mode;
import heronarts.lx.utils.LXUtils;
import titanicsend.app.TEGlobalPatternControls;
import titanicsend.lx.LXGradientUtils;
import titanicsend.lx.LXGradientUtils.BlendFunction;
import titanicsend.pattern.glengine.ShaderConfiguration;
import titanicsend.pattern.jon.TEControl;
import titanicsend.pattern.jon.TEControlTag;
import titanicsend.pattern.jon.VariableSpeedTimer;
import titanicsend.pattern.jon._CommonControlGetter;
import titanicsend.pattern.yoffa.framework.TEShaderView;
import titanicsend.util.MissingControlsManager;
import titanicsend.util.TE;
import titanicsend.util.TEColor;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


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

