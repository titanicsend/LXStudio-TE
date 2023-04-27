package titanicsend.lasercontrol;

import heronarts.lx.LX;
import heronarts.lx.LXComponent;
import heronarts.lx.LXLoopTask;
import heronarts.lx.color.LXDynamicColor;
import heronarts.lx.parameter.BooleanParameter;
import heronarts.lx.parameter.BooleanParameter.Mode;
import titanicsend.app.autopilot.TEOscMessage;
import titanicsend.pattern.TEPattern;

public class TELaserTask extends LXComponent implements LXLoopTask {

    public final BooleanParameter enabled =
        new BooleanParameter("Enabled", true)
        .setMode(Mode.TOGGLE);

    public TELaserTask(LX lx) {
        super(lx);

        addParameter("enabled", this.enabled);
    }

    @Override
    public void loop(double deltaMs) {
        if (this.enabled.isOn()) {
            // get the swatch color
            int primaryIndex = TEPattern.ColorType.PRIMARY.swatchIndex();
            LXDynamicColor primary = this.lx.engine.palette.swatch.getColor(primaryIndex);

            // convert to a 0 - 360 format for Pangolin
            int hue = (int) (primary.getHue());

            // send the OSC message
            TEOscMessage.sendOscToPangolin(lx, TEOscMessage.makePaletteHueAddress(), hue, false);
        }
    }
}
