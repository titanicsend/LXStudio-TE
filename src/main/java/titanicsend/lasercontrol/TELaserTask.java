package titanicsend.lasercontrol;

import heronarts.lx.LX;
import heronarts.lx.LXLoopTask;
import heronarts.lx.color.LXColor;
import heronarts.lx.color.LXDynamicColor;
import titanicsend.app.autopilot.TEOscMessage;
import titanicsend.pattern.TEPattern;
import titanicsend.util.TE;

public class TELaserTask implements LXLoopTask {
    public LX lx;
    public TELaserTask(LX lx) {
        this.lx = lx;
    }

    @Override
    public void loop(double deltaMs) {
        // get the swatch color
        int primaryIndex = TEPattern.ColorType.PRIMARY.swatchIndex();
        LXDynamicColor primary = this.lx.engine.palette.swatch.getColor(primaryIndex);
        int rgb = primary.getColor();

        // convert to a 0 - 360 format for Pangolin
        int hue = (int) (360 * LXColor.h(rgb));

        // send the OSC message
        TEOscMessage.sendOscToPangolin(lx, TEOscMessage.makePaletteHueAddress(), hue, false);
    }
}
